package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.seller.Seller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence integration test for Invoice.schemeId.
 *
 * <p>Verifies that scheme_id is correctly persisted to the database
 * by going through the full Hibernate flush → L1 cache clear → DB reload cycle.
 * Unit tests cannot catch issues like missing @Column mapping or wrong column name.
 *
 * <p>Uses H2 in-memory database (test/resources/application.properties) with
 * Flyway disabled and Hibernate DDL auto-create. No Docker required.
 */
@DataJpaTest
class SchemeIdPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void shouldPersistSchemeIdToDatabase_afterFlushAndReload() {
        // Given - persist required FK entities
        Seller seller = new Seller();
        seller.setName("Test Seller");
        seller.setStreet("123 Main St");
        seller.setPostcode("SW1A 1AA");
        seller.setCity("London");
        seller.setServiceDescription("IT Services");
        entityManager.persist(seller);

        Client client = new Client();
        client.setClientName("Test Client");
        client.setHourlyRate(50.0);
        client.setHouseNo(1L);
        client.setStreetName("High Street");
        client.setCity("London");
        client.setPostCode("EC1A 1BB");
        client.setEmail("client@test.com");
        client.setActive(true);
        entityManager.persist(client);

        Long expectedSchemeId = 999L;

        // When - build and persist invoice with schemeId
        Invoice invoice = new Invoice();
        invoice.setSeller(seller);
        invoice.setClient(client);
        invoice.setIssueDate(LocalDate.of(2026, 2, 15));
        invoice.setInvoiceNumberComponents(1, 2026, 2, "001-02-2026", expectedSchemeId);

        entityManager.persist(invoice);
        entityManager.flush();
        entityManager.clear(); // Evict L1 cache — next find() goes to DB

        // Then - reload from DB and verify column mapping
        Invoice reloaded = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(reloaded.getSchemeId())
            .as("scheme_id must survive full persist → flush → clear → reload cycle")
            .isEqualTo(expectedSchemeId);
    }

    @Test
    void shouldPersistNullSchemeId_whenNotProvided() {
        // Verifies backward compatibility: existing invoices without schemeId (null) remain valid
        Seller seller = new Seller();
        seller.setName("Legacy Seller");
        seller.setStreet("1 Old St");
        seller.setPostcode("E1 1AA");
        seller.setCity("London");
        seller.setServiceDescription("Legacy Services");
        entityManager.persist(seller);

        Client client = new Client();
        client.setClientName("Legacy Client");
        client.setHourlyRate(40.0);
        client.setHouseNo(2L);
        client.setStreetName("Old Lane");
        client.setCity("Manchester");
        client.setPostCode("M1 1AA");
        client.setEmail("legacy@test.com");
        client.setActive(true);
        entityManager.persist(client);

        Invoice invoice = new Invoice();
        invoice.setSeller(seller);
        invoice.setClient(client);
        invoice.setIssueDate(LocalDate.of(2021, 1, 31));
        invoice.setInvoiceNumberComponents(1, 2021, 0, "INV-001-2021", null); // schemeId=null

        entityManager.persist(invoice);
        entityManager.flush();
        entityManager.clear();

        Invoice reloaded = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(reloaded.getSchemeId())
            .as("null schemeId must be preserved (backward compat for pre-audit invoices)")
            .isNull();
    }
}