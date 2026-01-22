/**
 * Reset Password - JavaScript
 * Handles password reset via token link from email
 */

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('resetPasswordForm');
    if (!form) return;  // Form not shown if token invalid

    const errorDiv = document.getElementById('errorMessage');
    const successDiv = document.getElementById('successMessage');

    // Get CSRF token from meta tags
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    // Password toggle functionality
    document.querySelectorAll('.toggle-password').forEach(button => {
        button.addEventListener('click', function() {
            const targetId = this.getAttribute('data-target');
            const passwordInput = document.getElementById(targetId);

            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                this.classList.add('dimmed');
                this.setAttribute('aria-label', 'Hide password');
            } else {
                passwordInput.type = 'password';
                this.classList.remove('dimmed');
                this.setAttribute('aria-label', 'Show password');
            }
        });
    });

    // Form submission handler
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        errorDiv.style.display = 'none';
        successDiv.style.display = 'none';

        const token = document.getElementById('token').value;
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Client-side validation
        if (newPassword !== confirmPassword) {
            showError('Passwords do not match');
            return;
        }

        if (newPassword.length < 8) {
            showError('Password must be at least 8 characters');
            return;
        }

        try {
            const response = await fetch('/api/auth/reset-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({
                    token: token,
                    newPassword: newPassword,
                    confirmPassword: confirmPassword
                })
            });

            const data = await response.json();

            if (response.ok) {
                // Hide form and show success with instructions
                // Do NOT redirect - would overwrite session if another user is logged in
                form.style.display = 'none';
                showSuccessWithLoginOption(data.message || 'Password updated successfully.');
            } else {
                showError(data.error || 'Failed to update password');
            }
        } catch (error) {
            console.error('Error:', error);
            showError('An error occurred. Please try again.');
        }
    });

    /**
     * Display error message
     */
    function showError(message) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
        successDiv.style.display = 'none';
    }

    /**
     * Display success message
     */
    function showSuccess(message) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
        errorDiv.style.display = 'none';
    }

    /**
     * Display success message with login option.
     * Opens login in NEW WINDOW to avoid overwriting current session.
     */
    function showSuccessWithLoginOption(message) {
        successDiv.innerHTML = `
            <p><strong>${message}</strong></p>
            <p style="margin-top: 10px;">You can now close this tab and login with your new password.</p>
            <button type="button" onclick="window.open('/login', '_blank')"
                    style="margin-top: 15px; padding: 10px 20px; cursor: pointer;">
                Open Login Page (new window)
            </button>
        `;
        successDiv.style.display = 'block';
        errorDiv.style.display = 'none';
    }
});
