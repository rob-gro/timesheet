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

    @Column(name = "is_invoice")
    private boolean invoiced;

    @ManyToOne(fetch = FetchType.LAZY)  //  cascade = CascadeType.REFRESH -> don't know if it's necessary
    @JoinColumn(name = "invoice_id", referencedColumnName = "id")
    private Invoice invoice;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    public Timesheet(Long id, Client client, LocalDate serviceDate, double duration, boolean invoiced, LocalDate paymentDate) {
        this.id = id;
        this.client = client;
        this.serviceDate = serviceDate;
        this.duration = duration;
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
        return Double.compare(duration, timesheet.duration) == 0 && invoiced == timesheet.invoiced && Objects.equals(id, timesheet.id) && Objects.equals(client, timesheet.client) && Objects.equals(serviceDate, timesheet.serviceDate) && Objects.equals(invoice, timesheet.invoice) && Objects.equals(invoiceNumber, timesheet.invoiceNumber) && Objects.equals(paymentDate, timesheet.paymentDate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(client);
        result = 31 * result + Objects.hashCode(serviceDate);
        result = 31 * result + Double.hashCode(duration);
        result = 31 * result + Boolean.hashCode(invoiced);
        result = 31 * result + Objects.hashCode(invoice);
        result = 31 * result + Objects.hashCode(invoiceNumber);
        result = 31 * result + Objects.hashCode(paymentDate);
        return result;
    }
}
