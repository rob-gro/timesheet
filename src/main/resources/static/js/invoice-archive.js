document.addEventListener('DOMContentLoaded', function() {

    const filterForm = document.getElementById('filterForm');
    filterForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const formData = new FormData(this);
        const params = new URLSearchParams();

        for (let [key, value] of formData.entries()) {
            if (value) {  // dodaj tylko niepuste wartości
                params.append(key, value);
            }
        }
        window.location.href = '/invoice-archive?' + params.toString();
    });

    window.resendEmail = function(id) {
        fetch(`/api/v1/invoices/${id}/send-email`, {
            method: 'POST'
        })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    alert('Error sending email');
                }
            })
            .catch(error => {
                console.error('Email send error:', error);
                alert('Error occurred while sending email');
            });
    };

    window.deleteInvoice = function(id) {
        if (confirm('Are you sure you want to delete this invoice?')) {
            fetch(`/api/v1/invoices/${id}`, {
                method: 'DELETE'
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    } else {
                        alert('Error deleting invoice');
                    }
                })
                .catch(error => {
                    console.error('Delete error:', error);
                    alert('Error occurred while deleting invoice');
                });
        }
    };
});
