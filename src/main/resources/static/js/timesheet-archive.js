document.addEventListener('DOMContentLoaded', function() {

    window.detachTimesheet = function(id) {
        if (confirm('Are you sure you want to detach this timesheet from invoice?')) {
            fetch(`/api/v1/timesheets/${id}/detach`, {
                method: 'POST',
                headers: {
                    ...getCsrfHeaders()
                }
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    } else {
                        alert('Error detaching timesheet');
                    }
                })
                .catch(error => {
                    console.error('Detach error:', error);
                    alert('Error occurred while detaching timesheet');
                });
        }
    };

    window.deleteTimesheet = function(id) {
        if (confirm('Are you sure you want to delete this timesheet?')) {
            fetch(`/api/v1/timesheets/${id}`, {
                method: 'DELETE',
                headers: {
                    ...getCsrfHeaders()
                }
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    } else {
                        alert('Error deleting timesheet');
                    }
                })
                .catch(error => {
                    console.error('Delete error:', error);
                    alert('Error occurred while deleting timesheet');
                });
        }
    };
});
