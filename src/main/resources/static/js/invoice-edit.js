document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('editInvoiceForm');
    const invoiceId = form.getAttribute('data-id');
    const clientSelect = document.getElementById('clientId');
    const totalAmountElement = document.getElementById('totalAmount');
    const addItemButton = document.getElementById('addItemButton');
    const cancelButton = document.getElementById('cancelButton');

    function calculateTotal() {
        let total = 0;
        document.querySelectorAll('.item-amount').forEach(input => {
            const amount = parseFloat(input.value) || 0;
            total += amount;
        });
        totalAmountElement.textContent = total.toFixed(2);
    }

    function updateAmount(row) {
        const durationInput = row.querySelector('.item-duration');
        const amountInput = row.querySelector('.item-amount');
        const duration = parseFloat(durationInput.value) || 0;
        const rate = parseFloat(clientSelect.options[clientSelect.selectedIndex].dataset.rate) || 0;

        const amount = duration * rate;
        amountInput.value = amount.toFixed(2);

        calculateTotal();
    }

    addItemButton.addEventListener('click', function() {
        const tbody = document.querySelector('#invoiceItemsTable tbody');
        const newRow = document.createElement('tr');
        newRow.setAttribute('data-item-id', '');

        newRow.innerHTML = `
            <td>
                <input type="date" class="form-control item-date" required>
            </td>
            <td>
                <input type="text" class="form-control item-description" 
                       value="Cleaning services" required>
            </td>
            <td>
                <input type="number" step="0.5" min="0.5" class="form-control item-duration" value="1" required>
            </td>
            <td>
                <input type="number" step="0.01" min="0" class="form-control item-amount" required>
            </td>
            <td>
                <button type="button" class="del-button remove-item">Remove</button>
            </td>
        `;

        tbody.appendChild(newRow);

        const dateInput = newRow.querySelector('.item-date');
        const today = new Date();
        dateInput.value = today.toISOString().split('T')[0];

        // Attach event listeners to new row
        attachRowEventListeners(newRow);

        // Update amount for new row
        updateAmount(newRow);
    });

    function attachRowEventListeners(row) {
        const durationInput = row.querySelector('.item-duration');
        const removeButton = row.querySelector('.remove-item');

        durationInput.addEventListener('input', function() {
            updateAmount(row);
        });

        removeButton.addEventListener('click', function() {
            row.remove();
            calculateTotal();
        });
    }

    document.querySelectorAll('#invoiceItemsTable tbody tr').forEach(row => {
        attachRowEventListeners(row);
    });

    clientSelect.addEventListener('change', function() {
        console.log('Selected client index:', this.selectedIndex);
        console.log('Selected client value:', this.value);
        console.log('Selected client rate:', this.options[this.selectedIndex].dataset.rate);
        document.querySelectorAll('#invoiceItemsTable tbody tr').forEach(row => {
            updateAmount(row);
        });
    });


    form.addEventListener('submit', function(e) {
        e.preventDefault();

        const formData = {
            clientId: parseInt(clientSelect.value),
            issueDate: document.querySelector('input[name="issueDate"]').value,
            invoiceNumber: document.querySelector('input[name="invoiceNumber"]').value,
            items: []
        };

        document.querySelectorAll('#invoiceItemsTable tbody tr').forEach(row => {
            const itemId = row.getAttribute('data-item-id') || null;
            const itemData = {
                id: itemId ? parseInt(itemId) : null,
                serviceDate: row.querySelector('.item-date').value,
                description: row.querySelector('.item-description').value,
                duration: parseFloat(row.querySelector('.item-duration').value),
                amount: parseFloat(row.querySelector('.item-amount').value)
            };
            formData.items.push(itemData);
        });

        fetch(`/api/v1/invoices/${invoiceId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to update invoice');
                }
                return response.json();
            })
            .then(data => {
                alert('Invoice updated successfully');
                window.location.href = '/invoices/archive';
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error updating invoice: ' + error.message);
            });
    });

    cancelButton.addEventListener('click', function() {
        window.location.href = '/invoices/archive';
    });

    calculateTotal();
});
