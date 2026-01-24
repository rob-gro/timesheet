# Lista klas zaangażowanych w feature `seller-entity`

## 🔵 MODIFIED (Zmodyfikowane)

### Seller Package
```
src/main/java/dev/robgro/timesheet/seller/
├── Seller.java                    [ENTITY] Dodano isSystemDefault
├── SellerDto.java                 [DTO] Dodano systemDefault
├── SellerDtoMapper.java           [MAPPER] Mapowanie isSystemDefault
├── SellerRepository.java          [REPO] Nowe query methods
├── SellerService.java             [SERVICE] Nowe metody interfejsu
└── SellerServiceImpl.java         [SERVICE] Logika system default
```

### Invoice Package
```
src/main/java/dev/robgro/timesheet/invoice/
├── BillingServiceImpl.java        [SERVICE] Używa system default seller
└── InvoiceViewController.java     [CONTROLLER] Przekazuje currentUser
```

### User Package
```
src/main/java/dev/robgro/timesheet/user/
├── UserService.java               [SERVICE] Nowe metody interfejsu
└── UserServiceImpl.java           [SERVICE] Zarządza user's default seller
```

---

## 🟢 NEW (Nowe)

### Seller Package
```
src/main/java/dev/robgro/timesheet/seller/
└── SellerViewController.java      [CONTROLLER] Pełny CRUD dla sellerów
```

### Database Migrations
```
src/main/resources/db/migration/seller-entity/
└── V14__add_system_default_to_sellers.sql
```

---

## 📊 PODSUMOWANIE

| Kategoria | Modified | New | Total |
|-----------|----------|-----|-------|
| **Seller** | 6 | 1 | 7 |
| **Invoice** | 2 | 0 | 2 |
| **User** | 2 | 0 | 2 |
| **Migrations** | 0 | 1 | 1 |
| **TOTAL** | 10 | 2 | **12** |

---

## 🗂️ SZCZEGÓŁOWA LISTA (z ścieżkami)

### 1. SELLER - Entity i DTO
- `src/main/java/dev/robgro/timesheet/seller/Seller.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerDto.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerDtoMapper.java`

### 2. SELLER - Repository
- `src/main/java/dev/robgro/timesheet/seller/SellerRepository.java`

### 3. SELLER - Service Layer
- `src/main/java/dev/robgro/timesheet/seller/SellerService.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerServiceImpl.java`

### 4. SELLER - Controller
- `src/main/java/dev/robgro/timesheet/seller/SellerViewController.java` ✨ NEW

### 5. INVOICE - Service i Controller
- `src/main/java/dev/robgro/timesheet/invoice/BillingServiceImpl.java`
- `src/main/java/dev/robgro/timesheet/invoice/InvoiceViewController.java`

### 6. USER - Service Layer
- `src/main/java/dev/robgro/timesheet/user/UserService.java`
- `src/main/java/dev/robgro/timesheet/user/UserServiceImpl.java`

### 7. DATABASE - Migrations
- `src/main/resources/db/migration/seller-entity/V14__add_system_default_to_sellers.sql` ✨ NEW

---

## 🔗 ZALEŻNOŚCI MIĘDZY KLASAMI

```
SellerViewController
    ↓ uses
SellerService ← UserService
    ↓ uses
SellerRepository

BillingServiceImpl
    ↓ uses
SellerRepository (findByIsSystemDefaultTrue)

InvoiceViewController
    ↓ uses
UserService (findByUsername)
    ↓ uses
SellerRepository (setDefaultSeller)

UserServiceImpl
    ↓ uses
SellerRepository (findById)
```

---

## 📝 CZEGO DOTYCZĄ ZMIANY

### Seller.java
- Dodano pole `isSystemDefault` (boolean, not null, default=false)
- Zaktualizowano `equals()`, `hashCode()`, `toString()`

### SellerDto.java
- Dodano `boolean systemDefault`

### SellerDtoMapper.java
- Mapuje `seller.isSystemDefault()` → `dto.systemDefault`

### SellerRepository.java
- `findByIsSystemDefaultTrue()` - znajduje system default seller
- `findAllOrderByActiveAndName()` - wszystkie sellery (active + inactive)

