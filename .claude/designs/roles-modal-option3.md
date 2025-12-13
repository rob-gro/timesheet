# Opcja 3: Modal Overlay z Backdrop

## Wygląd:
Pełnoekranowe okno modalne z zaciemnionym tłem - jak w nowoczesnych aplikacjach.

## Preview (ASCII):
```
█████████████████████████████████████████
█░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░█  (Ciemne tło)
█░░  ┌───────────────────────────┐  ░░█
█░░  │ ✖                          │  ░░█
█░░  │  Manage User Roles         │  ░░█
█░░  ├───────────────────────────┤  ░░█
█░░  │                            │  ░░█
█░░  │  User: John Doe            │  ░░█
█░░  │                            │  ░░█
█░░  │  Select Roles:             │  ░░█
█░░  │                            │  ░░█
█░░  │  ☑ 👑 Administrator        │  ░░█
█░░  │  ☑ 👤 User                 │  ░░█
█░░  │  ☐ 👁️ Guest                │  ░░█
█░░  │                            │  ░░█
█░░  │  [Save Changes]  [Cancel]  │  ░░█
█░░  └───────────────────────────┘  ░░█
█░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░█
█████████████████████████████████████████
```

## CSS (dodaj do modal.css):
```css
/* Backdrop Overlay */
.modal-backdrop {
    display: none;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.7);
    backdrop-filter: blur(5px);
    z-index: 999;
    animation: fadeIn 0.3s ease;
}

.modal-backdrop.active {
    display: block;
}

@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}

/* Modal Box */
.roles-modal {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: linear-gradient(135deg, #1e2936 0%, #161b22 100%);
    border: 2px solid #ffc008;
    border-radius: 15px;
    padding: 0;
    width: 90%;
    max-width: 500px;
    max-height: 80vh;
    overflow: hidden;
    z-index: 1000;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.8);
    animation: slideIn 0.3s ease;
}

@keyframes slideIn {
    from {
        opacity: 0;
        transform: translate(-50%, -45%);
    }
    to {
        opacity: 1;
        transform: translate(-50%, -50%);
    }
}

/* Modal Header */
.modal-header {
    background: linear-gradient(90deg, #ffc008 0%, #ff8c00 100%);
    padding: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 2px solid #ff8c00;
}

.modal-title {
    font-size: 20px;
    font-weight: bold;
    color: #000;
    margin: 0;
}

.modal-close {
    background: none;
    border: none;
    font-size: 24px;
    color: #000;
    cursor: pointer;
    padding: 0;
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background 0.2s;
}

.modal-close:hover {
    background: rgba(0, 0, 0, 0.1);
}

/* Modal Body */
.modal-body {
    padding: 30px;
    max-height: 60vh;
    overflow-y: auto;
}

.user-info {
    background: #0d1117;
    border: 1px solid #30363d;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 25px;
}

.user-info strong {
    color: #ffc008;
}

.user-info span {
    color: #c9d1d9;
    font-size: 16px;
}

/* Checkboxes với Style */
.role-checkbox-fancy {
    display: flex;
    align-items: center;
    padding: 15px;
    background: #0d1117;
    border: 2px solid #21262d;
    border-radius: 10px;
    margin-bottom: 15px;
    cursor: pointer;
    transition: all 0.3s ease;
}

.role-checkbox-fancy:hover {
    border-color: #ffc008;
    background: #161b22;
    transform: translateX(5px);
}

.role-checkbox-fancy input[type="checkbox"] {
    width: 24px;
    height: 24px;
    margin-right: 15px;
    cursor: pointer;
    accent-color: #4CAF50;
}

.role-checkbox-fancy input[type="checkbox"]:checked + label {
    color: #4CAF50;
}

.role-checkbox-fancy label {
    font-size: 16px;
    color: #c9d1d9;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 10px;
    flex: 1;
    margin: 0;
}

.role-checkbox-fancy label .emoji {
    font-size: 24px;
}

/* Modal Footer */
.modal-footer {
    padding: 20px 30px;
    background: #0d1117;
    border-top: 1px solid #30363d;
    display: flex;
    gap: 10px;
    justify-content: flex-end;
}

.modal-footer button {
    padding: 12px 24px;
    font-size: 14px;
    font-weight: bold;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
}

.modal-footer .btn-primary {
    background: #238636;
    border: 1px solid #2ea043;
    color: white;
}

.modal-footer .btn-primary:hover {
    background: #2ea043;
    box-shadow: 0 0 15px rgba(35, 134, 54, 0.5);
}

.modal-footer .btn-secondary {
    background: transparent;
    border: 1px solid #ffc008;
    color: #ffc008;
}

.modal-footer .btn-secondary:hover {
    background: rgba(255, 192, 8, 0.1);
}
```

