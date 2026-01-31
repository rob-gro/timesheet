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

        const clientId = currentUrl.searchParams.get('clientId');
        const paymentStatus = currentUrl.searchParams.get('paymentStatus');
        const size = currentUrl.searchParams.get('size') || '10';
        const page = '0';

        currentUrl.searchParams.set('sortBy', column);
        currentUrl.searchParams.set('sortDir', direction);

        if (clientId) currentUrl.searchParams.set('clientId', clientId);
        if (paymentStatus) currentUrl.searchParams.set('paymentStatus', paymentStatus);
        if (size) currentUrl.searchParams.set('size', size);
        currentUrl.searchParams.set('page', page);

        document.querySelectorAll('.sort-icon').forEach(icon => {
            icon.classList.remove('active');
        });

        const activeIcon = document.querySelector(
            `.sort-icon[data-column="${column}"][data-direction="${direction}"]`
        );
        if (activeIcon) {
            activeIcon.classList.add('active');
        }

        window.location.href = currentUrl.toString();
    };

    window.editTimesheet = function (id) {
        window.location.href = `/timesheets/edit/${id}`;
    }

    window.deleteTimesheet = function (id) {
        if (confirm('Are you sure you want to delete this timesheet?')) {
            fetch(`/api/v1/timesheets/${id}`, {
                method: 'DELETE',
                headers: {
                    ...getCsrfHeaders()
                }
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

    // Accordion: Toggle details on row click (MOBILE only)
    document.querySelectorAll('.timesheet-item .row').forEach(row => {
        row.addEventListener('click', function(e) {
            // Don't toggle if clicking status badge (handled separately)
            if (e.target.closest('.status')) {
                return;
            }

            const details = this.nextElementSibling;
            if (!details || !details.classList.contains('details')) return;

            const isOpen = this.getAttribute('data-open') === 'true';
            this.setAttribute('data-open', String(!isOpen));
            details.style.display = isOpen ? 'none' : 'block';
        });
    });

    // Status badge click handler (MOBILE only) - using event delegation
    document.querySelectorAll('.status').forEach(badge => {
        badge.addEventListener('click', function(e) {
            e.stopPropagation(); // Prevent row accordion toggle

            const timesheetId = this.getAttribute('data-timesheet-id');
            const isPaid = this.getAttribute('data-is-paid') === 'true';

            if (isPaid) {
                // PAID → UNPAID (clear date)
                if (confirm('Are you sure you want to clear the payment date?')) {
                    fetch(`/api/v1/timesheets/${timesheetId}/payment`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            ...getCsrfHeaders()
                        },
                        body: JSON.stringify({ paymentDate: null })
                    })
                    .then(response => {
                        if (response.ok) {
                            location.reload();
                        }
                    })
                    .catch(error => {
                        console.error('Error clearing payment date:', error);
                        alert('Failed to clear payment date');
                    });
                }
            } else {
                // UNPAID → Show date picker
                const picker = document.getElementById(`payment-picker-${timesheetId}`);
                if (picker) {
                    picker.style.display = picker.style.display === 'none' ? 'flex' : 'none';

                    // Expand details if not already open
                    const row = this.closest('.row');
                    const details = row.nextElementSibling;
                    if (details && details.style.display === 'none') {
                        details.style.display = 'block';
                        row.setAttribute('data-open', 'true');
                    }
                }
            }
        });
    });

    // Confirm payment date from picker - MOBILE
    document.querySelectorAll('.confirm-payment-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const timesheetId = this.getAttribute('data-timesheet-id');
            const input = document.getElementById(`payment-input-${timesheetId}`);
            const selectedDate = input.value;

            if (!selectedDate) {
                alert('Please select a date');
                return;
            }

            fetch(`/api/v1/timesheets/${timesheetId}/payment`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getCsrfHeaders()
                },
                body: JSON.stringify({ paymentDate: selectedDate })
            })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    throw new Error('Failed to set payment date');
                }
            })
            .catch(error => {
                console.error('Error setting payment date:', error);
                alert('Failed to set payment date');
            });
        });
    });

    // Cancel payment date picker - MOBILE
    document.querySelectorAll('.cancel-payment-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const timesheetId = this.getAttribute('data-timesheet-id');
            const picker = document.getElementById(`payment-picker-${timesheetId}`);
            if (picker) {
                picker.style.display = 'none';
            }
        });
    });

    // DESKTOP: Old payment date handlers (keep for desktop table)
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
                    'Content-Type': 'application/json',
                    ...getCsrfHeaders()
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
                        'Content-Type': 'application/json',
                        ...getCsrfHeaders()
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
