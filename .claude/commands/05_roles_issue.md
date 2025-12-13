# Problem: Przycisk "Roles" nie działa na stronie /users

## Analiza problemu

### Zdiagnozowane błędy:

1. **Błędy składni w `users.js`** (CRITICAL):
   - **Linia 159:** przypadkowy tekst `es`
   - **Linia 175:** przypadkowy tekst `t`

   Te artefakty prawdopodobnie powstały przez przypadkowe usunięcie kodu i powodują błędy JavaScript podczas wykonywania skryptu.

2. **Backend jest OK**:
   - ✅ Endpoint `PUT /api/v1/users/{id}/roles` istnieje (UserController.java:114-120)
   - ✅ Funkcja `saveUserRoles()` w JS wygląda poprawnie (users.js:261-291)
   - ✅ Formularz HTML jest poprawny (users.html:111-147)

### Dlaczego to nie działa:

JavaScript parser natrafia na błędy składni (`es` i `t`) i **nie może wykonać całego skryptu**, przez co:
- Funkcja `showRolesForm()` może nie działać poprawnie
- Event listeners mogą nie być przypisane
- Przycisk "Roles" wywołuje błąd lub nic nie robi

---

## Plan naprawczy

### KROK 1: Napraw błędy składni w users.js

**Lokalizacja:** `src/main/resources/static/js/users.js`

#### Naprawa 1 - Linia 159 (w funkcji `showRolesForm`):
```javascript
// PRZED (BŁĄD):
    document.getElementById('rolesUsername').textContent = user.username;
es  // ← USUŃ TĘ LINIĘ!
    document.getElementById('roleAdmin').checked = false;

// PO (POPRAWNIE):
    document.getElementById('rolesUsername').textContent = user.username;
    // Linia 159 usunięta
    document.getElementById('roleAdmin').checked = false;
```

#### Naprawa 2 - Linia 175 (po funkcji `showRolesForm`):
```javascript
// PRZED (BŁĄD):
    document.getElementById('rolesFormSection').style.display = 'block';
}
t  // ← USUŃ TĘ LINIĘ!
function cancelUserForm() {

// PO (POPRAWNIE):
    document.getElementById('rolesFormSection').style.display = 'block';
}
// Linia 175 usunięta
function cancelUserForm() {
```

---

### KROK 2: Weryfikacja w przeglądarce

Po naprawie, otwórz https://timesheet.robgro.dev/users i:

1. **Sprawdź Console (F12)**:
   ```
   Console → Czy są błędy JavaScript?
   Przed naprawą: prawdopodobnie "Unexpected identifier 'es'" lub podobne
   Po naprawie: Brak błędów ✅
   ```

2. **Testuj przycisk "Roles"**:
   - Kliknij "Roles" przy dowolnym użytkowniku
   - Formularz powinien się pokazać
   - Checkboxy powinny być zaznaczone zgodnie z obecnymi rolami użytkownika

3. **Testuj zmianę ról**:
   - Zaznacz/odznacz role
   - Kliknij "Save Roles"
   - Sprawdź czy role się zmieniły w tabeli

---

### KROK 3: Deploy na produkcję

Po pomyślnej naprawie i testach lokalnych:

```bash
# 1. Dodaj i commituj naprawę
git add src/main/resources/static/js/users.js
git commit -m "fix: remove syntax errors in users.js (lines 159, 175)

Fixed JavaScript syntax errors that prevented Roles button from working:
- Removed stray 'es' on line 159
- Removed stray 't' on line 175

These artifacts were causing JavaScript parser errors and preventing
the roles management modal from opening."

# 2. Push do GitHub
git push

# 3. Deploy na produkcję (jeśli automatyczny deploy nie działa)
# ... wykonaj swój proces deploymentu ...
```

---

## Przewidywane rezultaty

Po naprawie:

✅ Przycisk "Roles" działa poprawnie
✅ Formularz zarządzania rolami otwiera się
✅ Obecne role użytkownika są poprawnie zaznaczone
✅ Zmiana ról działa i zapisuje się w bazie
✅ Brak błędów JavaScript w console

---

## Testy regresyjne

Po naprawie przetestuj również:
- [ ] Przycisk "Edit" - czy działa?
- [ ] Przycisk "Password" - czy działa?
- [ ] Przycisk "Reset Pass" - czy działa?
- [ ] Przycisk "Activate/Deactivate" - czy działa?

Wszystkie funkcje powinny działać, ponieważ błędy składni mogły wpływać na cały skrypt.

---

## Backup plan

Jeśli po naprawie nadal nie działa:

1. **Sprawdź network tab** (F12 → Network):
   - Czy request `PUT /api/v1/users/{id}/roles` jest wysyłany?
   - Jaki status HTTP zwraca?
   - Co jest w response?

2. **Sprawdź backend logs**:
   - Czy endpoint jest wywoływany?
   - Czy są błędy walidacji?
   - Czy RoleUpdateDto jest poprawnie deserializowany?

3. **Sprawdź czy user ma uprawnienia ADMIN**:
   - Endpoint wymaga `@PreAuthorize("hasRole('ADMIN')")`
   - Jeśli zalogowany user nie jest adminem → 403 Forbidden
