window.toggleSellerActive = function(id, active) {
    const action = active ? 'activate' : 'deactivate';
    const confirmMessage = `Are you sure you want to ${action} this seller?`;

    if (confirm(confirmMessage)) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
                          || getCsrfTokenFromCookie();
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
                           || 'X-CSRF-TOKEN';

        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(`/sellers/${id}/${action}`, {
            method: 'POST',
            headers: headers
        })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    throw new Error(`Failed to ${action} seller`);
                }
            })
            .catch(error => {
                console.error(`${action} error:`, error);
                alert(`Failed to ${action} seller. Please try again later.`);
            });
    }
};

window.setSystemDefault = function(id) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
                      || getCsrfTokenFromCookie();
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
                       || 'X-CSRF-TOKEN';

    const headers = {
        'Content-Type': 'application/json'
    };

    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(`/sellers/set-system-default/${id}`, {
        method: 'POST',
        headers: headers
    })
        .then(response => {
            if (response.ok) {
                location.reload();
            } else {
                throw new Error('Failed to set system default seller');
            }
        })
        .catch(error => {
            console.error('Set system default error:', error);
            alert('Failed to set system default seller. Please try again later.');
        });
};

window.setDefaultSeller = function(id) {
    // Get CSRF token from meta tag or cookie
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
                      || getCsrfTokenFromCookie();
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
                       || 'X-CSRF-TOKEN';

    const headers = {
        'Content-Type': 'application/json'
    };

    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(`/sellers/set-default/${id}`, {
        method: 'POST',
        headers: headers
    })
        .then(response => {
            if (response.ok) {
                location.reload();
            } else {
                throw new Error('Failed to set default seller');
            }
        })
        .catch(error => {
            console.error('Set default error:', error);
            alert('Failed to set default seller. Please try again later.');
        });
};

function getCsrfTokenFromCookie() {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; XSRF-TOKEN=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}
