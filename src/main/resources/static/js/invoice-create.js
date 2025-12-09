let invoiceId;

document.addEventListener('DOMContentLoaded', function () {
    invoiceId = document.getElementById('invoiceId').value;
});

function savePdfAndSendEmail() {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const button = document.querySelector('.save-button');
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = 'Processing...';

    fetch(`/invoices/create/${invoiceId}/save-and-send`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [header]: token
        },
        credentials: 'same-origin'
    })
        .then(response => {
            if (response.ok) {
                alert('😀 Invoice has been saved and sent to the client 😀');
                setTimeout(() => {
                    window.location.href = '/invoices/items';
                }, 3000);
            } else {
                throw new Error(`Status: ${response.status}`);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('There was an error processing the invoice');
            button.disabled = false;
            button.textContent = originalText;
        });
}

function createAndSendInvoice() {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    // Get form data from hidden fields
    const clientId = document.getElementById('clientId').value;
    const issueDate = document.getElementById('issueDate').value;
    const timesheetIdsStr = document.getElementById('timesheetIds').value;
    const timesheetIds = timesheetIdsStr.split(',').map(id => parseInt(id.trim()));

    const buttons = document.querySelectorAll('.save-button');
    buttons.forEach(button => {
        button.disabled = true;
        if (button.textContent.includes('Generate')) {
            button.textContent = 'Processing...';
        }
    });

    fetch('/invoices/create/confirm', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [header]: token
        },
        credentials: 'same-origin',
        body: JSON.stringify({
            clientId: parseInt(clientId),
            issueDate: issueDate,
            timesheetIds: timesheetIds
        })
    })
        .then(response => {
            if (response.ok) {
                alert('😀 Invoice has been created and sent to the client 😀');
                setTimeout(() => {
                    window.location.href = '/invoices/items';
                }, 3000);
            } else {
                return response.text().then(text => {
                    throw new Error(`Status: ${response.status}, Message: ${text}`);
                });
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('There was an error creating the invoice: ' + error.message);
            buttons.forEach(button => {
                button.disabled = false;
                if (button.textContent.includes('Processing')) {
                    button.textContent = 'Generate & Send PDF Invoice';
                }
            });
        });
}
