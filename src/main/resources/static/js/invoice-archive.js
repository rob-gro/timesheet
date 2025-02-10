document.addEventListener('DOMContentLoaded', function () {
    console.log('JS loaded');
    const filterForm = document.getElementById('filterForm');
    const fromYear = document.getElementById('fromYear');
    const fromMonth = document.getElementById('fromMonth');
    const toYear = document.getElementById('toYear');
    const toMonth = document.getElementById('toMonth');
    const dateRangeError = document.getElementById('dateRangeError');

    function validateDateRange() {
        if (!fromYear.value || !fromMonth.value || !toYear.value || !toMonth.value) {
            return true; // Pozwalamy na niekompletne daty
        }

        const fromDate = new Date(fromYear.value, fromMonth.value - 1);
        const toDate = new Date(toYear.value, toMonth.value - 1);

        const monthDiff = (toDate.getFullYear() - fromDate.getFullYear()) * 12
            + toDate.getMonth() - fromDate.getMonth();

        if (monthDiff < 0) {
            dateRangeError.textContent = 'End date cannot be earlier than start date';
            return false;
        }
        return true;
    }

    function updateDateRangeValidation() {
        const isValid = validateDateRange();
        dateRangeError.style.display = isValid ? 'none' : 'block';
        return isValid;
    }

    [fromYear, fromMonth, toYear, toMonth].forEach(element => {
        element.addEventListener('change', updateDateRangeValidation);
    });

    filterForm.addEventListener('submit', function(e) {
        e.preventDefault();
        if (!validateDateRange()) {
            return;
        }

        const formData = new FormData(this);
        const params = new URLSearchParams();

        for (let [key, value] of formData.entries()) {
            if (value) {
                params.append(key, value);
            }
        }
        params.set('page', 0);
        window.location.href = '/invoice-archive?' + params.toString();
    });

    window.sortTable = function(column, direction) {
        const currentUrl = new URL(window.location.href);

        const params = {
            clientId: currentUrl.searchParams.get('clientId'),
            fromYear: currentUrl.searchParams.get('fromYear'),
            fromMonth: currentUrl.searchParams.get('fromMonth'),
            toYear: currentUrl.searchParams.get('toYear'),
            toMonth: currentUrl.searchParams.get('toMonth'),
            size: currentUrl.searchParams.get('size') || '10',
            page: '0',
            sortBy: column,
            sortDir: direction
        };

        const newUrl = new URL('/invoice-archive', window.location.origin);
        Object.entries(params).forEach(([key, value]) => {
            if (value) newUrl.searchParams.set(key, value);
        });

        document.querySelectorAll('.sort-icon').forEach(icon => {
            icon.classList.remove('active');
        });

        const activeIcon = document.querySelector(
            `.sort-icon[data-column="${column}"][data-direction="${direction}"]`
        );
        if (activeIcon) {
            activeIcon.classList.add('active');
        }

        window.location.href = newUrl.toString();
    };

    window.deleteInvoice = function(id) {
        Swal.fire({
            title: 'Delete Invoice',
            text: 'Are you sure you want to delete this invoice?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes',
            cancelButtonText: 'No'
        }).then((result) => {
            if (result.isConfirmed) {
                Swal.fire({
                    title: 'Delete Timesheets',
                    text: 'Do you also want to delete the associated timesheets?',
                    icon: 'question',
                    showCancelButton: true,
                    confirmButtonText: 'Yes',
                    cancelButtonText: 'No'
                }).then((result) => {
                    fetch(`/api/v1/invoices/${id}/delete`, {
                        method: 'DELETE',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify({
                            deleteTimesheets: result.isConfirmed,
                            detachFromClient: !result.isConfirmed
                        }),
                    })
                        .then(response => {
                            if (response.ok) {
                                Swal.fire('Deleted!', 'Invoice has been deleted.', 'success')
                                    .then(() => location.reload());
                            } else {
                                throw new Error('Failed to delete');
                            }
                        })
                        .catch(error => {
                            Swal.fire('Error!', 'Failed to delete invoice.', 'error');
                        });
                });
            }
        });
    };

    const currentUrl = new URL(window.location.href);
    const sortBy = currentUrl.searchParams.get('sortBy');
    const sortDir = currentUrl.searchParams.get('sortDir');

    if (sortBy && sortDir) {
        const activeIcon = document.querySelector(
            `.sort-icon[data-column="${sortBy}"][data-direction="${sortDir}"]`
        );
        if (activeIcon) {
            activeIcon.classList.add('active');
        }
    }
});
