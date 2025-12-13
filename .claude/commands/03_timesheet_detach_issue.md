# Fix: Invoice Deletion - Bidirectional Relationship Issue

**Priorytet:** HIGH
**Podejście:** SENIOR - Helper methods w encjach + proper JPA

---

## 🎯 Kolejność Implementacji (POPRAWIONA)

### ✅ FAZA 2 FIRST: Bidirectional Relationship + Helper Methods (45 min)
**Dlaczego pierwszy:** Fundament - proper encapsulation, DRY principle

### ✅ FAZA 3 SECOND: Remove JDBC, use JPA (30 min)
**Dlaczego drugi:** Proper JPA usage, consistency

### ⚠️ FAZA 1 LAST (conditional): flush() only if needed
**Dlaczego ostatni:** Może nie być potrzebne po poprawkach 2+3

---

## FAZA 2: Helper Methods + Bidirectional Relationship

### Krok 1: Dodaj helper methods do Invoice entity

**Plik:** `Invoice.java`

```java
// DODAJ te metody do Invoice entity:

/**
 * Adds a timesheet to this invoice and sets bidirectional relationship.
 * Use this instead of direct collection manipulation.
 */
public void addTimesheet(Timesheet timesheet) {
    timesheets.add(timesheet);
    timesheet.setInvoice(this);
    timesheet.setInvoiced(true);
    timesheet.setInvoiceNumber(this.invoiceNumber);
}

/**
 * Removes a timesheet from this invoice and clears bidirectional relationship.
 * Use this instead of direct collection manipulation.
 */
public void removeTimesheet(Timesheet timesheet) {
    timesheets.remove(timesheet);
    timesheet.setInvoice(null);
    timesheet.setInvoiced(false);
    timesheet.setInvoiceNumber(null);
}

/**
 * Detaches all timesheets from this invoice.
 * Used when deleting invoice but preserving timesheets.
 */
public void detachAllTimesheets() {
    // Defensive copy to avoid ConcurrentModificationException
    new ArrayList<>(timesheets).forEach(this::removeTimesheet);
}
```

**Zalety:**
- ✅ Encapsulation - logika relacji w jednym miejscu
- ✅ DRY - nie powtarzamy kodu w serwisach
- ✅ Atomowość - niemożliwe zapomnieć o jednej stronie relacji
- ✅ Testowalne - łatwo przetestować na poziomie entity

---

### Krok 2: Użyj helper methods w InvoiceServiceImpl

**Plik:** `InvoiceServiceImpl.java`

**PRZED (linie 327-340):**
```java
List<Timesheet> timesheetsToProcess = new ArrayList<>(invoice.getTimesheets());
for (Timesheet timesheet : timesheetsToProcess) {
    log.info("Processing timesheet ID: {}", timesheet.getId());
    timesheet.setInvoice(null);
    timesheet.setInvoiced(false);
    timesheet.setInvoiceNumber(null);

    if (deleteTimesheets) {
        log.info("Deleting timesheet ID: {}", timesheet.getId());
        timesheetRepository.delete(timesheet);
    } else {
        log.info("Preserving timesheet ID: {}", timesheet.getId());
        timesheetRepository.save(timesheet);
    }
}
```

**PO (ZMIEŃ NA):**
```java
if (deleteTimesheets) {
    // Delete all timesheets associated with invoice
    log.info("Deleting {} timesheets", invoice.getTimesheets().size());
    List<Timesheet> timesheetsToDelete = new ArrayList<>(invoice.getTimesheets());
    timesheetsToDelete.forEach(ts -> {
        invoice.removeTimesheet(ts);  // ✅ Helper method - proper cleanup
        timesheetRepository.delete(ts);
    });
} else {
    // Detach timesheets but keep them in DB
    log.info("Detaching {} timesheets", invoice.getTimesheets().size());
    invoice.detachAllTimesheets();  // ✅ Helper method - clean & simple!
    // timesheetRepository.saveAll() handled by @Transactional flush
}
```

