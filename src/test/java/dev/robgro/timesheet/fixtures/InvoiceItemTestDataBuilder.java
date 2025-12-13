package dev.robgro.timesheet.fixtures;

import dev.robgro.timesheet.invoice.InvoiceItem;
import dev.robgro.timesheet.invoice.InvoiceItemDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceItemTestDataBuilder {
    private Long id = 1L;
    private LocalDate serviceDate = LocalDate.now().minusDays(7);
    private String description = "Software Development Services";
    private double duration = 8.0;
    private BigDecimal amount = BigDecimal.valueOf(400.00);
    private double hourlyRate = 50.0;
    private Long timesheetId = null;

    private InvoiceItemTestDataBuilder() {
    }

    public static InvoiceItemTestDataBuilder anInvoiceItem() {
        return new InvoiceItemTestDataBuilder();
    }

    public InvoiceItemTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public InvoiceItemTestDataBuilder onDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
        return this;
    }

    public InvoiceItemTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public InvoiceItemTestDataBuilder withDuration(double duration) {
        this.duration = duration;
        this.amount = BigDecimal.valueOf(duration * hourlyRate).setScale(2, BigDecimal.ROUND_HALF_UP);
        return this;
    }

    public InvoiceItemTestDataBuilder withHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.amount = BigDecimal.valueOf(duration * hourlyRate).setScale(2, BigDecimal.ROUND_HALF_UP);
        return this;
    }

    public InvoiceItemTestDataBuilder withAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public InvoiceItemTestDataBuilder forTimesheet(Long timesheetId) {
        this.timesheetId = timesheetId;
        return this;
    }

    public InvoiceItem build() {
        InvoiceItem item = new InvoiceItem();
        item.setId(id);
        item.setServiceDate(serviceDate);
        item.setDescription(description);
        item.setDuration(duration);
        item.setAmount(amount);
        item.setHourlyRate(hourlyRate);
        item.setTimesheetId(timesheetId);
        return item;
    }

    public InvoiceItemDto buildDto() {
        return new InvoiceItemDto(
            id,
            serviceDate,
            description,
            duration,
            amount,
            hourlyRate
        );
    }
}