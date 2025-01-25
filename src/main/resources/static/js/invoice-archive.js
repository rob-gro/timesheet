document.addEventListener('DOMContentLoaded', function () {

    const filterForm = document.getElementById('filterForm');
    filterForm.addEventListener('submit', function (e) {
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

    window.sortTable = function (column, direction) {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('sortBy', column);
        currentUrl.searchParams.set('sortDir', direction);

        // Usuń istniejącą klasę "active" z innych ikon
        document.querySelectorAll('.sort-icon').forEach(icon => {
            icon.classList.remove('active');
        });

        // Znajdź kliknięty element i dodaj mu klasę "active"
        const activeIcon = document.querySelector(`.sort-icon[data-column="${column}"][data-direction="${direction}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active');
        }

        window.location.href = currentUrl.toString();
    };

    // Funkcja do oznaczenia aktywnej ikony sortowania po załadowaniu strony
    const currentUrl = new URL(window.location.href);
    const sortBy = currentUrl.searchParams.get('sortBy'); // Pobierz aktualną kolumnę

    console.log(' ######################   sortBy:', sortBy);


    const sortDir = currentUrl.searchParams.get('sortDir'); // Pobierz aktualny kierunek sortowania

    console.log('#######################  sortDir:', sortDir);

    if (sortBy && sortDir) {
        const activeIcon = document.querySelector(`.sort-icon[data-column="${sortBy}"][data-direction="${sortDir}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active'); // Dodaj klasę "active" do ikony

            console.log('@@@@@@@@@@@@@@@@  Active Icon:', activeIcon);
        }
    }

    window.resendEmail = function (id) {
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

    window.deleteInvoice = function (id) {
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
