package dev.robgro.timesheet.service;

import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.invoice.InvoiceNumberGeneratorImpl;
import dev.robgro.timesheet.invoice.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceNumberGeneratorImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceNumberGeneratorImpl invoiceNumberGenerator;

    // ----- Basic Number Generation -----

    @Test
    void shouldGenerateFirstInvoiceNumberOfMonth() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "001-01-2023";

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(Collections.emptyList());

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    @Test
    void shouldGenerateInvoiceNumberForNonJanuaryMonth() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 5, 15);
        String expectedFormat = "001-05-2023";

        when(invoiceRepository.findByInvoiceNumberEndingWith("05-2023")).thenReturn(Collections.emptyList());

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    // ----- Sequential Number Generation -----

    @Test
    void shouldGenerateSequentialInvoiceNumber() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "002-01-2023";

        List<Invoice> existingInvoices = new ArrayList<>();
        Invoice invoice1 = new Invoice();
        invoice1.setInvoiceNumber("001-01-2023");
        existingInvoices.add(invoice1);

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    @Test
    void shouldGenerateSequentialInvoiceNumberWithMultipleExisting() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "004-01-2023";

        List<Invoice> existingInvoices = new ArrayList<>();
        Invoice invoice1 = new Invoice();
        invoice1.setInvoiceNumber("001-01-2023");
        Invoice invoice2 = new Invoice();
        invoice2.setInvoiceNumber("002-01-2023");
        Invoice invoice3 = new Invoice();
        invoice3.setInvoiceNumber("003-01-2023");
        existingInvoices.add(invoice1);
        existingInvoices.add(invoice2);
        existingInvoices.add(invoice3);

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    // ----- Gap Handling -----

    @Test
    void shouldFillGapInInvoiceNumberSequence() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "002-01-2023";

        List<Invoice> existingInvoices = new ArrayList<>();
        Invoice invoice1 = new Invoice();
        invoice1.setInvoiceNumber("001-01-2023");
        Invoice invoice3 = new Invoice();
        invoice3.setInvoiceNumber("003-01-2023");
        existingInvoices.add(invoice1);
        existingInvoices.add(invoice3);

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    @Test
    void shouldFillFirstGapInInvoiceNumberSequence() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "001-01-2023";

        List<Invoice> existingInvoices = new ArrayList<>();
        Invoice invoice2 = new Invoice();
        invoice2.setInvoiceNumber("002-01-2023");
        Invoice invoice3 = new Invoice();
        invoice3.setInvoiceNumber("003-01-2023");
        existingInvoices.add(invoice2);
        existingInvoices.add(invoice3);

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    @Test
    void shouldFindMiddleGapInLargerSequence() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "003-01-2023";

        List<Invoice> existingInvoices = new ArrayList<>();
        Invoice invoice1 = new Invoice();
        invoice1.setInvoiceNumber("001-01-2023");
        Invoice invoice2 = new Invoice();
        invoice2.setInvoiceNumber("002-01-2023");
        Invoice invoice4 = new Invoice();
        invoice4.setInvoiceNumber("004-01-2023");
        Invoice invoice5 = new Invoice();
        invoice5.setInvoiceNumber("005-01-2023");
        existingInvoices.add(invoice1);
        existingInvoices.add(invoice2);
        existingInvoices.add(invoice4);
        existingInvoices.add(invoice5);

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    // ----- Edge Cases -----

    @Test
    void shouldHandleUnsortedExistingInvoiceNumbers() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "004-01-2023";

        List<Invoice> existingInvoices = new ArrayList<>();
        Invoice invoice3 = new Invoice();
        invoice3.setInvoiceNumber("003-01-2023");
        Invoice invoice1 = new Invoice();
        invoice1.setInvoiceNumber("001-01-2023");
        Invoice invoice2 = new Invoice();
        invoice2.setInvoiceNumber("002-01-2023");
        existingInvoices.add(invoice3); // Unsorted order
        existingInvoices.add(invoice1);
        existingInvoices.add(invoice2);

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    @Test
    void shouldHandleYearChange() {
        // given
        LocalDate issueDate = LocalDate.of(2024, 1, 15);
        String expectedFormat = "001-01-2024";

        // Previous year's invoices shouldn't affect the numbering
        List<Invoice> existingInvoices = new ArrayList<>();

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2024")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }

    @Test
    void shouldHandleSingleDigitMonths() {
        // given
        LocalDate issueDateSingleDigit = LocalDate.of(2023, 1, 15);
        String expectedFormatSingleDigit = "001-01-2023";

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(Collections.emptyList());

        // when
        String resultSingleDigit = invoiceNumberGenerator.generateInvoiceNumber(issueDateSingleDigit);

        // then
        assertThat(resultSingleDigit).isEqualTo(expectedFormatSingleDigit);

        // given
        LocalDate issueDateDoubleDigit = LocalDate.of(2023, 10, 15);
        String expectedFormatDoubleDigit = "001-10-2023";

        when(invoiceRepository.findByInvoiceNumberEndingWith("10-2023")).thenReturn(Collections.emptyList());

        // when
        String resultDoubleDigit = invoiceNumberGenerator.generateInvoiceNumber(issueDateDoubleDigit);

        // then
        assertThat(resultDoubleDigit).isEqualTo(expectedFormatDoubleDigit);
    }

    @Test
    void shouldHandleLargeInvoiceNumbers() {
        // given
        LocalDate issueDate = LocalDate.of(2023, 1, 15);
        String expectedFormat = "101-01-2023";

        List<Invoice> existingInvoices = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(String.format("%03d-01-2023", i));
            existingInvoices.add(invoice);
        }

        when(invoiceRepository.findByInvoiceNumberEndingWith("01-2023")).thenReturn(existingInvoices);

        // when
        String result = invoiceNumberGenerator.generateInvoiceNumber(issueDate);

        // then
        assertThat(result).isEqualTo(expectedFormat);
    }
}
