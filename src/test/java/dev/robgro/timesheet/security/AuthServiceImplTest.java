package dev.robgro.timesheet.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    // ----- Login Tests -----

    @Test
    void shouldLoginSuccessfully_whenValidCredentials() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        Authentication mockAuth = mock(Authentication.class);
        String expectedToken = "jwt.token.here";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(tokenProvider.createToken(mockAuth))
                .thenReturn(expectedToken);

        // when
        JwtResponse response = authService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(expectedToken);
        assertThat(response.getType()).isEqualTo("Bearer");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(mockAuth);
    }

    @Test
    void shouldThrowException_whenInvalidCredentials() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // when/then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider, never()).createToken(any());
    }

    @Test
    void shouldAuthenticateWithCorrectCredentials() {
        // given
        String username = "admin";
        String password = "admin123";

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(tokenProvider.createToken(any())).thenReturn("token");

        // when
        authService.login(loginRequest);

        // then
        verify(authenticationManager).authenticate(
                argThat(auth ->
                        auth.getPrincipal().equals(username) &&
                        auth.getCredentials().equals(password)
                )
        );
    }

    // ----- Token Validation Tests -----

    @Test
    void shouldValidateToken_whenTokenIsValid() {
        // given
        String validToken = "valid.jwt.token";
        when(tokenProvider.validateToken(validToken)).thenReturn(true);

        // when
        boolean isValid = authService.validateToken(validToken);

        // then
        assertThat(isValid).isTrue();
        verify(tokenProvider).validateToken(validToken);
    }

    @Test
    void shouldReturnFalse_whenTokenIsInvalid() {
        // given
        String invalidToken = "invalid.jwt.token";
        when(tokenProvider.validateToken(invalidToken)).thenReturn(false);

        // when
        boolean isValid = authService.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
        verify(tokenProvider).validateToken(invalidToken);
    }

    @Test
    void shouldReturnFalse_whenTokenIsNull() {
        // given
        when(tokenProvider.validateToken(null)).thenReturn(false);

        // when
        boolean isValid = authService.validateToken(null);

        // then
        assertThat(isValid).isFalse();
        verify(tokenProvider).validateToken(null);
    }

    @Test
    void shouldReturnFalse_whenTokenIsEmpty() {
        // given
        String emptyToken = "";
        when(tokenProvider.validateToken(emptyToken)).thenReturn(false);

        // when
        boolean isValid = authService.validateToken(emptyToken);

        // then
        assertThat(isValid).isFalse();
        verify(tokenProvider).validateToken(emptyToken);
    }
}
