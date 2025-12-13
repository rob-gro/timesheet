# Plan Naprawy: deleteInvoice() - MID/SENIOR Level

**Data:** 2025-12-12
**Priorytet:** HIGH
**Cel:** Naprawić błędy w metodzie deleteInvoice() zgodnie z best practices

---

## 🔍 Zidentyfikowane Problemy

### 🔴 PROBLEM #1: Bidirectional Relationship - Inconsistent State
**Lokalizacja:** `InvoiceServiceImpl.java:330-332`
**Opis:** Kod czyści referencję w Timesheet, ale nie usuwa Timesheet z kolekcji Invoice
**Skutek:** Inconsistent state w JPA entity manager

**Obecny kod:**
```java
timesheet.setInvoice(null);
timesheet.setInvoiced(false);
timesheet.setInvoiceNumber(null);
// ❌ Brak: invoice.getTimesheets().remove(timesheet)
```

**Impact:**
- Entity manager ma niespójne dane
- Potencjalne lazy loading issues
- Trudne do debugowania błędy

---

### 🔴 PROBLEM #2: Brak flush() - Race Condition z UI
**Lokalizacja:** `InvoiceServiceImpl.java:339`
**Opis:** Zapisane timesheety nie są od razu commitowane do DB
**Skutek:** UI odświeża PRZED commit transakcji → timesheety niewidoczne

**Obecny kod:**
```java
} else {
    log.info("Preserving timesheet ID: {}", timesheet.getId());
    timesheetRepository.save(timesheet);
}
// ❌ Brak flush() - zmiany nie są od razu w DB!
```

**Impact:**
- User widzi stare dane po refresh
- Trzeba odświeżać wielokrotnie
- Wrażenie "buggy app"

---

### 🟡 PROBLEM #3: Raw JDBC w @Transactional - JPA Bypass
**Lokalizacja:** `InvoiceServiceImpl.java:344`
**Opis:** Używa jdbcTemplate zamiast JPA repository
**Skutek:** Bypass JPA cache, cascade rules ignored

**Obecny kod:**
```java
jdbcTemplate.update("DELETE FROM invoice_items WHERE invoice_id = ?", id);
```

**Impact:**
- JPA cache inconsistency
- Orphan removal nie działa
- Entity listeners nie są wywoływane
- Trudniejszy testing (trzeba mockować JDBC)

---

## 📋 Plan Implementacji

### FAZA 1: Quick Win - Dodaj flush() (15 min)
**Priorytet:** 🔴 CRITICAL
**Cel:** Natychmiastowa poprawa user experience

#### Kroki:
1. ✅ Dodaj `timesheetRepository.flush()` po pętli (linia ~341)
2. ✅ Test manualny: usuń fakturę, odśwież UI → timesheety widoczne od razu
3. ✅ Commit: `fix: add flush after detaching timesheets`

#### Kod:
```java
// Linia 341 - DODAJ po pętli:
}

// ✅ DODAJ TEN KOD:
if (!deleteTimesheets) {
    log.debug("Flushing detached timesheets to database");
    timesheetRepository.flush();
}

log.info("Deleting invoice items");
```

**Expected Result:**
- Timesheety widoczne natychmiast po refresh
- Brak race condition z UI
- User experience: ✅ działa od pierwszego refresh

---

### FAZA 2: Fix Bidirectional Relationship (30 min)
**Priorytet:** 🔴 HIGH
**Cel:** Proper entity state management

#### Kroki:
1. ✅ Usuń timesheet z kolekcji Invoice PRZED czyszczeniem referencji
2. ✅ Dodaj unit test weryfikujący consistent state
3. ✅ Commit: `fix: properly manage bidirectional relationship in deleteInvoice`

#### Kod:
```java
// Linia 328-332 - ZMIEŃ:
List<Timesheet> timesheetsToProcess = new ArrayList<>(invoice.getTimesheets());
for (Timesheet timesheet : timesheetsToProcess) {
    log.info("Processing timesheet ID: {}", timesheet.getId());

    // ✅ DODAJ - usuń z kolekcji Invoice (FIRST!)
    invoice.getTimesheets().remove(timesheet);

    // Następnie czyść referencje w Timesheet
    timesheet.setInvoice(null);
    timesheet.setInvoiced(false);
    timesheet.setInvoiceNumber(null);
```

#### Unit Test:
```java
@Test
void deleteInvoice_shouldProperlyDetachTimesheets() {
    // given
    Invoice invoice = createInvoiceWithTimesheets(3);
    Long invoiceId = invoice.getId();

    // when
    invoiceService.deleteInvoice(invoiceId, false, false);

    // then
    List<Timesheet> detachedTimesheets = timesheetRepository
        .findByInvoiceNumberIsNull();

    assertThat(detachedTimesheets).hasSize(3);
    detachedTimesheets.forEach(ts -> {
        assertThat(ts.getInvoice()).isNull();
        assertThat(ts.getInvoiceNumber()).isNull();
        assertThat(ts.isInvoiced()).isFalse();
    });
}
```

**Expected Result:**
- Bidirectional relationship properly managed
- No orphaned references in entity manager
- Test coverage: ✅ green

---

### FAZA 3: Replace JDBC with JPA (45 min)
**Priorytet:** 🟡 MEDIUM
**Cel:** Consistent JPA usage, better testing

