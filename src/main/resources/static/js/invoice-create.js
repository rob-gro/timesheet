let invoiceId;

document.addEventListener('DOMContentLoaded', function() {
    invoiceId = document.getElementById('invoiceId').value;
});

function savePdfAndSendEmail() {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const button = document.querySelector('.save-button');
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = 'Processing...';

    fetch(`/invoice-create/${invoiceId}/save-and-send`, {
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
                    window.location.href = '/';
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
