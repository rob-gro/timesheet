package dev.robgro.timesheet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-64-characters-long-for-hs512-algorithm-to-work-properly";
    private static final long TEST_EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, TEST_EXPIRATION);
    }

    // ----- Token Creation Tests -----

    @Test
    void shouldCreateToken_whenValidAuthentication() {
        // given
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        UserDetails userDetails = new User("testuser", "password", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, authorities
        );

        // when
        String token = jwtTokenProvider.createToken(authentication);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void shouldCreateDifferentTokens_forDifferentUsers() {
        // given
        UserDetails user1 = new User("user1", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails user2 = new User("user2", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        Authentication auth1 = new UsernamePasswordAuthenticationToken(user1, null, user1.getAuthorities());
        Authentication auth2 = new UsernamePasswordAuthenticationToken(user2, null, user2.getAuthorities());

        // when
        String token1 = jwtTokenProvider.createToken(auth1);
        String token2 = jwtTokenProvider.createToken(auth2);

        // then
        assertThat(token1).isNotEqualTo(token2);
    }

    // ----- Token Validation Tests -----

    @Test
    void shouldValidateToken_whenTokenIsValid() {
        // given
        UserDetails userDetails = new User("testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        String token = jwtTokenProvider.createToken(authentication);

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldReturnFalse_whenTokenIsInvalid() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsNull() {
        // when
        boolean isValid = jwtTokenProvider.validateToken(null);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsEmpty() {
        // when
        boolean isValid = jwtTokenProvider.validateToken("");

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsModified() {
        // given
        UserDetails userDetails = new User("testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        String token = jwtTokenProvider.createToken(authentication);
        String modifiedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // when
        boolean isValid = jwtTokenProvider.validateToken(modifiedToken);

        // then
        assertThat(isValid).isFalse();
    }

    // ----- Username Extraction Tests -----

    @Test
    void shouldExtractUsername_whenTokenIsValid() {
        // given
        String expectedUsername = "testuser";
        UserDetails userDetails = new User(expectedUsername, "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        String token = jwtTokenProvider.createToken(authentication);

        // when
        String username = jwtTokenProvider.getUsername(token);

        // then
        assertThat(username).isEqualTo(expectedUsername);
    }

    // ----- Authentication Extraction Tests -----

    @Test
    void shouldExtractAuthentication_whenTokenIsValid() {
        // given
        String expectedUsername = "testuser";
        List<SimpleGrantedAuthority> expectedAuthorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        UserDetails userDetails = new User(expectedUsername, "password", expectedAuthorities);
        Authentication originalAuth = new UsernamePasswordAuthenticationToken(
                userDetails, null, expectedAuthorities
        );
        String token = jwtTokenProvider.createToken(originalAuth);

        // when
        Authentication extractedAuth = jwtTokenProvider.getAuthentication(token);

        // then
        assertThat(extractedAuth).isNotNull();
        assertThat(extractedAuth.getName()).isEqualTo(expectedUsername);
        assertThat(extractedAuth.getAuthorities())
                .hasSize(2)
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void shouldPreserveAuthorities_inTokenRoundtrip() {
        // given
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_MANAGER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        UserDetails userDetails = new User("admin", "password", authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        // when
        String token = jwtTokenProvider.createToken(auth);
        Authentication extractedAuth = jwtTokenProvider.getAuthentication(token);

        // then
        assertThat(extractedAuth.getAuthorities())
                .hasSize(3)
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER", "ROLE_ADMIN");
    }

    // ----- Constructor Validation Tests -----

    @Test
    void shouldThrowException_whenSecretIsTooShort() {
        // given
        String shortSecret = "tooshort"; // less than 32 characters

        // when/then
        assertThatThrownBy(() -> new JwtTokenProvider(shortSecret, TEST_EXPIRATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void shouldThrowException_whenSecretIsNull() {
        // when/then
        assertThatThrownBy(() -> new JwtTokenProvider(null, TEST_EXPIRATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void shouldThrowException_whenSecretIsEmpty() {
        // when/then
        assertThatThrownBy(() -> new JwtTokenProvider("", TEST_EXPIRATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 characters");
    }
}
