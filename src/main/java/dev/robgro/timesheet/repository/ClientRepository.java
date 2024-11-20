package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
