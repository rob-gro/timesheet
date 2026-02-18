// Invoice Numbering Settings - JavaScript
// Handles preset selection, custom templates, and live preview

// Helper function for getElementById
function $(id) {
    return document.getElementById(id);
}

// Preset card selection (non-custom presets)
document.querySelectorAll('.preset-card:not(.custom-card)').forEach(card => {
    card.addEventListener('click', function() {
        // Remove selection from all cards
        document.querySelectorAll('.preset-card').forEach(c => c.classList.remove('selected'));

        // Select this card
        this.classList.add('selected');

        // Fill form fields
        const selectedTemplate = $('selectedTemplate');
        const resetPeriodSelect = $('resetPeriodSelect');
        const customEditor = $('customTemplateEditor');

        if (selectedTemplate) selectedTemplate.value = this.dataset.template;
        if (resetPeriodSelect) resetPeriodSelect.value = this.dataset.reset;
        if (customEditor) customEditor.style.display = 'none';

        // Update preview
        updatePreview();
    });
});

// Custom card selection
const customCard = $('customCard');
if (customCard) {
    customCard.addEventListener('click', function() {
        document.querySelectorAll('.preset-card').forEach(c => c.classList.remove('selected'));
        this.classList.add('selected');

        const customEditor = $('customTemplateEditor');
        const selectedTemplate = $('selectedTemplate');

        if (customEditor) customEditor.style.display = 'block';
        if (selectedTemplate) selectedTemplate.value = '';

        updateTemplateRequired();
    });
}

// Template input change
const templateInput = $('templateInput');
if (templateInput) {
    templateInput.addEventListener('input', function() {
        const selectedTemplate = $('selectedTemplate');
        if (selectedTemplate) selectedTemplate.value = this.value;
        updatePreview();
    });
}

// Department preview input change
const deptPreviewInput = $('deptPreviewInput');
if (deptPreviewInput) {
    deptPreviewInput.addEventListener('input', updatePreview);
}

// Reset period change
const resetPeriodSelect = $('resetPeriodSelect');
if (resetPeriodSelect) {
    resetPeriodSelect.addEventListener('change', updatePreview);
}

// Live preview update - generates 3 example invoice numbers
function updatePreview() {
    const selectedTemplate = $('selectedTemplate');
    const resetPeriodSelect = $('resetPeriodSelect');
    const deptPreviewInput = $('deptPreviewInput');
    const preview1El = $('preview1');
    const preview2El = $('preview2');
    const preview3El = $('preview3');

    if (!selectedTemplate || !preview1El || !preview2El || !preview3El) {
        return;
    }

    const template = selectedTemplate.value;
    const resetPeriod = resetPeriodSelect ? resetPeriodSelect.value : '';
    const deptCode = deptPreviewInput ? deptPreviewInput.value : '';

    if (!template) {
        preview1El.textContent = '---';
        preview2El.textContent = '---';
        preview3El.textContent = '---';
        return;
    }

    // Generate 3 example invoice numbers (sequence 1, 2, 3)
    for (let seq = 1; seq <= 3; seq++) {
        let preview = template;

        // Replace {SEQ:N} with padded sequence number
        preview = preview.replace(/{SEQ:(\d+)}/g, (match, digits) =>
            String(seq).padStart(parseInt(digits, 10), '0')
        );

        // Replace year tokens
        preview = preview.replace(/{YYYY}/g, '2026');
        preview = preview.replace(/{YY}/g, '26');

        // Replace month tokens always — {MM}/{M} is a display token, not tied to reset period
        preview = preview.replace(/{MM}/g, '02');
        preview = preview.replace(/{M}/g, '2');

        // Replace department tokens (if department preview is provided)
        if (deptCode) {
            preview = preview.replace(/{DEPT}/g, deptCode.toUpperCase());
            preview = preview.replace(/{DEPT_NAME}/g, deptCode.toUpperCase());
        } else {
            // Remove department tokens if no department (with optional separators)
            preview = preview.replace(/-?\{DEPT\}-?/g, '');
            preview = preview.replace(/\/?\{DEPT\}\/?/g, '');
            preview = preview.replace(/{DEPT_NAME}/g, '');
        }

        // Update corresponding preview element
        if (seq === 1) preview1El.textContent = preview;
        if (seq === 2) preview2El.textContent = preview;
        if (seq === 3) preview3El.textContent = preview;
    }
}

// Dynamic validation: Custom template required when custom format selected
function updateTemplateRequired() {
    const customCard = $('customCard');
    const templateInput = $('templateInput');

    if (!customCard || !templateInput) return;

    const isCustomSelected = customCard.classList.contains('selected');
    templateInput.required = isCustomSelected;

    if (!isCustomSelected) {
        templateInput.classList.remove('is-invalid');
    }
}

// Form submit validation
const schemeForm = document.querySelector('form');
if (schemeForm) {
    schemeForm.addEventListener('submit', function(e) {
        const templateInput = $('templateInput');

        if (templateInput && templateInput.required && !templateInput.value.trim()) {
            e.preventDefault();
            templateInput.classList.add('is-invalid');
            templateInput.focus();
            return false;
        }
    });
}

// Load current active scheme into the editor and preview
function loadCurrentScheme() {
    const banner = document.getElementById('currentSchemeBanner');
    if (!banner) return;
    const template = banner.dataset.template;
    const reset = banner.dataset.reset;
    if (!template) return;

    const selectedTemplate = $('selectedTemplate');
    const templateInp = $('templateInput');
    const resetPeriodSel = $('resetPeriodSelect');
    const customEditor = $('customTemplateEditor');
    const customCrd = $('customCard');

    // Deselect all preset cards, select custom
    document.querySelectorAll('.preset-card').forEach(c => c.classList.remove('selected'));
    if (customCrd) customCrd.classList.add('selected');

    // Fill form fields
    if (selectedTemplate) selectedTemplate.value = template;
    if (templateInp) templateInp.value = template;
    if (resetPeriodSel && reset) resetPeriodSel.value = reset;
    if (customEditor) customEditor.style.display = 'block';

    updatePreview();
}

// Initialize - load active template into custom editor for preview, but keep reset period at server default (MONTHLY)
// "Load into editor" button loads full active scheme including reset period
document.addEventListener('DOMContentLoaded', function() {
    const livePreview = $('livePreview');
    const activeTemplate = livePreview ? livePreview.dataset.activeTemplate : '';
    const selectedTemplate = $('selectedTemplate');

    if (activeTemplate && selectedTemplate && !selectedTemplate.value) {
        // Show active template in custom editor for preview — do NOT touch reset period
        const templateInp = $('templateInput');
        const customEditor = $('customTemplateEditor');
        const customCrd = $('customCard');

        document.querySelectorAll('.preset-card').forEach(c => c.classList.remove('selected'));
        if (customCrd) customCrd.classList.add('selected');
        if (selectedTemplate) selectedTemplate.value = activeTemplate;
        if (templateInp) templateInp.value = activeTemplate;
        if (customEditor) customEditor.style.display = 'block';
        updatePreview();
    } else if (selectedTemplate && !selectedTemplate.value) {
        const firstPreset = document.querySelector('.preset-card:not(.custom-card)');
        if (firstPreset) firstPreset.click();
    }
});