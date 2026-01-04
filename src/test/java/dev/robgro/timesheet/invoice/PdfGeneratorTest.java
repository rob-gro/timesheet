package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.exception.ServiceOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfGeneratorTest {

    private PdfGenerator pdfGenerator;
    private Invoice testInvoice;
    private InvoiceSeller testSeller;
    private Client testClient;

    @BeforeEach
    void setUp() {
        pdfGenerator = new PdfGenerator();

        // Create test seller
        testSeller = new InvoiceSeller();
        testSeller.setName("Test Company Ltd");
        testSeller.setStreet("123 Test Street");
        testSeller.setPostcode("TE1 2ST");
        testSeller.setCity("Testville");

        // Create test client
        testClient = new Client();
        testClient.setId(1L);
        testClient.setClientName("Test Client Ltd");
        testClient.setHouseNo(456L);
        testClient.setStreetName("Client Avenue");
        testClient.setCity("Clientown");
        testClient.setPostCode("CL3 4NT");
        testClient.setEmail("client@test.com");
        testClient.setHourlyRate(50.0);

        // Create seller entity
        dev.robgro.timesheet.seller.Seller seller = new dev.robgro.timesheet.seller.Seller();
        seller.setId(1L);
        seller.setName("Test Company Ltd");
        seller.setStreet("123 Test Street");
        seller.setPostcode("TE1 2ST");
        seller.setCity("Testville");
        seller.setActive(true);

        // Create test invoice
        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setInvoiceNumber("001-01-2025");
        testInvoice.setIssueDate(LocalDate.of(2025, 1, 15));
        testInvoice.setTotalAmount(new BigDecimal("250.00"));
        testInvoice.setClient(testClient);
        testInvoice.setSeller(seller);
        testInvoice.setItemsList(new ArrayList<>());
    }

    @Test
    void shouldGeneratePdfSuccessfully_whenValidInvoiceWithSingleItem() {
        // given
        InvoiceItem item = createInvoiceItem(
                LocalDate.of(2025, 1, 10),
                5.0,
                50.0,
                new BigDecimal("250.00")
        );
        testInvoice.getItemsList().add(item);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        pdfGenerator.generateInvoicePdf(testInvoice, outputStream);

        // then
        assertThat(outputStream.toByteArray()).isNotEmpty();
        assertThat(outputStream.size()).isGreaterThan(1000); // PDF should be substantial
    }

    @Test
    void shouldGeneratePdfSuccessfully_whenValidInvoiceWithMultipleItems() {
        // given
        InvoiceItem item1 = createInvoiceItem(
                LocalDate.of(2025, 1, 10),
                5.0,
                50.0,
                new BigDecimal("250.00")
        );
        InvoiceItem item2 = createInvoiceItem(
                LocalDate.of(2025, 1, 11),
                3.0,
                50.0,
                new BigDecimal("150.00")
        );
        InvoiceItem item3 = createInvoiceItem(
                LocalDate.of(2025, 1, 12),
                4.0,
                50.0,
                new BigDecimal("200.00")
        );

        testInvoice.getItemsList().addAll(List.of(item1, item2, item3));
        testInvoice.setTotalAmount(new BigDecimal("600.00"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        pdfGenerator.generateInvoicePdf(testInvoice, outputStream);

        // then
        assertThat(outputStream.toByteArray()).isNotEmpty();
        assertThat(outputStream.size()).isGreaterThan(1000);
    }

    @Test
    void shouldGeneratePdfSuccessfully_whenInvoiceWithNoItems() {
        // given
        testInvoice.setTotalAmount(BigDecimal.ZERO);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        pdfGenerator.generateInvoicePdf(testInvoice, outputStream);

        // then
        assertThat(outputStream.toByteArray()).isNotEmpty();
    }

    @Test
    void shouldThrowServiceOperationException_whenOutputStreamFails() {
        // given
        InvoiceItem item = createInvoiceItem(
                LocalDate.of(2025, 1, 10),
                5.0,
                50.0,
                new BigDecimal("250.00")
        );
        testInvoice.getItemsList().add(item);

        OutputStream failingOutputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Simulated write failure");
            }
        };

        // when & then
        // Note: OpenPDF wraps IOException in ExceptionConverter, so we just verify ServiceOperationException is thrown
        assertThatThrownBy(() -> pdfGenerator.generateInvoicePdf(testInvoice, failingOutputStream))
                .isInstanceOf(ServiceOperationException.class)
                .hasMessageContaining("Failed to generate PDF");
    }

    @Test
    void shouldGeneratePdfWithCorrectlyFormattedDates() {
        // given
        InvoiceItem item = createInvoiceItem(
                LocalDate.of(2025, 12, 31), // Test year-end date
                5.0,
                50.0,
                new BigDecimal("250.00")
        );
        testInvoice.getItemsList().add(item);
        testInvoice.setIssueDate(LocalDate.of(2025, 12, 31));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        pdfGenerator.generateInvoicePdf(testInvoice, outputStream);

        // then
        assertThat(outputStream.toByteArray()).isNotEmpty();
        // Note: We can't easily verify date format in PDF without parsing,
        // but we ensure it doesn't throw an exception with edge-case dates
    }

    @Test
    void shouldGeneratePdfWithDecimalHourlyRates() {
        // given
        InvoiceItem item = createInvoiceItem(
                LocalDate.of(2025, 1, 10),
                5.5, // Decimal duration
                45.75, // Decimal hourly rate
                new BigDecimal("251.63") // 5.5 * 45.75
        );
        testInvoice.getItemsList().add(item);
        testInvoice.setTotalAmount(new BigDecimal("251.63"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        pdfGenerator.generateInvoicePdf(testInvoice, outputStream);

        // then
        assertThat(outputStream.toByteArray()).isNotEmpty();
    }

    @Test
    void shouldGeneratePdfWithLargeAmounts() {
        // given
        InvoiceItem item = createInvoiceItem(
                LocalDate.of(2025, 1, 10),
                100.0,
                999.99,
                new BigDecimal("99999.00")
        );
        testInvoice.getItemsList().add(item);
        testInvoice.setTotalAmount(new BigDecimal("99999.00"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        pdfGenerator.generateInvoicePdf(testInvoice, outputStream);

        // then
        assertThat(outputStream.toByteArray()).isNotEmpty();
    }

    @Test
    void shouldGeneratePdfWithSpecialCharactersInClientName() {
        // given
        testClient.setClientName("O'Brien & Sons Ltd. (UK)");
        testClient.setStreetName("St. Mary's Road");

        InvoiceItem item = createInvoiceItem(
                LocalDate.of(2025, 1, 10),
                5.0,
                50.0,
                new BigDecimal("250.00")
        );
        testInvoice.getItemsList().add(item);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        pdfGenerator.generateInvoicePdf(testInvoice, outputStream);

        // then
        assertThat(outputStream.toByteArray()).isNotEmpty();
    }

    // Helper method to create invoice items
    private InvoiceItem createInvoiceItem(LocalDate serviceDate, Double duration, Double hourlyRate, BigDecimal amount) {
        InvoiceItem item = new InvoiceItem();
        item.setServiceDate(serviceDate);
        item.setDuration(duration);
        item.setHourlyRate(hourlyRate);
        item.setAmount(amount);
        item.setDescription("Cleaning services");
        return item;
    }
}
