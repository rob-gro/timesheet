/**
 * Forgot Password - JavaScript
 * Handles password reset request via email
 */

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('forgotPasswordForm');
    const errorDiv = document.getElementById('errorMessage');
    const successDiv = document.getElementById('successMessage');

    // Get CSRF token from meta tags
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    // Form submission handler
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        errorDiv.style.display = 'none';
        successDiv.style.display = 'none';

        const email = document.getElementById('email').value;

        try {
            const response = await fetch('/api/auth/forgot-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ email: email })
            });

            const data = await response.json();

            if (response.ok) {
                showSuccess(data.message || 'If that email exists in our system, a reset link has been sent.');
                form.reset();
            } else {
                showError(data.error || 'An error occurred. Please try again.');
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
