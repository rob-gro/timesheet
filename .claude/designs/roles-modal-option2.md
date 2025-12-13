# Opcja 2: Eleganckie Toggle Switches

## Wygląd:
Przełączniki iOS-style zamiast checkboxów - bardziej eleganckie.

## Preview (ASCII):
```
┌─────────────────────────────────────┐
│  Manage User Roles                  │
│  User: John Doe                     │
├─────────────────────────────────────┤
│                                      │
│  👑  Administrator                  │
│      Full system access       [ON]  │
│                                      │
│  👤  User                           │
│      Standard access          [ON]  │
│                                      │
│  👁️  Guest                          │
│      Read-only access         [OFF] │
│                                      │
│  [Save Roles]      [Cancel]         │
└─────────────────────────────────────┘
```

## CSS (dodaj do modal.css):
```css
/* Toggle Switch Container */
.roles-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin: 20px 0;
}

.role-item {
    background: #1e2936;
    border: 1px solid #2d3748;
    border-radius: 10px;
    padding: 15px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    transition: all 0.3s ease;
}

.role-item:hover {
    background: #253345;
    border-color: #ffc008;
}

.role-info {
    display: flex;
    align-items: center;
    gap: 15px;
    flex: 1;
}

.role-emoji {
    font-size: 32px;
}

.role-details {
    flex: 1;
}

.role-title {
    font-size: 16px;
    font-weight: bold;
    color: #ffc008;
    margin-bottom: 5px;
}

.role-desc {
    font-size: 13px;
    color: #8b949e;
}

/* Toggle Switch */
.toggle-switch {
    position: relative;
    width: 60px;
    height: 30px;
}

.toggle-switch input[type="checkbox"] {
    opacity: 0;
    width: 0;
    height: 0;
}

.toggle-slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #404040;
    transition: 0.4s;
    border-radius: 30px;
}

.toggle-slider:before {
    position: absolute;
    content: "";
    height: 22px;
    width: 22px;
    left: 4px;
    bottom: 4px;
    background-color: white;
    transition: 0.4s;
    border-radius: 50%;
}

.toggle-switch input:checked + .toggle-slider {
    background-color: #4CAF50;
}

.toggle-switch input:checked + .toggle-slider:before {
    transform: translateX(30px);
}

.toggle-slider:hover {
    box-shadow: 0 0 10px rgba(76, 175, 80, 0.5);
}
```

## HTML (zamień w users.html linie 123-139):
```html
<div class="form-row">
    <label>Roles:</label>
    <div class="roles-list">
        <div class="role-item">
            <div class="role-info">
                <div class="role-emoji">👑</div>
                <div class="role-details">
                    <div class="role-title">Administrator</div>
                    <div class="role-desc">Full system access and management</div>
                </div>
            </div>
            <label class="toggle-switch">
                <input type="checkbox" id="roleAdmin" value="ROLE_ADMIN">
                <span class="toggle-slider"></span>
            </label>
        </div>

        <div class="role-item">
            <div class="role-info">
                <div class="role-emoji">👤</div>
                <div class="role-details">
                    <div class="role-title">User</div>
                    <div class="role-desc">Standard access to core features</div>
                </div>
            </div>
            <label class="toggle-switch">
                <input type="checkbox" id="roleUser" value="ROLE_USER">
                <span class="toggle-slider"></span>
            </label>
        </div>

        <div class="role-item">
            <div class="role-info">
                <div class="role-emoji">👁️</div>
                <div class="role-details">
                    <div class="role-title">Guest</div>
                    <div class="role-desc">Read-only access to information</div>
                </div>
            </div>
            <label class="toggle-switch">
                <input type="checkbox" id="roleGuest" value="ROLE_GUEST">
                <span class="toggle-slider"></span>
            </label>
        </div>
    </div>
</div>
```

## Zalety:
✅ Bardzo intuicyjne (ON/OFF)
✅ Profesjonalny wygląd
✅ Płynne animacje
✅ Dobrze znane z iOS/Material Design
✅ Łatwe w obsłudze na mobile

## Wady:
❌ Bardziej skomplikowany CSS
