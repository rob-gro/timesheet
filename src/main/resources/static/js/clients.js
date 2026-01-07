window.toggleClientActive = function(id, active) {
    const action = active ? 'activate' : 'deactivate';
    if (confirm(`Are you sure you want to ${action} this client?`)) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

        fetch(`/clients/${id}/${action}`, {
            method: 'POST',
            headers: { [csrfHeader]: csrfToken }
        })
        .then(response => response.ok ? location.reload() : Promise.reject())
        .catch(() => alert(`Failed to ${action} client`));
    }
};
