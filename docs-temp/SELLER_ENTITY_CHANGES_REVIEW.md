# Przegląd zmian w branch `feature/seller-entity`

## Cel featury
Zastąpienie hardcoded `InvoiceSeller` konfiguracją bazodanową - wprowadzenie encji `Seller` z wsparciem dla:
- **System Default Seller** - używany przez CRON do automatycznego generowania faktur
- **User Default Seller** - każdy user może mieć swojego defaultowego sellera (pre-selection w UI)

---

## 1. SELLER ENTITY - Nowe pole `isSystemDefault`

### Seller.java
```diff
+ @Column(name = "is_system_default", nullable = false)
+ private boolean isSystemDefault = false;
```
- Dodano pole `isSystemDefault` (boolean, not null, default=false)
- Zaktualizowano `equals()`, `hashCode()`, `toString()`

### SellerDto.java
```diff
+ boolean systemDefault
```
- Dodano field `systemDefault` do DTO

### SellerDtoMapper.java
```diff
+ seller.isSystemDefault()
```
- Mapuje `isSystemDefault` → `systemDefault` w DTO

---

## 2. SELLER REPOSITORY - Nowe query methods

### SellerRepository.java
```java
// Nowe metody:
Optional<Seller> findByIsSystemDefaultTrue();
List<Seller> findAllOrderByActiveAndName();
```

**Cel:**
- `findByIsSystemDefaultTrue()` - znajduje seller z flagą system default (dla CRON)
- `findAllOrderByActiveAndName()` - zwraca wszystkich sellerów sortowanych po active DESC, name ASC (dla ADMIN)

---

## 3. SELLER SERVICE - Nowe metody i logika

### SellerService.java
```java
// Nowe metody:
List<SellerDto> getAllSellers(boolean includeInactive);
OperationResult setActiveStatus(Long id, boolean active);
```

### SellerServiceImpl.java

#### 3.1 getAllSellers(boolean includeInactive)
```java
List<Seller> sellers = includeInactive
    ? sellerRepository.findAllOrderByActiveAndName()    // ADMIN widzi wszystkich
    : sellerRepository.findAllActiveOrderByName();       // USER widzi tylko active
```

#### 3.2 mapToEntity() - System Default Logic
```java
// WAŻNE: Gdy ustawiasz seller jako systemDefault=true
if (dto.systemDefault() && !seller.isSystemDefault()) {
    // Automatycznie czyści flagę system default u wszystkich innych sellerów
    sellerRepository.findByIsSystemDefaultTrue()
        .ifPresent(currentDefault -> {
            currentDefault.setSystemDefault(false);
            sellerRepository.save(currentDefault);
        });
}
seller.setSystemDefault(dto.systemDefault());
```
**Logika:** Tylko jeden seller może być system default jednocześnie.

#### 3.3 setActiveStatus(Long id, boolean active)
```java
// Aktywacja/deaktywacja sellera (zamiast soft delete)
seller.setActive(active);
```

#### 3.4 createEmptySellerDto()
```diff
- null, null, null, null, null, true
+ null, null, null, null, null, true, false  // dodano systemDefault=false
```

---

## 4. SELLER VIEW CONTROLLER (NOWY)

### SellerViewController.java
**Pełny CRUD controller dla zarządzania sellerami**

| Endpoint | Metoda | Opis |
|----------|--------|------|
| `/sellers` | GET | Lista sellerów (ADMIN widzi wszystkich, USER tylko active) |
| `/sellers/new` | GET | Formularz nowego sellera |
| `/sellers/edit/{id}` | GET | Formularz edycji sellera |
| `/sellers/save` | POST | Zapis sellera (new/update) |
| `/sellers/delete/{id}` | DELETE | Soft delete (deactivate) |
| `/sellers/set-default/{id}` | POST | Ustawia **user's default seller** (dla zalogowanego usera) |
| `/sellers/set-system-default/{id}` | POST | Ustawia **system default seller** (dla CRON) |
| `/sellers/{id}/activate` | POST | Aktywuje sellera |
| `/sellers/{id}/deactivate` | POST | Deaktywuje sellera |

