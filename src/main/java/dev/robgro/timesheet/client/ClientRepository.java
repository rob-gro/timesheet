package dev.robgro.timesheet.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByActiveTrue();

    @Query("SELECT c FROM Client c WHERE " +
            "c.active = true AND " +
            "(:name IS NULL OR LOWER(c.clientName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Client> findActiveClientsByName(@Param("name") String name);

    @Query("SELECT c FROM Client c WHERE c.active = true ORDER BY c.clientName ASC")
    List<Client> findAllActiveOrderByName();
}
