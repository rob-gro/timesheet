window.deleteClient = function(id) {
    if (confirm('Are you sure you want to deactivate this client? The client will be hidden but preserved for historical data.')) {
        fetch(`/api/v1/clients/${id}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    throw new Error('Failed to deactivate client');
                }
            })
            .catch(error => {
                console.error('Deactivation error:', error);
                alert('Failed to deactivate client. Please try again later.');
            });
    }
};
