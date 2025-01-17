document.addEventListener('DOMContentLoaded', function() {
    function refreshTimesheets() {
        const clientId = document.getElementById('clientFilter').value;
        const status = document.getElementById('statusFilter').value;

        let url = '/api/v1/timesheets';
        if (clientId) {
            url += `/client/${clientId}`;
        }
        if (status !== '') {
            url += `${clientId ? '?' : '?'}invoiced=${status}`;
        }

        fetch(url)
            .then(response => response.json())
            .then(timesheets => updateTimesheetsTable(timesheets))
            .catch(error => console.error('Error:', error));
    }

    document.getElementById('clientFilter').addEventListener('change', refreshTimesheets);
    document.getElementById('statusFilter').addEventListener('change', refreshTimesheets);
});

function updateTimesheetsTable(timesheets) {
    const tbody = document.querySelector('.table tbody');
    tbody.innerHTML = '';

    timesheets.forEach(timesheet => {
        tbody.innerHTML += `
            <tr>
                <td>${timesheet.clientName}</td>
                <td>${timesheet.serviceDate}</td>
                <td>${timesheet.duration}</td>
                <td>${timesheet.isInvoice ? 'Invoiced' : 'Not Invoiced'}</td>
                <td>
                    <button class="save-button" onclick="editTimesheet(${timesheet.id})">Edit</button>
                    <button class="del-button" onclick="deleteTimesheet(${timesheet.id})">Delete</button>
                </td>
            </tr>`;
    });
}

function editTimesheet(id) {
    window.location.href = `/timesheets/edit/${id}`;
}

function deleteTimesheet(id) {
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
