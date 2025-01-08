document.getElementById('clientFilter').addEventListener('change', function() {
    const clientId = this.value;

    if (clientId) {
        fetch(`/timesheets/filter/${clientId}`)
            .then(response => response.json())
            .then(timesheets => updateTimesheetsTable(timesheets))
            .catch(error => console.error('Error:', error));
    } else {
        window.location.reload();
    }
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
                <td>
                    <button class="view-btn" onclick="viewTimesheet(${timesheet.id})">View</button>
                    <button class="btn btn-primary" onclick="editTimesheet(${timesheet.id})">Edit</button>
                    <button class="delete-btn" onclick="deleteTimesheet(${timesheet.id})">Delete</button>
                </td>
            </tr>`;
    });
}
