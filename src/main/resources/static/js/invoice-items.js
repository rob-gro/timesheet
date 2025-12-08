document.addEventListener('DOMContentLoaded', function () {
    const clientSelect = document.getElementById('clientId');
    const totalAmountElement = document.querySelector('.total-amount');
    const timesheetCheckboxes = document.querySelectorAll('.timesheet-select');
    const rows = document.querySelectorAll('table tbody tr');
    const form = document.querySelector('form');

    function updateAmounts() {
        let total = 0;

        timesheetCheckboxes.forEach(checkbox => {
            const row = checkbox.closest('tr');
            const rateCell = row.querySelector('.hourly-rate');
            const amountCell = row.querySelector('.amount');
            const rate = parseFloat(checkbox.dataset.rate) || 0;

            // Display the rate for this timesheet
            rateCell.textContent = rate.toFixed(2);

            if (checkbox.checked) {
                const duration = parseFloat(checkbox.dataset.duration) || 0;
                const amount = duration * rate;
                amountCell.textContent = amount.toFixed(2);
                total += amount;
            } else {
                amountCell.textContent = '';
            }
        });

        totalAmountElement.textContent = total.toFixed(2);
    }

    function filterRowsByClient() {
        const selectedClientId = clientSelect.value;
        rows.forEach(row => {
            const rowClientId = row.getAttribute('data-clientId');
            const checkbox = row.querySelector('.timesheet-select');
            if (!selectedClientId || rowClientId === selectedClientId) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
                // Uncheck hidden timesheets
                if (checkbox && checkbox.checked) {
                    checkbox.checked = false;
                }
            }
        });
    }

    function validateClientSelection(checkbox) {
        const selectedClientId = clientSelect.value;

        if (!selectedClientId) {
            alert('Please select a client first!');
            checkbox.checked = false;
            return false;
        }

        const row = checkbox.closest('tr');
        const rowClientId = row.getAttribute('data-clientId');

        if (rowClientId !== selectedClientId) {
            alert('Cannot select this timesheet! It belongs to a different client than the one selected.');
            checkbox.checked = false;
            return false;
        }

        return true;
    }

    clientSelect.addEventListener('change', () => {
        filterRowsByClient();
        updateAmounts();
    });

    timesheetCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            if (this.checked && !validateClientSelection(this)) {
                updateAmounts();
                return;
            }
            updateAmounts();
        });
    });

    // Validate on form submit
    form.addEventListener('submit', function(e) {
        const selectedClientId = clientSelect.value;

        if (!selectedClientId) {
            e.preventDefault();
            alert('Please select a client!');
            return false;
        }

        const checkedTimesheets = Array.from(timesheetCheckboxes).filter(cb => cb.checked);

        if (checkedTimesheets.length === 0) {
            e.preventDefault();
            alert('Please select at least one timesheet!');
            return false;
        }

        // Validate all checked timesheets belong to selected client
        const invalidTimesheets = checkedTimesheets.filter(checkbox => {
            const row = checkbox.closest('tr');
            const rowClientId = row.getAttribute('data-clientId');
            return rowClientId !== selectedClientId;
        });

        if (invalidTimesheets.length > 0) {
            e.preventDefault();
            alert('Error: Some selected timesheets belong to different clients! Please review your selection.');
            return false;
        }
    });

    filterRowsByClient();
    updateAmounts();
});