## HTML (zamień cały rolesFormSection):
```html
<!-- Modal Backdrop -->
<div class="modal-backdrop" id="rolesBackdrop"></div>

<!-- Roles Modal -->
<div class="roles-modal" id="rolesModal" style="display: none;">
    <div class="modal-header">
        <h2 class="modal-title">Manage User Roles</h2>
        <button class="modal-close" onclick="cancelRolesForm()">✖</button>
    </div>

    <div class="modal-body">
        <div class="user-info">
            <strong>User:</strong> <span id="rolesUsername"></span>
        </div>

        <input type="hidden" id="rolesUserId">

        <div class="roles-container-fancy">
            <label class="role-checkbox-fancy">
                <input type="checkbox" id="roleAdmin" value="ROLE_ADMIN">
                <label for="roleAdmin">
                    <span class="emoji">👑</span>
                    <span>Administrator - Full system access</span>
                </label>
            </label>

            <label class="role-checkbox-fancy">
                <input type="checkbox" id="roleUser" value="ROLE_USER">
                <label for="roleUser">
                    <span class="emoji">👤</span>
                    <span>User - Standard access</span>
                </label>
            </label>

            <label class="role-checkbox-fancy">
                <input type="checkbox" id="roleGuest" value="ROLE_GUEST">
                <label for="roleGuest">
                    <span class="emoji">👁️</span>
                    <span>Guest - Read-only access</span>
                </label>
            </label>
        </div>
    </div>

    <div class="modal-footer">
        <button class="btn-primary" id="saveRolesBtn">Save Changes</button>
        <button class="btn-secondary" onclick="cancelRolesForm()">Cancel</button>
    </div>
</div>
```

## JavaScript (update w users.js):
```javascript
function showRolesForm(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('rolesUserId').value = user.id;
    document.getElementById('rolesUsername').textContent = user.username;

    // Reset checkboxes
    document.getElementById('roleAdmin').checked = false;
    document.getElementById('roleUser').checked = false;
    document.getElementById('roleGuest').checked = false;

    // Set current roles
    if (user.roles) {
        if (user.roles.includes('ROLE_ADMIN')) document.getElementById('roleAdmin').checked = true;
        if (user.roles.includes('ROLE_USER')) document.getElementById('roleUser').checked = true;
        if (user.roles.includes('ROLE_GUEST')) document.getElementById('roleGuest').checked = true;
    }

    // Show modal with backdrop
    document.getElementById('rolesBackdrop').classList.add('active');
    document.getElementById('rolesModal').style.display = 'block';
    document.getElementById('userListSection').style.display = 'none';
}

function cancelRolesForm() {
    document.getElementById('rolesBackdrop').classList.remove('active');
    document.getElementById('rolesModal').style.display = 'none';
    document.getElementById('userListSection').style.display = 'block';
}

// Close modal when clicking backdrop
document.getElementById('rolesBackdrop').addEventListener('click', cancelRolesForm);
```

## Zalety:
✅ Najbardziej profesjonalny wygląd
✅ Focus użytkownika na modal (zaciemnione tło)
✅ Płynne animacje (fade in, slide in)
✅ Przycisk X do zamknięcia
✅ Możliwość zamknięcia przez kliknięcie w tło
✅ Responsywne i scrollowalne
✅ Nowoczesny gradient header

## Wady:
❌ Najwięcej kodu do dodania
❌ Zmienia całą strukturę formularza
