document.addEventListener('DOMContentLoaded', function() {
    const clientSelect = document.querySelector('select[name="clientId"]');
    const yearSelect = document.querySelector('select[name="year"]');
    const monthSelect = document.querySelector('select[name="month"]');

    // Aktualizacja miesięcy przy zmianie roku
    yearSelect?.addEventListener('change', function() {
        if (!this.value) {
            monthSelect.value = '';  // Reset miesiąca gdy rok nie jest wybrany
        }
    });

    // Funkcja usuwania faktury
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
