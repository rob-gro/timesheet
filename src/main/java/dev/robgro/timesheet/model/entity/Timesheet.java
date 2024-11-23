package dev.robgro.timesheet.model.entity;

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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "duration")
    private double duration;

    @Column(name = "is_invoice")
    private boolean isInvoice;

    public Timesheet(Long id, Client client, LocalDate serviceDate, double duration, boolean isInvoice) {
        this.id = id;
        this.client = client;
        this.serviceDate = serviceDate;
        this.duration = duration;
        this.isInvoice = isInvoice;
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
                ", isInvoice=" + isInvoice +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Timesheet timesheet = (Timesheet) o;
        return Double.compare(duration, timesheet.duration) == 0 && isInvoice == timesheet.isInvoice && Objects.equals(id, timesheet.id) && Objects.equals(client, timesheet.client) && Objects.equals(serviceDate, timesheet.serviceDate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(client);
        result = 31 * result + Objects.hashCode(serviceDate);
        result = 31 * result + Double.hashCode(duration);
        result = 31 * result + Boolean.hashCode(isInvoice);
        return result;
    }
}
