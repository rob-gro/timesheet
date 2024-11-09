package dev.robgro.timesheet.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "timesheets")
public class Timesheet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;
    private LocalDate date;
    private double duration;



}
