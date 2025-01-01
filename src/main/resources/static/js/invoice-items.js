document.addEventListener('DOMContentLoaded', function () {
    const clientSelect = document.getElementById('clientId');
    const totalAmountElement = document.querySelector('.total-amount');
    const timesheetCheckboxes = document.querySelectorAll('.timesheet-select');
    const rows = document.querySelectorAll('table tbody tr');

    function updateAmounts() {
        const selectedClient = clientSelect.options[clientSelect.selectedIndex];
        const rate = selectedClient ? parseFloat(selectedClient.dataset.rate) || 0 : 0;

        // Update rate display
        document.querySelectorAll('.hourly-rate').forEach(el => {
            el.textContent = !isNaN(rate) ? rate.toFixed(2) : '0.00';
        });

        let total = 0;
        timesheetCheckboxes.forEach(checkbox => {
            const row = checkbox.closest('tr');
            const amountCell = row.querySelector('.amount');

            if (checkbox.checked && !isNaN(rate)) {
                const duration = parseFloat(checkbox.dataset.duration) || 0;
                const amount = duration * rate;
                amountCell.textContent = amount.toFixed(2);
                total += amount;
            } else {
                amountCell.textContent = '';
            }
        });

        totalAmountElement.textContent = total.toFixed(2);
    }0

    function filterRowsByClient() {
        const selectedClientId = clientSelect.value;
        rows.forEach(row => {
            const rowClientId = row.getAttribute('data-clientId');
            if (!selectedClientId || rowClientId === selectedClientId) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    }

    clientSelect.addEventListener('change', () => {
        filterRowsByClient();
        updateAmounts();
    });

    timesheetCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateAmounts);
    });

    // Wywołaj inicjalnie na starcie
    filterRowsByClient();
    updateAmounts();
});
