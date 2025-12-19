package dev.robgro.timesheet.seller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    List<Seller> findByActiveTrue();

    @Query("SELECT s FROM Seller s WHERE " +
           "s.active = true AND " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Seller> findActiveSellersByName(@Param("name") String name);

    @Query("SELECT s FROM Seller s WHERE s.active = true ORDER BY s.name ASC")
    List<Seller> findAllActiveOrderByName();
}