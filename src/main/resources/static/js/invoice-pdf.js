document.addEventListener('DOMContentLoaded', function () {
    console.log('JS loaded');
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
        window.location.href = '/invoices/archive/pdf?' + params.toString();
    });

    window.sortTable = function (column, direction) {
        const currentUrl = new URL(window.location.href);

        const params = {
            clientId: currentUrl.searchParams.get('clientId'),
            year: currentUrl.searchParams.get('year'),
            month: currentUrl.searchParams.get('month'),
            size: currentUrl.searchParams.get('size') || '10',
            page: '0',
            sortBy: column,
            sortDir: direction
        };

        const newUrl = new URL('/invoices/archive/pdf', window.location.origin);
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
