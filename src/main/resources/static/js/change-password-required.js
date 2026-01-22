/**
 * Change Password Required - JavaScript
 * Handles forced password change after admin reset
 */

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('changePasswordForm');
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
                this.classList.add('dimmed'); // Dim the eye when password is visible
                this.setAttribute('aria-label', 'Hide password');
            } else {
                passwordInput.type = 'password';
                this.classList.remove('dimmed'); // Full opacity when password is hidden
                this.setAttribute('aria-label', 'Show password');
            }
        });
    });

    // Form submission handler
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        errorDiv.style.display = 'none';
        successDiv.style.display = 'none';

        const currentPassword = document.getElementById('currentPassword').value;
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Client-side validation
        if (newPassword !== confirmPassword) {
            showError('New password and confirmation do not match');
            return;
        }

        if (newPassword.length < 6) {
            showError('Password must be at least 6 characters long');
            return;
        }

        try {
            const response = await fetch('/api/auth/change-password-required', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({
                    currentPassword: currentPassword,
                    newPassword: newPassword,
                    confirmPassword: confirmPassword
                })
            });

            const data = await response.json();

            if (response.ok) {
                showSuccess(data.message || 'Password changed successfully');
                form.reset();

                // Redirect to login after 2 seconds
                setTimeout(() => {
                    window.location.href = '/login?message=Password changed successfully. Please login with your new password.';
                }, 2000);
            } else {
                showError(data.message || 'Failed to change password. Please check your input and try again.');
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
});
