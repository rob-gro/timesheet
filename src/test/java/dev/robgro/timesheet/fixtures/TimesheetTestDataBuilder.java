package dev.robgro.timesheet.fixtures;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.timesheet.Timesheet;
import dev.robgro.timesheet.timesheet.TimesheetDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TimesheetTestDataBuilder {
    private Long id = 1L;
    private Client client;
    private LocalDate serviceDate = LocalDate.now().minusDays(7);
    private double duration = 8.0;
    private Double hourlyRate = null;
    private boolean invoiced = false;
    private Invoice invoice = null;
    private String invoiceNumber = null;
    private LocalDate paymentDate = null;

    private TimesheetTestDataBuilder() {
    }

    public static TimesheetTestDataBuilder aTimesheet() {
        return new TimesheetTestDataBuilder();
    }

    public TimesheetTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TimesheetTestDataBuilder forClient(Client client) {
        this.client = client;
        return this;
    }

    public TimesheetTestDataBuilder onDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
        return this;
    }

    public TimesheetTestDataBuilder withDuration(double duration) {
        this.duration = duration;
        return this;
    }

    public TimesheetTestDataBuilder withHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
        return this;
    }

    public TimesheetTestDataBuilder invoiced() {
        this.invoiced = true;
        return this;
    }

    public TimesheetTestDataBuilder notInvoiced() {
        this.invoiced = false;
        this.invoice = null;
        this.invoiceNumber = null;
        return this;
    }

    public TimesheetTestDataBuilder withInvoice(Invoice invoice) {
        this.invoice = invoice;
        this.invoiced = true;
        this.invoiceNumber = invoice != null ? invoice.getInvoiceNumber() : null;
        return this;
    }

    public TimesheetTestDataBuilder paid(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
        return this;
    }

    public TimesheetTestDataBuilder unpaid() {
        this.paymentDate = null;
        return this;
    }

    public Timesheet build() {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(id);
        timesheet.setClient(client != null ? client : ClientTestDataBuilder.aClient().build());
        timesheet.setServiceDate(serviceDate);
        timesheet.setDuration(duration);
        timesheet.setHourlyRate(hourlyRate);
        timesheet.setInvoiced(invoiced);
        timesheet.setInvoice(invoice);
        timesheet.setInvoiceNumber(invoiceNumber);
        timesheet.setPaymentDate(paymentDate);
        return timesheet;
    }

    public TimesheetDto buildDto() {
        Timesheet timesheet = build();
        Client effectiveClient = client != null ? client : ClientTestDataBuilder.aClient().build();
        double effectiveRate = hourlyRate != null ? hourlyRate : effectiveClient.getHourlyRate();

        BigDecimal value = BigDecimal.valueOf(duration * effectiveRate)
            .setScale(2, BigDecimal.ROUND_HALF_UP);

        return new TimesheetDto(
            id,
            effectiveClient.getClientName(),
            serviceDate,
            duration,
            invoiced,
            effectiveClient.getId(),
            effectiveRate,
            invoiceNumber,
            paymentDate,
            value
        );
    }
}