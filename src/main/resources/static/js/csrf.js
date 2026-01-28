/**
 * CSRF Protection Utilities
 *
 * Provides helper functions for including CSRF tokens in fetch() requests.
 * Used across all JavaScript files that make state-changing API calls.
 *
 * @file csrf.js
 */

/**
 * Get CSRF headers object for fetch() requests
 * Exposed globally to ensure availability across all scripts
 * @returns {Object} Headers object with CSRF token
 */
window.getCsrfHeaders = function() {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    if (!token) {
        // Note: Public pages (e.g., /login) don't have CSRF tokens - this is expected
        // Using debug level to avoid "false alarm" noise in console
        console.debug('CSRF token not found in page meta tags');
        return {};
    }

    return { [headerName]: token };
};

/**
 * Get CSRF token value
 * @returns {string|null} CSRF token or null if not found
 */
window.getCsrfToken = function() {
    return document.querySelector('meta[name="_csrf"]')?.content || null;
};

/**
 * Get CSRF header name
 * @returns {string} CSRF header name (defaults to 'X-CSRF-TOKEN')
 */
window.getCsrfHeaderName = function() {
    return document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
};
