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
        window.location.href = '/invoice-list?' + params.toString();
    });

    window.sortTable = function (column, direction) {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('sortBy', column);
        currentUrl.searchParams.set('sortDir', direction);

        document.querySelectorAll('.sort-icon').forEach(icon => {
            icon.classList.remove('active');
        });

        const activeIcon = document.querySelector(`.sort-icon[data-column="${column}"][data-direction="${direction}"]`);
        if (activeIcon) {
            activeIcon.classList.add('active');
        }

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
});
