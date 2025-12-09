package dev.robgro.timesheet.timesheet;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.invoice.Invoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "timesheets")
public class Timesheet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "duration")
    private double duration;

    @Column(name = "hourly_rate")
    private Double hourlyRate;

    @Column(name = "is_invoice")
    private boolean invoiced;

    @ManyToOne(fetch = FetchType.LAZY)  //  cascade = CascadeType.REFRESH -> don't know if it's necessary
    @JoinColumn(name = "invoice_id", referencedColumnName = "id")
    private Invoice invoice;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Returns the effective hourly rate for this timesheet.
     * If hourlyRate is not set (null or 0 - for old timesheets), returns the client's current hourly rate.
     * Otherwise, returns the timesheet's stored hourly rate.
     *
     * @return the effective hourly rate to use for this timesheet
     */
    public double getEffectiveHourlyRate() {
        return (hourlyRate != null && hourlyRate > 0) ? hourlyRate : client.getHourlyRate();
    }

    /**
     * Calculates and returns the monetary value of this timesheet.
     * Value is calculated as duration multiplied by the effective hourly rate.
     *
     * @return the monetary value of this timesheet, rounded to 2 decimal places
     */
    public java.math.BigDecimal getValue() {
        return java.math.BigDecimal.valueOf(duration * getEffectiveHourlyRate())
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public Timesheet(Long id, Client client, LocalDate serviceDate, double duration, double hourlyRate, boolean invoiced, LocalDate paymentDate) {
        this.id = id;
        this.client = client;
        this.serviceDate = serviceDate;
        this.duration = duration;
        this.hourlyRate = hourlyRate;
        this.invoiced = invoiced;
        this.paymentDate = paymentDate;
    }

    public Timesheet() {
    }

    @Override
    public String toString() {
        return "Timesheet{" +
                "id=" + id +
                ", client=" + client +
                ", serviceDate=" + serviceDate +
                ", duration=" + duration +
                ", hourlyRate=" + hourlyRate +
                ", invoiced=" + invoiced +
                ", invoice=" + invoice +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", paymentDate=" + paymentDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Timesheet timesheet = (Timesheet) o;
        return Double.compare(duration, timesheet.duration) == 0 && invoiced == timesheet.invoiced && Objects.equals(id, timesheet.id) && Objects.equals(client, timesheet.client) && Objects.equals(serviceDate, timesheet.serviceDate) && Objects.equals(hourlyRate, timesheet.hourlyRate) && Objects.equals(invoice, timesheet.invoice) && Objects.equals(invoiceNumber, timesheet.invoiceNumber) && Objects.equals(paymentDate, timesheet.paymentDate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(client);
        result = 31 * result + Objects.hashCode(serviceDate);
        result = 31 * result + Double.hashCode(duration);
        result = 31 * result + Objects.hashCode(hourlyRate);
        result = 31 * result + Boolean.hashCode(invoiced);
        result = 31 * result + Objects.hashCode(invoice);
        result = 31 * result + Objects.hashCode(invoiceNumber);
        result = 31 * result + Objects.hashCode(paymentDate);
        return result;
    }
}
