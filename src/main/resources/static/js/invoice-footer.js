// Invoice Footer Settings - JavaScript
// Handles live preview of footer content

function updateFooterPreview() {
    var website = document.getElementById('website').value.trim();
    var email   = document.getElementById('email').value.trim();
    var phone   = document.getElementById('phone').value.trim();

    var parts = [];
    if (website) parts.push('🌐 ' + website);
    if (phone)   parts.push('📞 ' + phone);
    if (email)   parts.push('✉️ ' + email);

    var preview = document.getElementById('footerPreview');
    if (parts.length === 0) {
        preview.innerHTML = '<span class="text-muted">—</span>';
    } else {
        preview.textContent = parts.join('  |  ');
    }
}

document.addEventListener('DOMContentLoaded', updateFooterPreview);