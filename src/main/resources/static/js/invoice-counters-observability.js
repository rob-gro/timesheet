'use strict';

const loadBtn = document.getElementById('loadBtn');
const copyLinkBtn = document.getElementById('copyLinkBtn');
const sellerIdInput = document.getElementById('sellerIdInput');
const templateInfo = document.getElementById('templateInfo');
const loadingMessage = document.getElementById('loadingMessage');
const errorMessage = document.getElementById('errorMessage');
const emptyMessage = document.getElementById('emptyMessage');
const tableContainer = document.getElementById('tableContainer');
const countersTableBody = document.getElementById('countersTableBody');

// Auto-load on page open if sellerId is prefilled (from Thymeleaf or query param)
document.addEventListener('DOMContentLoaded', () => {
    if (sellerIdInput.value) {
        loadCounters();
    }
});

async function loadCounters() {
    const sellerId = sellerIdInput.value.trim();
    if (!sellerId) {
        showError('Please enter a Seller ID.');
        return;
    }

    resetUI();
    setLoading(true);

    try {
        const response = await fetch(`/internal/invoice-counters?sellerId=${encodeURIComponent(sellerId)}`, {
            credentials: 'same-origin'
        });

        if (response.status === 401) {
            showError('Not authorized — please log in again.');
            return;
        }
        if (response.status === 403) {
            showError('Access denied (ADMIN role required).');
            return;
        }
        if (response.status === 404) {
            showError('Seller not found.');
            return;
        }
        if (!response.ok) {
            showError(`Server error (HTTP ${response.status}). Check application logs.`);
            return;
        }

        const data = await response.json();
        renderCounters(data);
        copyLinkBtn.style.display = '';

    } catch (e) {
        showError('Network error — could not reach the server.');
    } finally {
        setLoading(false);
    }
}

function renderCounters(data) {
    // Show current template
    if (data.currentTemplate) {
        templateInfo.textContent = `Active template: ${data.currentTemplate}`;
        templateInfo.style.display = '';
    }

    if (!data.counters || data.counters.length === 0) {
        emptyMessage.style.display = '';
        return;
    }

    // Sort: drift first, then periodKey descending
    const sorted = [...data.counters].sort((a, b) => {
        if (a.hasDrift !== b.hasDrift) return a.hasDrift ? -1 : 1;
        return b.periodKey.localeCompare(a.periodKey);
    });

    countersTableBody.innerHTML = sorted.map(c => {
        const delta = c.expectedValue - c.lastValue;
        const statusIcon = c.hasDrift ? '❌' : '✅';
        const rowStyle = c.hasDrift ? 'background-color: rgba(244,7,32,0.15);' : '';
        const deltaDisplay = delta !== 0 ? `<span style="color:#f40720">${delta > 0 ? '+' : ''}${delta}</span>` : '0';
        const updatedAt = c.updatedAt ? c.updatedAt.replace('T', ' ').substring(0, 16) : '—';

        return `<tr style="${rowStyle}">
            <td>${c.periodKey}</td>
            <td>${c.resetPeriod}</td>
            <td>${c.lastValue}</td>
            <td>${c.invoiceCount}</td>
            <td>${c.expectedValue}</td>
            <td>${deltaDisplay}</td>
            <td>${c.lastInvoiceNumber ?? '—'}</td>
            <td>${updatedAt}</td>
            <td>${statusIcon}</td>
        </tr>`;
    }).join('');

    tableContainer.style.display = '';
}

function copyLink() {
    const sellerId = sellerIdInput.value.trim();
    const url = `${window.location.origin}/admin/invoice-counters?sellerId=${sellerId}`;
    navigator.clipboard.writeText(url).then(() => {
        const original = copyLinkBtn.textContent;
        copyLinkBtn.textContent = 'Copied!';
        setTimeout(() => { copyLinkBtn.textContent = original; }, 2000);
    });
}

function resetUI() {
    templateInfo.style.display = 'none';
    templateInfo.textContent = '';
    errorMessage.style.display = 'none';
    errorMessage.textContent = '';
    emptyMessage.style.display = 'none';
    tableContainer.style.display = 'none';
    countersTableBody.innerHTML = '';
    copyLinkBtn.style.display = 'none';
}

function setLoading(active) {
    loadingMessage.style.display = active ? '' : 'none';
    loadBtn.disabled = active;
    loadBtn.textContent = active ? 'Loading...' : 'Check';
}

function showError(msg) {
    errorMessage.textContent = msg;
    errorMessage.style.display = '';
}