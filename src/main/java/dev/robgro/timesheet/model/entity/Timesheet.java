package dev.robgro.timesheet.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

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

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "duration")
    private double duration;

    @Column(name = "is_invoice")
    private boolean invoiced;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "invoice_id", referencedColumnName = "id")
    private Invoice invoice;

    @Formula("(SELECT i.invoice_number FROM invoices i WHERE i.id = invoice_id)")
    private String invoiceNumber;

    public Timesheet(Long id, Client client, LocalDate serviceDate, double duration, boolean invoiced) {
        this.id = id;
        this.client = client;
        this.serviceDate = serviceDate;
        this.duration = duration;
        this.invoiced = invoiced;
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
                ", invoice=" + (invoice != null ? invoice.getInvoiceNumber() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Timesheet timesheet = (Timesheet) o;
        return Double.compare(duration, timesheet.duration) == 0 && invoiced == timesheet.invoiced && Objects.equals(id, timesheet.id) && Objects.equals(client, timesheet.client) && Objects.equals(serviceDate, timesheet.serviceDate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(client);
        result = 31 * result + Objects.hashCode(serviceDate);
        result = 31 * result + Double.hashCode(duration);
        result = 31 * result + Boolean.hashCode(invoiced);
        return result;
    }
}
