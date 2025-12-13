# Opcja 1: Nowoczesne karty z ikonami

## Wygląd:
Każda rola jako kolorowa karta z ikoną i opisem.

## Preview (ASCII):
```
┌─────────────────────────────────────────┐
│  Manage User Roles - John Doe           │
├─────────────────────────────────────────┤
│                                          │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │  👑      │  │  👤      │  │  👁️    ││
│  │  ADMIN   │  │  USER    │  │  GUEST ││
│  │  ✓       │  │  ✓       │  │        ││
│  │  Full    │  │  Standard│  │  Read  ││
│  │  Access  │  │  Access  │  │  Only  ││
│  └──────────┘  └──────────┘  └────────┘│
│     Active      Active      Inactive    │
│                                          │
│  [Save Roles]          [Cancel]         │
└─────────────────────────────────────────┘
```

## CSS (dodaj do modal.css):
```css
/* Role Cards */
.roles-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    margin: 20px 0;
}

.role-card {
    background: linear-gradient(135deg, #2a3b3b 0%, #1a2a2a 100%);
    border: 2px solid #404040;
    border-radius: 12px;
    padding: 20px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
}

.role-card:hover {
    transform: translateY(-5px);
    border-color: #ffc008;
    box-shadow: 0 10px 25px rgba(255, 192, 8, 0.2);
}

.role-card.active {
    border-color: #4CAF50;
    background: linear-gradient(135deg, #2d4a2d 0%, #1d3a1d 100%);
}

.role-card.active::after {
    content: '✓';
    position: absolute;
    top: 10px;
    right: 10px;
    background: #4CAF50;
    color: white;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: bold;
}

.role-icon {
    font-size: 48px;
    margin-bottom: 10px;
}

.role-name {
    font-size: 18px;
    font-weight: bold;
    color: #ffc008;
    margin: 10px 0;
}

.role-description {
    font-size: 12px;
    color: #999;
}

/* Hide default checkbox */
.role-card input[type="checkbox"] {
    display: none;
}
```

## HTML (zamień w users.html linie 123-139):
```html
<div class="form-row">
    <label>Roles:</label>
    <div class="roles-grid">
        <div class="role-card" data-role="admin">
            <input type="checkbox" id="roleAdmin" value="ROLE_ADMIN">
            <div class="role-icon">👑</div>
            <div class="role-name">ADMIN</div>
            <div class="role-description">Full Access</div>
        </div>
        <div class="role-card" data-role="user">
            <input type="checkbox" id="roleUser" value="ROLE_USER">
            <div class="role-icon">👤</div>
            <div class="role-name">USER</div>
            <div class="role-description">Standard Access</div>
        </div>
        <div class="role-card" data-role="guest">
            <input type="checkbox" id="roleGuest" value="ROLE_GUEST">
            <div class="role-icon">👁️</div>
            <div class="role-name">GUEST</div>
            <div class="role-description">Read Only</div>
        </div>
    </div>
</div>
```

## JavaScript (dodaj do users.js):
```javascript
// Po załadowaniu strony, dodaj event listeners do kart
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.role-card').forEach(card => {
        card.addEventListener('click', function() {
            const checkbox = this.querySelector('input[type="checkbox"]');
            checkbox.checked = !checkbox.checked;
            this.classList.toggle('active', checkbox.checked);
        });
    });
});

// W funkcji showRolesForm, dodaj aktualizację kart:
function showRolesForm(userId) {
    // ... istniejący kod ...

    // Aktualizuj wizualny stan kart
    document.querySelectorAll('.role-card').forEach(card => {
        const checkbox = card.querySelector('input[type="checkbox"]');
        card.classList.toggle('active', checkbox.checked);
    });
}
```

## Zalety:
✅ Bardzo wizualne - łatwo zobaczyć jakie role są aktywne
✅ Nowoczesny design
✅ Responsywne (można kliknąć całą kartę)
✅ Animacje przy hover
✅ Ikony pomagają w identyfikacji ról

## Wady:
❌ Zajmuje więcej miejsca
❌ Wymaga więcej kodu CSS/JS
