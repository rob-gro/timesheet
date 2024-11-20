package dev.robgro.timesheet.model;

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

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "duration")
    private double duration;

    public Timesheet(Long id, Client client, String clientName, LocalDate date, double duration) {
        this.id = id;
        this.client = client;
        this.clientName = clientName;
        this.date = date;
        this.duration = duration;
    }

    public Timesheet() {
    }

    @Override
    public String toString() {
        return "Timesheet{" +
                "id=" + id +
                ", client=" + client +
                ", clientName='" + clientName + '\'' +
                ", date=" + date +
                ", duration=" + duration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timesheet timesheet = (Timesheet) o;
        return Double.compare(duration, timesheet.duration) == 0 && Objects.equals(id, timesheet.id) && Objects.equals(client, timesheet.client) && Objects.equals(clientName, timesheet.clientName) && Objects.equals(date, timesheet.date);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(client);
        result = 31 * result + Objects.hashCode(clientName);
        result = 31 * result + Objects.hashCode(date);
        result = 31 * result + Double.hashCode(duration);
        return result;
    }
}
