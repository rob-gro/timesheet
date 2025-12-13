package dev.robgro.timesheet.fixtures;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.invoice.InvoiceDto;
import dev.robgro.timesheet.invoice.InvoiceItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvoiceTestDataBuilder {
    private Long id = 1L;
    private String invoiceNumber = "001-12-2024";
    private LocalDate issueDate = LocalDate.now();
    private BigDecimal totalAmount = BigDecimal.valueOf(400.00);
    private Client client;
    private List<InvoiceItem> itemsList = new ArrayList<>();
    private LocalDateTime issuedDate = null;
    private String pdfPath = null;
    private LocalDateTime pdfGeneratedAt = null;
    private LocalDateTime emailSentAt = null;

    private InvoiceTestDataBuilder() {
    }

    public static InvoiceTestDataBuilder anInvoice() {
        return new InvoiceTestDataBuilder();
    }

    public InvoiceTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public InvoiceTestDataBuilder withInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
        return this;
    }

    public InvoiceTestDataBuilder issuedOn(LocalDate issueDate) {
        this.issueDate = issueDate;
        return this;
    }

    public InvoiceTestDataBuilder forClient(Client client) {
        this.client = client;
        return this;
    }

    public InvoiceTestDataBuilder withTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public InvoiceTestDataBuilder withItem(InvoiceItem item) {
        this.itemsList.add(item);
        item.setInvoice(this.build());
        return this;
    }

    public InvoiceTestDataBuilder withItems(List<InvoiceItem> items) {
        this.itemsList.addAll(items);
        items.forEach(item -> item.setInvoice(this.build()));
        return this;
    }

    public InvoiceTestDataBuilder withPdf(String pdfPath) {
        this.pdfPath = pdfPath;
        this.pdfGeneratedAt = LocalDateTime.now();
        return this;
    }

    public InvoiceTestDataBuilder emailSent() {
        this.emailSentAt = LocalDateTime.now();
        return this;
    }

    public InvoiceTestDataBuilder issued() {
        this.issuedDate = LocalDateTime.now();
        return this;
    }

    public Invoice build() {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(issueDate);
        invoice.setTotalAmount(totalAmount);
        invoice.setClient(client != null ? client : ClientTestDataBuilder.aClient().build());
        invoice.setItemsList(itemsList);
        invoice.setIssuedDate(issuedDate);
        invoice.setPdfPath(pdfPath);
        invoice.setPdfGeneratedAt(pdfGeneratedAt);
        invoice.setEmailSentAt(emailSentAt);
        return invoice;
    }

    public InvoiceDto buildDto() {
        Invoice invoice = build();
        Client effectiveClient = client != null ? client : ClientTestDataBuilder.aClient().build();

        return new InvoiceDto(
            id,
            effectiveClient.getId(),
            effectiveClient.getClientName(),
            invoiceNumber,
            issueDate,
            totalAmount,
            pdfPath,
            itemsList.stream()
                .map(item -> InvoiceItemTestDataBuilder.anInvoiceItem()
                    .withId(item.getId())
                    .onDate(item.getServiceDate())
                    .withDescription(item.getDescription())
                    .withDuration(item.getDuration())
                    .withAmount(item.getAmount())
                    .withHourlyRate(item.getHourlyRate())
                    .buildDto())
                .toList(),
            pdfGeneratedAt,
            emailSentAt
        );
    }
}