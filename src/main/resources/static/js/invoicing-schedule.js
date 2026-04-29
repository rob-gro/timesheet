/**
 * Invoicing Schedule Settings — timezone search + live next-run preview.
 */
document.addEventListener('DOMContentLoaded', function () {

    // --- Timezone preselection ---
    const tzSelect = document.getElementById('timezone');
    if (tzSelect && tzSelect.dataset.current) {
        tzSelect.value = tzSelect.dataset.current;
    }

    // --- Timezone search filter ---
    const tzSearch = document.getElementById('timezoneSearch');
    if (tzSearch && tzSelect) {
        tzSearch.addEventListener('input', function () {
            const filter = this.value.toLowerCase();
            for (const opt of tzSelect.options) {
                opt.hidden = filter.length > 0 && !opt.value.toLowerCase().includes(filter);
            }
            // If currently selected is hidden, don't change selection — user can still save it
        });
    }

    // --- Enabled toggle: dim fields when disabled ---
    const enabledToggle = document.getElementById('enabledToggle');
    const scheduleFields = document.getElementById('scheduleFields');
    if (enabledToggle && scheduleFields) {
        function updateFieldsState() {
            scheduleFields.style.opacity = enabledToggle.checked ? '1' : '0.5';
            scheduleFields.querySelectorAll('select, input').forEach(function (el) {
                el.disabled = !enabledToggle.checked;
            });
        }
        enabledToggle.addEventListener('change', updateFieldsState);
        updateFieldsState();
    }

    // --- Live next-run preview ---
    const daySelect   = document.getElementById('scheduledDay');
    const hourSelect  = document.getElementById('scheduledHour');
    const previewDiv  = document.getElementById('jsNextRunPreview');

    function updatePreview() {
        if (!daySelect || !hourSelect || !previewDiv) return;

        const day  = parseInt(daySelect.value, 10);
        const hour = parseInt(hourSelect.value, 10);
        const tz   = (tzSelect && tzSelect.value) ? tzSelect.value : 'UTC';

        if (isNaN(day) || isNaN(hour)) {
            previewDiv.textContent = 'Select values above to preview next run';
            return;
        }

        const now = new Date();
        // Estimate next run month: if today's day/hour is before scheduled → this month, else next month
        let year  = now.getFullYear();
        let month = now.getMonth(); // 0-indexed

        const todayDay  = now.getDate();
        const todayHour = now.getHours();

        if (todayDay > day || (todayDay === day && todayHour >= hour)) {
            // This month's slot already passed → next month
            month += 1;
            if (month > 11) { month = 0; year++; }
        }

        // Clamp day to last day of month
        const lastDay = new Date(year, month + 1, 0).getDate();
        const effectiveDay = Math.min(day, lastDay);

        // Build candidate date (local — we can't do true TZ conversion in plain JS without Intl abuse)
        const candidate = new Date(year, month, effectiveDay, hour, 0, 0);

        if (candidate <= now) {
            previewDiv.innerHTML = '<span class="status-badge status-fail" style="padding:2px 8px;">Due now / Overdue</span>';
            return;
        }

        const dd   = String(effectiveDay).padStart(2, '0');
        const mm   = String(month + 1).padStart(2, '0');
        const hh   = String(hour).padStart(2, '0');
        previewDiv.textContent = dd + '/' + mm + '/' + year + ' at ' + hh + ':00 (' + tz + ')';
    }

    if (daySelect)  daySelect.addEventListener('change', updatePreview);
    if (hourSelect) hourSelect.addEventListener('change', updatePreview);
    if (tzSelect)   tzSelect.addEventListener('change', updatePreview);

    updatePreview();
});