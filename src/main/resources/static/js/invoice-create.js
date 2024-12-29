let invoiceId;

document.addEventListener('DOMContentLoaded', function() {
    invoiceId = document.getElementById('invoiceId').value;
});

function savePdfAndSendEmail() {
    fetch(`/invoice-create/${invoiceId}/save-and-send`, {
        method: 'POST'
    })
        .then(response => {
            if (response.ok) {
                alert('Invoice has been saved and sent to the client');
            } else {
                alert('There was an error processing the invoice');
            }
        });
}