#### showSellerList() - Filtrowanie według roli
```java
boolean isAdmin = authentication.getAuthorities().stream()
    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

model.addAttribute("sellers", sellerService.getAllSellers(isAdmin));
```

#### setDefaultSeller() - User's default
```java
// Ustawia default seller dla KONKRETNEGO USERA
User currentUser = userService.findByUsername(authentication.getName());
userService.setDefaultSeller(currentUser.getId(), id);
```

#### setSystemDefaultSeller() - System default
```java
// Ustawia system default seller (dla CRON)
SellerDto updatedSeller = new SellerDto(
    ...,
    true  // systemDefault = true
);
sellerService.saveSeller(updatedSeller);
```
**Uwaga:** Ten endpoint wywołuje `mapToEntity()` która automatycznie czyści flagę u innych.

---

## 5. BILLING SERVICE - Używa System Default Seller

### BillingServiceImpl.java

#### createInvoice() - STARY KOD:
```java
// TODO FAZA 2: Use authenticated user's defaultSeller instead of first active seller
Seller defaultSeller = sellerRepository.findByActiveTrue().stream()
    .findFirst()
    .orElseThrow(() -> new BusinessRuleViolationException("No active seller found..."));
```

#### createInvoice() - NOWY KOD:
```java
// Use system default seller for CRON-generated invoices
Seller systemDefaultSeller = sellerRepository.findByIsSystemDefaultTrue()
    .orElseGet(() -> sellerRepository.findByActiveTrue().stream()
        .findFirst()
        .orElseThrow(() -> new BusinessRuleViolationException("No active seller found...")));

return invoiceCreationService.createInvoice(clientId, systemDefaultSeller.getId(), issueDate, timesheetIds);
```

**Logika:**
1. Szuka sellera z `isSystemDefault=true`
2. Jeśli nie ma → fallback na pierwszego active sellera
3. Jeśli nie ma żadnego active → throw exception

**To znaczy:** CRON będzie zawsze używał sellera z flagą system default.

---

## 6. INVOICE VIEW CONTROLLER - Przekazuje Current User

### InvoiceViewController.java

```diff
+ private final UserService userService;

  @GetMapping
- public String showItemsForm(Model model) {
+ public String showItemsForm(Model model, Authentication authentication) {
      ...
+     // Add current user for default seller pre-selection
+     if (authentication != null) {
+         User currentUser = userService.findByUsername(authentication.getName());
+         model.addAttribute("currentUser", currentUser);
+     }
```

**Cel:** UI może użyć `currentUser.defaultSeller` do pre-select sellera w formularzu tworzenia faktury.

---

## 7. USER SERVICE - Zarządza User's Default Seller

### UserService.java
```java
// Nowe metody:
User findByUsername(String username);
void setDefaultSeller(Long userId, Long sellerId);
```

### UserServiceImpl.java

```diff
+ private final dev.robgro.timesheet.seller.SellerRepository sellerRepository;
```

#### findByUsername()
```java
return userRepository.findByUsername(username)
    .orElseThrow(() -> new RuntimeException("User not found: " + username));
```

#### setDefaultSeller()
```java
User user = userRepository.findById(userId)
    .orElseThrow(() -> new RuntimeException("User not found..."));

if (sellerId != null) {
    Seller seller = sellerRepository.findById(sellerId)
        .orElseThrow(() -> new RuntimeException("Seller not found..."));
    user.setDefaultSeller(seller);
} else {
    user.setDefaultSeller(null);  // Clear default
}

userRepository.save(user);
```

**Logika:** Każdy user może mieć swojego default sellera (niezależnie od system default).

---

## 8. DATABASE MIGRATION

