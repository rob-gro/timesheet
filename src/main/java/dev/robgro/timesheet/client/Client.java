package dev.robgro.timesheet.client;

import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.timesheet.Timesheet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "hourly_rate", nullable = false)
    private double hourlyRate;

    @Column(name = "house_number", nullable = false)
    private String houseNo;

    @Column(name = "street_name", nullable = false)
    private String streetName;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "post_code", nullable = false)
    private String postCode;

    @Column(name = "email", nullable = false)
    private String email;

    @OneToMany(mappedBy = "client")
    List<Timesheet> timesheets;

    @OneToMany(mappedBy = "client")
    List<Invoice> invoices;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", clientName='" + clientName + '\'' +
                ", hourlyRate=" + hourlyRate +
                ", houseNo=" + houseNo +
                ", streetName='" + streetName + '\'' +
                ", city='" + city + '\'' +
                ", postCode='" + postCode + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Client client = (Client) o;
        return Double.compare(hourlyRate, client.hourlyRate) == 0 && Objects.equals(houseNo, client.houseNo) && Objects.equals(id, client.id) && Objects.equals(clientName, client.clientName) && Objects.equals(streetName, client.streetName) && Objects.equals(city, client.city) && Objects.equals(postCode, client.postCode) && Objects.equals(email, client.email);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(clientName);
        result = 31 * result + Double.hashCode(hourlyRate);
        result = 31 * result + Objects.hashCode(houseNo);
        result = 31 * result + Objects.hashCode(streetName);
        result = 31 * result + Objects.hashCode(city);
        result = 31 * result + Objects.hashCode(postCode);
        result = 31 * result + Objects.hashCode(email);
        return result;
    }
}
