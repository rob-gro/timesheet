package dev.robgro.timesheet.passwordreset;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.InvalidTokenException;
import dev.robgro.timesheet.exception.TokenAlreadyUsedException;
import dev.robgro.timesheet.exception.TokenExpiredException;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PasswordResetApiControllerTest {

    @Mock
    private PasswordResetTokenService tokenService;

    @Mock
    private PasswordResetEmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordResetApiController controller;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    // ----- Admin Reset Password Tests -----

    @Test
    void shouldSendResetEmail_whenAdminResetsPassword() throws MessagingException {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenService.createResetToken(
                eq(1L),
                eq(PasswordResetToken.ResetRequestType.ADMIN),
                anyString(),
                anyString()
        )).thenReturn("abc123token");

        // when
        ResponseEntity<Map<String, String>> response =
                controller.adminResetPassword(1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Password reset email sent");

        verify(tokenService).createResetToken(
                eq(1L),
                eq(PasswordResetToken.ResetRequestType.ADMIN),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
        verify(emailService).sendResetLinkEmail(
                eq("test@example.com"),
                eq("testuser"),
                eq("abc123token"),
                eq(30)
        );
    }

    @Test
    void shouldReturn404_whenAdminResetsNonExistentUser() throws MessagingException {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        ResponseEntity<Map<String, String>> response =
                controller.adminResetPassword(999L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");

        verify(tokenService, never()).createResetToken(anyLong(), any(), anyString(), anyString());
        verify(emailService, never()).sendResetLinkEmail(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void shouldReturn500_whenEmailSendingFails() throws MessagingException {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenService.createResetToken(anyLong(), any(), anyString(), anyString()))
                .thenReturn("token123");
        doThrow(new MessagingException("SMTP error"))
                .when(emailService).sendResetLinkEmail(anyString(), anyString(), anyString(), anyInt());

        // when
        ResponseEntity<Map<String, String>> response =
                controller.adminResetPassword(1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsKey("error");
    }

    // ----- Forgot Password Tests -----

    @Test
    void shouldReturnGenericMessage_whenEmailExists() throws MessagingException {
        // given
        ForgotPasswordDto dto = new ForgotPasswordDto("test@example.com");

        when(rateLimitService.isRateLimited(anyString(), anyString())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(tokenService.createResetToken(anyLong(), any(), anyString(), anyString()))
                .thenReturn("token123");

        // when
        ResponseEntity<Map<String, String>> response =
                controller.forgotPassword(dto, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry(
                "message",
                "If that email exists in our system, a reset link has been sent."
        );

        verify(rateLimitService).recordAttempt(anyString(), anyString());
        verify(emailService).sendResetLinkEmail(
                eq("test@example.com"),
                eq("testuser"),
                eq("token123"),
                eq(15)
        );
    }

    @Test
    void shouldReturnGenericMessage_whenEmailNotFound() throws MessagingException {
        // given
        ForgotPasswordDto dto = new ForgotPasswordDto("nonexistent@example.com");

        when(rateLimitService.isRateLimited(anyString(), anyString())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // when
        ResponseEntity<Map<String, String>> response =
                controller.forgotPassword(dto, request);

        // then
        // IMPORTANT: Generic message to prevent email enumeration
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry(
                "message",
                "If that email exists in our system, a reset link has been sent."
        );

        verify(rateLimitService).recordAttempt(anyString(), anyString());
        verify(emailService, never()).sendResetLinkEmail(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void shouldReturn429_whenRateLimited() {
        // given
        ForgotPasswordDto dto = new ForgotPasswordDto("test@example.com");

        when(rateLimitService.isRateLimited(anyString(), anyString())).thenReturn(true);

        // when
        ResponseEntity<Map<String, String>> response =
                controller.forgotPassword(dto, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody().get("error")).contains("Too many requests");

        verify(rateLimitService, never()).recordAttempt(anyString(), anyString());
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void shouldReturnGenericMessage_whenEmailServiceThrowsException() throws MessagingException {
        // given
        ForgotPasswordDto dto = new ForgotPasswordDto("test@example.com");

        when(rateLimitService.isRateLimited(anyString(), anyString())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(tokenService.createResetToken(anyLong(), any(), anyString(), anyString()))
                .thenReturn("token123");
        doThrow(new MessagingException("SMTP error"))
                .when(emailService).sendResetLinkEmail(anyString(), anyString(), anyString(), anyInt());

        // when
        ResponseEntity<Map<String, String>> response =
                controller.forgotPassword(dto, request);

        // then
        // IMPORTANT: Still return generic message (don't leak errors)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry(
                "message",
                "If that email exists in our system, a reset link has been sent."
        );
    }

    // ----- Reset Password Tests -----

    @Test
    void shouldResetPassword_whenTokenIsValid() {
        // given
        ResetPasswordDto dto = new ResetPasswordDto(
                "validtoken123",
                "newPassword123",
                "newPassword123"
        );

        doNothing().when(tokenService).consumeToken("validtoken123", "newPassword123");

        // when
        ResponseEntity<Map<String, String>> response =
                controller.resetPassword(dto);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody().get("message")).contains("successfully");

        verify(tokenService).consumeToken("validtoken123", "newPassword123");
    }

    @Test
    void shouldReturn409_whenTokenAlreadyUsed() {
        // given
        ResetPasswordDto dto = new ResetPasswordDto(
                "usedtoken",
                "newPassword123",
                "newPassword123"
        );

        doThrow(new TokenAlreadyUsedException("Token has already been used"))
                .when(tokenService).consumeToken("usedtoken", "newPassword123");

        // when
        ResponseEntity<Map<String, String>> response =
                controller.resetPassword(dto);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void shouldReturn404_whenTokenExpired() {
        // given
        ResetPasswordDto dto = new ResetPasswordDto(
                "expiredtoken",
                "newPassword123",
                "newPassword123"
        );

        doThrow(new TokenExpiredException("Token has expired"))
                .when(tokenService).consumeToken("expiredtoken", "newPassword123");

        // when
        ResponseEntity<Map<String, String>> response =
                controller.resetPassword(dto);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void shouldReturn404_whenTokenInvalid() {
        // given
        ResetPasswordDto dto = new ResetPasswordDto(
                "invalidtoken",
                "newPassword123",
                "newPassword123"
        );

        doThrow(new InvalidTokenException("Invalid token"))
                .when(tokenService).consumeToken("invalidtoken", "newPassword123");

        // when
        ResponseEntity<Map<String, String>> response =
                controller.resetPassword(dto);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void shouldReturn500_whenUnexpectedErrorOccurs() {
        // given
        ResetPasswordDto dto = new ResetPasswordDto(
                "token",
                "newPassword123",
                "newPassword123"
        );

        doThrow(new RuntimeException("Database error"))
                .when(tokenService).consumeToken("token", "newPassword123");

        // when
        ResponseEntity<Map<String, String>> response =
                controller.resetPassword(dto);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsKey("error");
    }

    // ----- X-Forwarded-For Tests -----

    @Test
    void shouldExtractIpFromXForwardedFor_whenHeaderPresent() throws MessagingException {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenService.createResetToken(anyLong(), any(), eq("192.168.1.100"), anyString()))
                .thenReturn("token");

        // when
        controller.adminResetPassword(1L, request);

        // then
        verify(tokenService).createResetToken(
                anyLong(),
                any(),
                eq("192.168.1.100"),  // First IP from X-Forwarded-For
                anyString()
        );
    }
}
