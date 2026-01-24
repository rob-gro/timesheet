# Email NOT NULL - Lista Zmian

## Problem

Każdy użytkownik MUSI mieć email, bo bez emaila nie można wysłać password reset link.

## Rozwiązanie

Dodać `nullable = false` do pola `email` w User entity + zaktualizować powiązane pliki.

---

## Lista plików do zmiany (6 plików)

### 1. ✅ User Entity - dodać NOT NULL constraint

**Plik:** `src/main/java/dev/robgro/timesheet/user/User.java`

**Linia:** 26

**Zmiana:**
```java
// PRZED:
private String email;

// PO:
@Column(nullable = false)
private String email;
```

**Uzasadnienie:** Database constraint - email MUST exist

---

### 2. ✅ UserDto - dodać @NotBlank validation

**Plik:** `src/main/java/dev/robgro/timesheet/user/UserDto.java`

**Linia:** 19

**Zmiana:**
```java
// PRZED:
@Email
String email,

// PO:
@NotBlank(message = "Email is required")
@Email(message = "Invalid email format")
String email,
```

**Uzasadnienie:** DTO validation - prevent null email from API

---

### 3. ✅ UserServiceImpl - walidacja przy tworzeniu

**Plik:** `src/main/java/dev/robgro/timesheet/user/UserServiceImpl.java`

**Metoda:** `createUser(UserDto userDto)` (linia ~75)

**Zmiana:**
```java
// Dodać walidację PRZED createUser:
public UserDto createUser(UserDto userDto) {
    // Validate email is not empty
    if (userDto.email() == null || userDto.email().isBlank()) {
        throw new IllegalArgumentException("Email is required");
    }

    // Check if email already exists
    if (userRepository.existsByEmail(userDto.email())) {
        throw new IllegalArgumentException("Email already exists");
    }

    // ... rest of method
}
```

**Uzasadnienie:** Business logic validation + unique email check

---

### 4. ✅ UserRepository - dodać existsByEmail

**Plik:** `src/main/java/dev/robgro/timesheet/user/UserRepository.java`

**Dodać metodę:**
```java
boolean existsByEmail(String email);
```

**Uzasadnienie:** Check email uniqueness

---

### 5. ✅ users.html - email required w formularzu

**Plik:** `src/main/resources/templates/users.html`

**Linia:** 61

**Zmiana:**
```html
<!-- PRZED: -->
<input type="email" id="email" class="form-control">

<!-- PO: -->
<input type="email" id="email" required class="form-control">
```

**Uzasadnienie:** Frontend validation - prevent empty submission

---

### 6. ✅ users.js - walidacja JavaScript

**Plik:** `src/main/resources/static/js/users.js`

**Gdzie:** W funkcji `saveUser()` lub `validateUserForm()`

**Dodać:**
```javascript
function validateUserForm() {
    const email = document.getElementById('email').value;

    if (!email || email.trim() === '') {
        alert('Email is required');
        return false;
    }

    // Email format validation (optional - HTML5 handles this)
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        alert('Invalid email format');
        return false;
    }

    return true;
}
```

**Uzasadnienie:** Client-side validation before API call

---

## Database Migration

**Plik:** Nowa migracja Flyway

**Nazwa:** `V17__make_users_email_not_null.sql`

**Lokalizacja:** `src/main/resources/db/migration/`

**Zawartość:**
```sql
-- Make email NOT NULL in users table
-- WARNING: This will fail if there are users without email!

-- Step 1: Check if any users have NULL email
-- Run this BEFORE migration to identify problematic users:
-- SELECT id, username, email FROM users WHERE email IS NULL OR email = '';

-- Step 2: If needed, update users without email (assign temporary email)
-- UPDATE users SET email = CONCAT(username, '@temp.example.com') WHERE email IS NULL OR email = '';

-- Step 3: Add NOT NULL constraint
ALTER TABLE users
MODIFY COLUMN email VARCHAR(255) NOT NULL;

-- Step 4: Add unique constraint (optional but recommended)
-- ALTER TABLE users
-- ADD CONSTRAINT uk_users_email UNIQUE (email);
```

---

## Checklist implementacji

- [ ] 1. Sprawdź czy są użytkownicy bez emaila: `SELECT * FROM users WHERE email IS NULL`
- [ ] 2. Jeśli są - zaktualizuj ich: `UPDATE users SET email = 'temp@example.com' WHERE email IS NULL`
- [ ] 3. Zmień User.java - dodaj `@Column(nullable = false)`
- [ ] 4. Zmień UserDto.java - dodaj `@NotBlank`
- [ ] 5. Zmień UserServiceImpl.java - dodaj walidację + unique check
- [ ] 6. Dodaj UserRepository.existsByEmail()
- [ ] 7. Zmień users.html - dodaj `required`
- [ ] 8. Zmień users.js - dodaj walidację
- [ ] 9. Utwórz migrację V17__make_users_email_not_null.sql
- [ ] 10. Uruchom aplikację - sprawdź czy Flyway wykonał migrację
- [ ] 11. Test: Spróbuj utworzyć użytkownika bez emaila (powinno się nie udać)

---

## Testy do wykonania

### Test 1: Nie można utworzyć użytkownika bez emaila

**Kroki:**
1. Przejdź do `/users`
2. Kliknij "Add New User"
3. Wypełnij tylko username i password (zostaw email pusty)
4. Kliknij "Save User"

**Expected:** Frontend validation: "Email is required"

---

### Test 2: Email musi być unikalny

**Kroki:**
1. Utwórz użytkownika z emailem `test@example.com`
2. Spróbuj utworzyć kolejnego użytkownika z tym samym emailem

**Expected:** Error: "Email already exists"

---

### Test 3: Email format validation

**Kroki:**
1. Spróbuj utworzyć użytkownika z emailem `invalid-email`

**Expected:** Error: "Invalid email format"

---

## Potencjalne problemy

### Problem: Migration fails - users with NULL email

**Rozwiązanie:**
```sql
-- BEFORE migration, update all NULL emails
UPDATE users
SET email = CONCAT(username, '@temporary.local')
WHERE email IS NULL OR email = '';
```

### Problem: Duplicate emails exist

**Rozwiązanie:**
```sql
-- Find duplicates
SELECT email, COUNT(*)
FROM users
WHERE email IS NOT NULL
GROUP BY email
HAVING COUNT(*) > 1;

-- Manually fix duplicates by updating emails
UPDATE users
SET email = CONCAT(email, '.', id)
WHERE email IN (SELECT email FROM (
    SELECT email FROM users GROUP BY email HAVING COUNT(*) > 1
) AS dups);
```

---

**Utworzono:** 2026-01-17
**Status:** TODO (waiting for implementation)
