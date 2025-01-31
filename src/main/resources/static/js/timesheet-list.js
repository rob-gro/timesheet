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
        window.location.href = '/timesheets/list?' + params.toString();
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

    // Oznacz aktywną ikonę sortowania po załadowaniu strony
    const currentUrl = new URL(window.location.href);
    const sortBy = currentUrl.searchParams.get('sortBy');
    const sortDir = currentUrl.searchParams.get('sortDir');

    if (sortBy && sortDir) {
        const activeIcon = document.querySelector(`.sort-icon[data-column="${sortBy}"][data-direction="${sortDir}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active');
        }
    }

    // Funkcje do edycji i usuwania timesheet'ów
    window.editTimesheet = function(id) {
        window.location.href = `/timesheets/edit/${id}`;
    }

    window.deleteTimesheet = function(id) {
        if (confirm('Are you sure you want to delete this timesheet?')) {
            fetch(`/api/v1/timesheets/${id}`, {
                method: 'DELETE'
            })
                .then(response => {
                    if (response.ok) {
                        window.location.reload();
                    } else {
                        throw new Error('Failed to delete timesheet');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error deleting timesheet. It might be attached to an invoice.');
                });
        }
    }
});
