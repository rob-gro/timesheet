package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByActiveTrue();
}