**Rezultat:**
- Kod czystszy, bardziej readable
- Logika relacji w Invoice, nie w Service
- Niemożliwe zapomnieć o invoice_number czy invoiced flag

---

### Krok 3: Unit Test dla helper methods

**Plik:** `InvoiceTest.java` (nowy plik)

```java
package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.timesheet.Timesheet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InvoiceTest {

    @Test
    void addTimesheet_shouldSetBidirectionalRelationship() {
        // given
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-001");
        Timesheet timesheet = new Timesheet();

        // when
        invoice.addTimesheet(timesheet);

        // then
        assertThat(invoice.getTimesheets()).contains(timesheet);
        assertThat(timesheet.getInvoice()).isEqualTo(invoice);
        assertThat(timesheet.isInvoiced()).isTrue();
        assertThat(timesheet.getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void removeTimesheet_shouldClearBidirectionalRelationship() {
        // given
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-001");
        Timesheet timesheet = new Timesheet();
        invoice.addTimesheet(timesheet);

        // when
        invoice.removeTimesheet(timesheet);

        // then
        assertThat(invoice.getTimesheets()).doesNotContain(timesheet);
        assertThat(timesheet.getInvoice()).isNull();
        assertThat(timesheet.isInvoiced()).isFalse();
        assertThat(timesheet.getInvoiceNumber()).isNull();
    }

    @Test
    void detachAllTimesheets_shouldClearAllRelationships() {
        // given
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-001");
        Timesheet ts1 = new Timesheet();
        Timesheet ts2 = new Timesheet();
        invoice.addTimesheet(ts1);
        invoice.addTimesheet(ts2);

        // when
        invoice.detachAllTimesheets();

        // then
        assertThat(invoice.getTimesheets()).isEmpty();
        assertThat(ts1.getInvoice()).isNull();
        assertThat(ts2.getInvoice()).isNull();
        assertThat(ts1.isInvoiced()).isFalse();
        assertThat(ts2.isInvoiced()).isFalse();
    }
}
```

---

## FAZA 3: Replace JDBC with JPA

### Option A: Repository Bulk Delete (RECOMMENDED)

**Plik:** `InvoiceItemRepository.java`

```java
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    @Modifying
    @Query("DELETE FROM InvoiceItem i WHERE i.invoice.id = :invoiceId")
    void deleteByInvoiceId(@Param("invoiceId") Long invoiceId);
}
```

**Plik:** `InvoiceServiceImpl.java` (linia 344)

```java
// ❌ PRZED:
jdbcTemplate.update("DELETE FROM invoice_items WHERE invoice_id = ?", id);

// ✅ PO:
log.info("Deleting invoice items using JPA");
invoiceItemRepository.deleteByInvoiceId(id);
```

**Zalety:**
- ✅ Pure JPA - no JDBC bypass
- ✅ JPA cache consistency
- ✅ Better testability (mock repository)
- ✅ Explicit control over deletion

---

### Option B: Cascade + Orphan Removal (ALTERNATIVE)

**Plik:** `Invoice.java`

```java
@OneToMany(mappedBy = "invoice",
           cascade = CascadeType.ALL,
           orphanRemoval = true)
private List<InvoiceItem> items = new ArrayList<>();
```

**Plik:** `InvoiceServiceImpl.java`

```java
// Usuń całą linię 344 - cascade automatically deletes items
// invoiceRepository.delete(invoice) wystarczy
```

**Zalety:**
- ✅ Less code - JPA handles it
- ✅ Declarative - clear intent in entity

**Wady:**
- ⚠️ Less explicit - magic happens
- ⚠️ Potential performance issues with large collections

**Rekomendacja:** **Option A** - explicit control, better for this use case

---

## FAZA 1: flush() - ONLY IF NEEDED

### Kiedy dodać flush()?

**Test reprodukowalności:**
1. Usuń fakturę z "Keep timesheets"
2. Odśwież UI natychmiast (< 1 sekunda)
3. Czy timesheety widoczne?