### SellerService.java
- `getAllSellers(boolean includeInactive)` - filtrowanie active/inactive
- `setActiveStatus(Long id, boolean active)` - aktywacja/deaktywacja

### SellerServiceImpl.java
- Logika: tylko jeden seller może być system default
- Automatyczne czyszczenie flagi u innych sellerów
- Implementacja `setActiveStatus()`

### SellerViewController.java ✨
- Pełny CRUD: list, new, edit, save, delete
- `/sellers/set-default/{id}` - user's default
- `/sellers/set-system-default/{id}` - system default (CRON)
- `/sellers/{id}/activate` i `/sellers/{id}/deactivate`

### BillingServiceImpl.java
- Zmiana z `findByActiveTrue().findFirst()` na `findByIsSystemDefaultTrue()`
- Fallback: jeśli brak system default → pierwszy active

### InvoiceViewController.java
- Dodano `UserService` dependency
- Przekazuje `currentUser` do modelu (pre-selection w UI)

### UserService.java
- `findByUsername(String username)`
- `setDefaultSeller(Long userId, Long sellerId)`

### UserServiceImpl.java
- Implementacja `setDefaultSeller()` - zarządza user's default seller
- Dodano `SellerRepository` dependency

### V14__add_system_default_to_sellers.sql ✨
- `ALTER TABLE sellers ADD COLUMN is_system_default`
- `UPDATE` ustawia pierwszego active sellera jako system default

---

## ⚠️ SUPPORTING CLASSES (Nie zmieniane, ale używane)

### Bezpośrednio używane:
- `OperationResult` - zwracany przez `setActiveStatus()`, `deactivateSeller()`
- `User` - entity używana przez `UserService`
- `Authentication` - Spring Security, używana w controllerach

### Pośrednio związane:
- `InvoiceCreationService` - wywoływana przez `BillingServiceImpl`
- `RoleService` - używana w `UserServiceImpl` (istniejąca funkcjonalność)

---

## 🎯 QUICK REFERENCE - Co gdzie sprawdzić?

| Co chcesz sprawdzić | Gdzie szukać |
|---------------------|--------------|
| Jak działa system default logic? | `SellerServiceImpl.mapToEntity()` |
| Jak CRON wybiera sellera? | `BillingServiceImpl.createInvoice()` |
| Jak user ustawia swojego default? | `SellerViewController.setDefaultSeller()` |
| Jak ADMIN ustawia system default? | `SellerViewController.setSystemDefaultSeller()` |
| Jakie query methods są w repo? | `SellerRepository.java` |
| Jak aktywować/deaktywować? | `SellerServiceImpl.setActiveStatus()` |
| Co robi migracja V14? | `V14__add_system_default_to_sellers.sql` |

---

## ✅ STATUS PLIKÓW (Git)

```bash
M  src/main/java/dev/robgro/timesheet/invoice/BillingServiceImpl.java
M  src/main/java/dev/robgro/timesheet/invoice/InvoiceViewController.java
M  src/main/java/dev/robgro/timesheet/seller/Seller.java
M  src/main/java/dev/robgro/timesheet/seller/SellerDto.java
M  src/main/java/dev/robgro/timesheet/seller/SellerDtoMapper.java
M  src/main/java/dev/robgro/timesheet/seller/SellerRepository.java
M  src/main/java/dev/robgro/timesheet/seller/SellerService.java
M  src/main/java/dev/robgro/timesheet/seller/SellerServiceImpl.java
M  src/main/java/dev/robgro/timesheet/user/UserService.java
M  src/main/java/dev/robgro/timesheet/user/UserServiceImpl.java
??  src/main/java/dev/robgro/timesheet/seller/SellerViewController.java
??  src/main/resources/db/migration/seller-entity/V14__add_system_default_to_sellers.sql
```

**M** = Modified (zmodyfikowany)
**??** = Untracked (nowy, nie w git)

---

## 🚀 DO ZROBIENIA

1. ✅ Przejrzeć wszystkie klasy z listy
2. ⏳ Sprawdzić czy logika jest poprawna
3. ⏳ Przetestować funkcjonalność
4. ⏳ Dodać wszystkie pliki do git: `git add -A`
5. ⏳ Zrobić commit: `git commit -m "..."`
6. ⏳ Deploy na PROD (zmienić profil na `prod`)
