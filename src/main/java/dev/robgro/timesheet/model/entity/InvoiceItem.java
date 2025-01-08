package dev.robgro.timesheet.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "description")
    private String description = "Cleaning service ";

    @Column(name = "duration")
    private Double duration;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "timesheet_id")
    private Long timesheetId;
}
