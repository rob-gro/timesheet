document.addEventListener('DOMContentLoaded', function () {
    const filterForm = document.getElementById('filterForm');
    filterForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const formData = new FormData(this);
        const params = new URLSearchParams();

        for (let [key, value] of formData.entries()) {
            if (value) {
                params.append(key, value);
            }
        }
        window.location.href = '/timesheets/list?' + params.toString();
    });

    window.sortTable = function (column, direction) {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('sortBy', column);
        currentUrl.searchParams.set('sortDir', direction);

        const clientId = currentUrl.searchParams.get('clientId');
        const size = currentUrl.searchParams.get('size') || '10';

        document.querySelectorAll('.sort-icon').forEach(icon => {
            icon.classList.remove('active');
        });

        const activeIcon = document.querySelector(`.sort-icon[data-column="${column}"][data-direction="${direction}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active');
        }

        window.location.href = currentUrl.toString();
    };

    const currentUrl = new URL(window.location.href);
    const sortBy = currentUrl.searchParams.get('sortBy');
    const sortDir = currentUrl.searchParams.get('sortDir');

    if (sortBy && sortDir) {
        const activeIcon = document.querySelector(`.sort-icon[data-column="${sortBy}"][data-direction="${sortDir}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active');
        }
    }

    window.editTimesheet = function (id) {
        window.location.href = `/timesheets/edit/${id}`;
    }

    window.deleteTimesheet = function (id) {
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

    window.showDatePicker = function (btn) {
        const container = btn.parentElement;
        const dateContainer = container.querySelector('.date-input-container');
        btn.style.display = 'none';
        dateContainer.style.display = 'flex';
    }

    window.clearPaymentDate = function (timesheetId) {
        if (confirm('Are you sure you want to clear the payment date?')) {
            fetch(`/api/v1/timesheets/${timesheetId}/payment`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    paymentDate: null
                })
            })
                .then(response => {
                    if (response.ok) {
                        location.reload();
                    }
                });
        }
    }

    document.querySelectorAll('.confirm-date-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const dateInput = this.previousElementSibling;
            const timesheetId = dateInput.dataset.timesheetId;
            const selectedDate = dateInput.value;

            if (selectedDate) {
                fetch(`/api/v1/timesheets/${timesheetId}/payment`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        paymentDate: selectedDate
                    })
                })
                    .then(response => {
                        if (response.ok) {
                            location.reload();
                        }
                    });
            }
        });
    });
});