#### Option A: Custom Repository Method (RECOMMENDED)
```java
// InvoiceItemRepository.java - DODAJ:
@Modifying
@Query("DELETE FROM InvoiceItem i WHERE i.invoice.id = :invoiceId")
void deleteByInvoiceId(@Param("invoiceId") Long invoiceId);
```

```java
// InvoiceServiceImpl.java - linia 344 ZMIEŃ:
// ❌ jdbcTemplate.update("DELETE FROM invoice_items WHERE invoice_id = ?", id);
// ✅ invoiceItemRepository.deleteByInvoiceId(id);
log.info("Deleting invoice items");
invoiceItemRepository.deleteByInvoiceId(id);
invoiceItemRepository.flush(); // Ensure immediate deletion
```

#### Option B: Cascade Delete (ALTERNATIVE)
```java
// Invoice.java - ZMIEŃ @OneToMany:
@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
private List<InvoiceItem> items = new ArrayList<>();

// InvoiceServiceImpl.java - USUŃ linię 344:
// Cascade automatically deletes items when invoice deleted
```

#### Kroki:
1. ✅ Implement Option A (safer, explicit)
2. ✅ Add InvoiceItemRepository method
3. ✅ Replace jdbcTemplate call
4. ✅ Update unit tests to verify deletion
5. ✅ Commit: `refactor: use JPA repository instead of JDBC for invoice items deletion`

**Expected Result:**
- No JDBC bypass in @Transactional
- JPA cache consistency
- Better testability (mock repository, not JDBC)

---

## 🧪 Testing Strategy

### Manual Testing Checklist
- [ ] Delete invoice with "Keep timesheets" → refresh UI → timesheety widoczne od razu
- [ ] Delete invoice with "Delete timesheets" → timesheety usunięte z DB
- [ ] Verify invoice_number cleared in timesheets table
- [ ] Verify invoice_items deleted
- [ ] Check logs for proper execution order

### Unit Tests to Add
```java
// InvoiceServiceImplTest.java

@Test
void deleteInvoice_withDetachTimesheets_shouldFlushImmediately() {
    // Verify flush() is called
}

@Test
void deleteInvoice_shouldRemoveTimesheetsFromInvoiceCollection() {
    // Verify bidirectional relationship
}

@Test
void deleteInvoice_shouldDeleteInvoiceItemsUsingRepository() {
    // Verify JPA method called, not JDBC
}

@Test
void deleteInvoice_withDeleteTimesheets_shouldRemoveAllTimesheets() {
    // Existing test - verify still works
}
```

---

## 📊 Risk Assessment

| Issue | Risk | Mitigation |
|-------|------|------------|
| flush() breaks something | LOW | @Transactional handles it, flush() just forces timing |
| Bidirectional fix causes NPE | MEDIUM | Defensive copy already exists, add null checks |
| JPA delete slower than JDBC | LOW | Batch size config, acceptable for invoice deletion |
| Breaking existing tests | MEDIUM | Run full test suite before commit |

---

## 🚀 Deployment Strategy

### Development
1. Apply FAZA 1 (flush) → test → commit
2. Apply FAZA 2 (bidirectional) → test → commit
3. Apply FAZA 3 (JPA) → test → commit

### Staging (Heroku Test)
1. Deploy after each fase
2. Manual QA testing
3. Monitor logs for errors

### Production
1. Deploy all 3 fixes together
2. Backup database before deploy
3. Monitor first invoice deletions
4. Rollback plan: revert last 3 commits

---

## 📝 Commit Messages

```bash
# FAZA 1
git commit -m "fix: add flush after detaching timesheets from deleted invoice

- Prevents race condition with UI refresh
- Ensures timesheets are immediately visible in database
- Fixes issue where detached timesheets don't appear until multiple refreshes"

# FAZA 2
git commit -m "fix: properly manage bidirectional relationship in deleteInvoice

- Remove timesheets from invoice collection before nullifying reference
- Ensures consistent entity state in JPA context
- Prevents orphaned references in entity manager"

# FAZA 3
git commit -m "refactor: use JPA repository instead of JDBC for invoice items deletion

- Replace jdbcTemplate with InvoiceItemRepository method
- Ensures JPA cache consistency
- Better testability and follows Spring Data best practices"
```

---

## ✅ Definition of Done

- [ ] All 3 problems fixed
- [ ] Unit tests added and passing
- [ ] Manual testing completed
- [ ] Code reviewed (self or peer)
- [ ] Committed with meaningful messages
- [ ] Deployed to production
- [ ] Monitored for 24h post-deploy
- [ ] Documentation updated (if needed)

---

## 🎯 Success Criteria

1. **User Experience:**
   - Timesheety widoczne natychmiast po usunięciu faktury (1 refresh, nie 5+)

2. **Code Quality:**
   - No JDBC in @Transactional methods
   - Bidirectional relationships properly managed
   - Test coverage > 80% for deleteInvoice()

3. **Production Stability:**
   - No errors in logs after deployment
   - No user complaints about invoice deletion

---

**Estimated Time:** 1.5h (dev) + 0.5h (testing) = 2h total
**Complexity:** MEDIUM
**Business Value:** HIGH (better UX, fewer support tickets)