**Jeśli TAK** → flush() NIE jest potrzebne ✅
**Jeśli NIE** → dodaj flush() tylko w detach branch:

```java
} else {
    log.info("Detaching {} timesheets", invoice.getTimesheets().size());
    invoice.detachAllTimesheets();

    // ✅ DODAJ tylko jeśli problem persists:
    log.debug("Flushing detached timesheets to database");
    timesheetRepository.flush();
}
```

**Dlaczego prawdopodobnie NIE będzie potrzebne:**
- @Transactional automatycznie flushuje przed commit
- Proper bidirectional management = JPA wie o zmianach
- UI zwykle czeka na response (czyli po commit)

---

## 📊 Final Code Comparison

### PRZED (rozproszona logika):
```java
// InvoiceServiceImpl - 15 linii, mixed concerns
List<Timesheet> timesheetsToProcess = new ArrayList<>(invoice.getTimesheets());
for (Timesheet timesheet : timesheetsToProcess) {
    timesheet.setInvoice(null);
    timesheet.setInvoiced(false);
    timesheet.setInvoiceNumber(null);
    if (deleteTimesheets) {
        timesheetRepository.delete(timesheet);
    } else {
        timesheetRepository.save(timesheet);
    }
}
jdbcTemplate.update("DELETE FROM invoice_items WHERE invoice_id = ?", id);
```

### PO (clean, encapsulated):
```java
// Invoice - helper methods
public void removeTimesheet(Timesheet ts) { ... }
public void detachAllTimesheets() { ... }

// InvoiceServiceImpl - 6 linii, clear intent
if (deleteTimesheets) {
    invoice.getTimesheets().forEach(ts -> {
        invoice.removeTimesheet(ts);
        timesheetRepository.delete(ts);
    });
} else {
    invoice.detachAllTimesheets();
}
invoiceItemRepository.deleteByInvoiceId(id);
```

**Redukcja kodu:** 15 linii → 6 linii
**Encapsulation:** ✅ Logika relacji w Invoice
**Maintainability:** ✅ Łatwo zrozumieć i zmienić
**Testability:** ✅ Unit testy na poziomie entity

---

## ✅ Checklist Implementacji

### FAZA 2:
- [ ] Dodaj helper methods do `Invoice.java`
- [ ] Zmień `InvoiceServiceImpl.deleteInvoice()` - użyj helpers
- [ ] Napisz unit testy `InvoiceTest.java`
- [ ] Manual test: usuń fakturę, sprawdź DB
- [ ] Commit: `refactor: add helper methods for bidirectional relationship in Invoice`

### FAZA 3:
- [ ] Dodaj `deleteByInvoiceId()` do `InvoiceItemRepository`
- [ ] Zamień JDBC na JPA w `InvoiceServiceImpl`
- [ ] Update unit tests
- [ ] Manual test: verify invoice items deleted
- [ ] Commit: `refactor: replace JDBC with JPA for invoice items deletion`

### FAZA 1 (conditional):
- [ ] Test reprodukowalności po FAZA 2+3
- [ ] Jeśli problem persists: dodaj flush()
- [ ] Jeśli OK: skip this phase ✅

---

## 🎯 Expected Results

1. **Code Quality:**
   - Helper methods w Invoice = proper OOP
   - No JDBC bypass = pure JPA
   - DRY principle followed

2. **Functionality:**
   - Bidirectional relationship properly managed
   - Timesheety detached correctly
   - Invoice items deleted via JPA

3. **User Experience:**
   - Timesheety widoczne natychmiast (lub po flush jeśli potrzebne)
   - No UI bugs

---

**Estimated Time:**
- FAZA 2: 45 min (dev) + 15 min (testing)
- FAZA 3: 30 min (dev) + 15 min (testing)
- FAZA 1: 10 min (tylko jeśli potrzebne)
- **TOTAL:** ~2h

**Complexity:** MEDIUM
**Quality:** SENIOR LEVEL 🎯