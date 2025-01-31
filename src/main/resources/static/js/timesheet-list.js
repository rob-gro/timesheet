document.addEventListener('DOMContentLoaded', function () {
        const clientFilter = document.getElementById('clientFilter');
        const statusFilter = document.getElementById('statusFilter');

        function refreshTimesheets() {
            const clientId = clientFilter.value;
            const status = statusFilter.value;

            let url = '/api/v1/timesheets';
            if (clientId) {
                url += `/client/${clientId}`;
                if (status !== '') {
                    url += `?invoiced=${status}`;
                }
            } else if (status !== '') {
                url += `/status/${status}`;
            }

            fetch(url)
                .then(response => response.json())
                .then(timesheets => updateTimesheetsTable(timesheets))
                .catch(error => console.error('Error:', error));
        }

        clientFilter.addEventListener('change', refreshTimesheets);
        statusFilter.addEventListener('change', refreshTimesheets);

        function updateTimesheetsTable(timesheets) {
            const tbody = document.querySelector('.table tbody');
            tbody.innerHTML = '';

            timesheets.forEach(timesheet => {
                tbody.innerHTML += `
               <tr>
                   <td>${timesheet.clientName}</td>
                   <td>${timesheet.serviceDate}</td>
                   <td>${timesheet.duration}</td>
                   <td>${timesheet.invoiced ? timesheet.invoiceNumber : '---'}</td>
                   <td>
                       <button class="save-button" onclick="editTimesheet(${timesheet.id})">Edit</button>
                       <button class="del-button" onclick="deleteTimesheet(${timesheet.id})">Delete</button>
                   </td>
               </tr>`;
            });
        }
    }
);
