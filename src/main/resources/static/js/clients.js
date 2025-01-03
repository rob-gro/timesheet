document.addEventListener('DOMContentLoaded', function() {
    window.deleteClient = function(id) {
        if (confirm('Are you sure you want to delete this client?')) {
            fetch(`/api/v1/clients/${id}`, {
                method: 'DELETE'
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    } else {
                        alert('Error deleting client');
                    }
                })
                .catch(error => {
                    console.error('Delete error:', error);
                    alert('Error occurred while deleting client');
                });
        }
    };
});
