document.addEventListener('DOMContentLoaded', function () {

    const filterForm = document.getElementById('filterForm');

    filterForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const formData = new FormData(this);
        const params = new URLSearchParams();

        for (let [key, value] of formData.entries()) {
            if (value) {
                params.append(key, value);
            }
        }
        params.set('page', 0);
        params.set('size', 10);
        window.location.href = '/invoice-archive?' + params.toString();
    });

    window.sortTable = function (column, direction) {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('sortBy', column);
        currentUrl.searchParams.set('sortDir', direction);
        currentUrl.searchParams.set('page', 0);

        document.querySelectorAll('.sort-icon').forEach(icon => {
            icon.classList.remove('active');
        });

        const activeIcon = document.querySelector(`.sort-icon[data-column="${column}"][data-direction="${direction}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active');
        }

        window.location.href = currentUrl.toString();
    };

    window.changePage = function (newPage) {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('page', newPage);
        window.location.href = currentUrl.toString();
    };

    const currentUrl = new URL(window.location.href);
    const sortBy = currentUrl.searchParams.get('sortBy');
    const sortDir = currentUrl.searchParams.get('sortDir');

    if (sortBy && sortDir) {
        const activeIcon = document.querySelector(`.sort-icon[data-column="${sortBy}"][data-direction="${sortDir}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active');
        }
    }

    window.resendEmail = function (id) {
        fetch(`/api/v1/invoices/${id}/send-email`, {
            method: 'POST'
        })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    alert('Error sending email');
                }
            })
            .catch(error => {
                console.error('Email send error:', error);
                alert('Error occurred while sending email');
            });
    };

    window.deleteInvoice = function (id) {
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
                        },
                        body: JSON.stringify({
                            deleteTimesheets: result.isConfirmed,
                            detachFromClient: result.isConfirmed,
                        }),
                    })
                        .then(response => {
                            if (response.ok) {
                                Swal.fire('Deleted!', 'Invoice has been deleted.', 'success')
                                    .then(() => location.reload());
                            }
                        })
                        .catch(error => {
                            Swal.fire('Error!', 'Failed to delete invoice.', 'error');
                        });
                });
            }
        });
    };
});