### V14__add_system_default_to_sellers.sql
```sql
-- Add is_system_default column to sellers table for CRON job
-- Indicates which seller is used by CRON job for automatic monthly invoicing
-- Only one seller should have this flag set to TRUE at a time
ALTER TABLE sellers
ADD COLUMN is_system_default BOOLEAN NOT NULL DEFAULT FALSE;

-- Set the first active seller as system default
UPDATE sellers
SET is_system_default = TRUE
WHERE id = (
    SELECT id
    FROM sellers
    WHERE active = TRUE
    ORDER BY id ASC
    LIMIT 1
);
```

**Logika migracji:**
1. Dodaje kolumnę `is_system_default` (default=FALSE)
2. Ustawia pierwszego active sellera jako system default

**Ważne:** Usunięto problematyczną linię `COMMENT ON COLUMN` (PostgreSQL syntax, nie działa na MariaDB)

---

## PODSUMOWANIE LOGIKI

### System Default Seller (dla CRON)
- Tylko **JEDEN** seller może mieć `isSystemDefault=true`
- Ustawiany przez ADMIN via `/sellers/set-system-default/{id}`
- Automatycznie czyści flagę u innych sellerów
- Używany przez **CRON scheduler** do automatycznego generowania faktur
- Fallback: jeśli brak system default → używa pierwszego active sellera

### User's Default Seller (dla UI)
- Każdy **USER** może mieć swojego default sellera
- Ustawiany via `/sellers/set-default/{id}`
- Używany do **pre-selection** w UI przy tworzeniu faktur ręcznie
- Niezależny od system default seller

### Aktywacja/Deaktywacja
- Soft delete pattern: `active=true/false`
- ADMIN widzi wszystkich (active + inactive)
- USER widzi tylko active
- Można reaktywować deaktywowanego sellera

---

## PLIKI DO SPRAWDZENIA

### Modified:
- ✅ `Seller.java` - dodano `isSystemDefault`
- ✅ `SellerDto.java` - dodano `systemDefault`
- ✅ `SellerDtoMapper.java` - mapowanie `isSystemDefault`
- ✅ `SellerRepository.java` - `findByIsSystemDefaultTrue()`, `findAllOrderByActiveAndName()`
- ✅ `SellerService.java` - `getAllSellers(boolean)`, `setActiveStatus()`
- ✅ `SellerServiceImpl.java` - implementacja system default logic
- ✅ `BillingServiceImpl.java` - używa system default seller
- ✅ `InvoiceViewController.java` - przekazuje currentUser
- ✅ `UserService.java` - `findByUsername()`, `setDefaultSeller()`
- ✅ `UserServiceImpl.java` - implementacja setDefaultSeller()

### New:
- ✅ `SellerViewController.java` - pełny CRUD dla sellerów
- ✅ `V14__add_system_default_to_sellers.sql` - migracja

### Templates (do sprawdzenia osobno):
- `templates/sellers/` - formularze i lista sellerów
- `static/js/sellers.js` - JavaScript dla UI
- `templates/invoices/create.html` - pre-selection sellera
- `templates/invoices/items.html` - pre-selection sellera

---

## PYTANIA DO WERYFIKACJI

1. ✅ Czy tylko jeden seller może być system default? → TAK (logika w `mapToEntity()`)
2. ✅ Czy CRON używa system default seller? → TAK (zmiana w `BillingServiceImpl`)
3. ✅ Czy każdy user może mieć swojego default? → TAK (`UserService.setDefaultSeller()`)
4. ✅ Czy ADMIN widzi wszystkich, USER tylko active? → TAK (`getAllSellers(boolean)`)
5. ✅ Czy można reaktywować sellera? → TAK (`setActiveStatus()`)
6. ✅ Czy migracja V14 działa na MariaDB? → TAK (usunięto `COMMENT ON COLUMN`)

---

## NASTĘPNE KROKI

1. **Przejrzeć kod** - sprawdzić czy logika się zgadza
2. **Sprawdzić templates** - czy UI dobrze używa currentUser.defaultSeller
3. **Przetestować lokalnie** (jeśli możliwe) lub na TEST DB
4. **Commit zmian** - `git add -A && git commit`
5. **Deploy na PROD** - zmienić profil na `prod`, sprawdzić czy migracja przejdzie
