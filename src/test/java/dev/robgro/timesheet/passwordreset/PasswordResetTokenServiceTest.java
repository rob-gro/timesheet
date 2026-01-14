package dev.robgro.timesheet.passwordreset;

import dev.robgro.timesheet.exception.InvalidTokenException;
import dev.robgro.timesheet.exception.TokenAlreadyUsedException;
import dev.robgro.timesheet.exception.TokenExpiredException;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private PasswordResetTokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Set token TTL values via reflection
        ReflectionTestUtils.setField(tokenService, "tokenTtlMinutes", 30);
        ReflectionTestUtils.setField(tokenService, "tokenTtlMinutesSelf", 15);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setTokenVersion(1);
    }

    // ----- Create Reset Token Tests -----

    @Test
    void shouldCreateResetToken_whenUserExists() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        String plainToken = tokenService.createResetToken(
                1L,
                PasswordResetToken.ResetRequestType.ADMIN,
                "192.168.1.1",
                "Mozilla/5.0"
        );

        // then
        assertThat(plainToken).isNotNull();
        assertThat(plainToken).hasSize(64);  // 32 bytes = 64 hex chars

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getRequestedBy()).isEqualTo(PasswordResetToken.ResetRequestType.ADMIN);
        assertThat(savedToken.getRequestIp()).isEqualTo("192.168.1.1");
        assertThat(savedToken.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(savedToken.getResetVersion()).isEqualTo(1);
        assertThat(savedToken.getTokenHash()).hasSize(64);  // SHA-256 hash = 64 hex chars
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void shouldInvalidateOldTokens_whenCreatingNewToken() {
        // given
        PasswordResetToken oldToken = new PasswordResetToken();
        oldToken.setId(100L);
        oldToken.setUsedAt(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUserIdAndUsedAtIsNull(1L))
                .thenReturn(Arrays.asList(oldToken));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        tokenService.createResetToken(
                1L,
                PasswordResetToken.ResetRequestType.SELF,
                "127.0.0.1",
                "TestAgent"
        );

        // then
        assertThat(oldToken.getUsedAt()).isNotNull();
        verify(tokenRepository).saveAll(anyList());
    }

    @Test
    void shouldUseCorrectTTL_forAdminReset() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        tokenService.createResetToken(
                1L,
                PasswordResetToken.ResetRequestType.ADMIN,
                "127.0.0.1",
                "Test"
        );

        // then
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());

        PasswordResetToken token = captor.getValue();
        long minutesDiff = java.time.Duration.between(
                token.getCreatedAt(),
                token.getExpiresAt()
        ).toMinutes();

        assertThat(minutesDiff).isEqualTo(30);  // Admin TTL
    }

    @Test
    void shouldUseCorrectTTL_forSelfReset() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        tokenService.createResetToken(
                1L,
                PasswordResetToken.ResetRequestType.SELF,
                "127.0.0.1",
                "Test"
        );

        // then
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());

        PasswordResetToken token = captor.getValue();
        long minutesDiff = java.time.Duration.between(
                token.getCreatedAt(),
                token.getExpiresAt()
        ).toMinutes();

        assertThat(minutesDiff).isEqualTo(15);  // Self-service TTL
    }

    // ----- Validate Token Tests -----

    @Test
    void shouldValidateToken_whenTokenIsValid() {
        // given
        String plainToken = "a".repeat(64);  // 64 hex chars
        PasswordResetToken token = createValidToken();

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        // when
        PasswordResetToken result = tokenService.validateToken(plainToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(token);
    }

    @Test
    void shouldThrowException_whenTokenNotFound() {
        // given
        String plainToken = "a".repeat(64);
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> tokenService.validateToken(plainToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid or expired token");
    }

    @Test
    void shouldThrowException_whenTokenAlreadyUsed() {
        // given
        String plainToken = "a".repeat(64);
        PasswordResetToken token = createValidToken();
        token.setUsedAt(LocalDateTime.now());

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        // when/then
        assertThatThrownBy(() -> tokenService.validateToken(plainToken))
                .isInstanceOf(TokenAlreadyUsedException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void shouldThrowException_whenTokenExpired() {
        // given
        String plainToken = "a".repeat(64);
        PasswordResetToken token = createValidToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        // when/then
        assertThatThrownBy(() -> tokenService.validateToken(plainToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");
    }

    // ----- Consume Token Tests -----

    @Test
    void shouldConsumeToken_andUpdatePassword() {
        // given
        String plainToken = "a".repeat(64);
        String newPassword = "newSecurePassword123";
        String encodedPassword = "encodedPassword";

        PasswordResetToken token = createValidToken();
        token.setUser(testUser);

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode(newPassword))
                .thenReturn(encodedPassword);
        when(sessionRegistry.getAllSessions(anyString(), anyBoolean()))
                .thenReturn(List.of());

        // when
        tokenService.consumeToken(plainToken, newPassword);

        // then
        assertThat(testUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(testUser.getLastPasswordResetAt()).isNotNull();
        assertThat(testUser.getTokenVersion()).isEqualTo(2);  // Incremented
        assertThat(token.getUsedAt()).isNotNull();

        verify(userRepository).save(testUser);
        verify(tokenRepository).save(token);
    }

    @Test
    void shouldInvalidateSessions_whenConsumingToken() {
        // given
        String plainToken = "a".repeat(64);
        PasswordResetToken token = createValidToken();
        token.setUser(testUser);

        SessionInformation session1 = mock(SessionInformation.class);
        SessionInformation session2 = mock(SessionInformation.class);
        List<SessionInformation> sessions = Arrays.asList(session1, session2);

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");
        when(sessionRegistry.getAllSessions("testuser", false))
                .thenReturn(sessions);

        // when
        tokenService.consumeToken(plainToken, "newPassword");

        // then
        verify(session1).expireNow();
        verify(session2).expireNow();
        verify(sessionRegistry).getAllSessions("testuser", false);
    }

    // ----- Invalidate User Tokens Tests -----

    @Test
    void shouldInvalidateAllUnusedTokens_forUser() {
        // given
        PasswordResetToken token1 = new PasswordResetToken();
        token1.setUsedAt(null);
        PasswordResetToken token2 = new PasswordResetToken();
        token2.setUsedAt(null);

        when(tokenRepository.findByUserIdAndUsedAtIsNull(1L))
                .thenReturn(Arrays.asList(token1, token2));

        // when
        tokenService.invalidateUserTokens(1L);

        // then
        assertThat(token1.getUsedAt()).isNotNull();
        assertThat(token2.getUsedAt()).isNotNull();
        verify(tokenRepository).saveAll(anyList());
    }

    // ----- Helper Methods -----

    private PasswordResetToken createValidToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setTokenHash("abcd".repeat(16));  // 64 chars
        token.setUser(testUser);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        token.setUsedAt(null);
        token.setRequestedBy(PasswordResetToken.ResetRequestType.SELF);
        token.setRequestIp("127.0.0.1");
        token.setUserAgent("TestAgent");
        token.setResetVersion(1);
        return token;
    }
}
