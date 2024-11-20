package dev.robgro.timesheet.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "house_number", nullable = false)
    private long houseNo;

    @Column(name = "street_name", nullable = false)
    private String streetName;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "post_code", nullable = false)
    private String postCode;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "client")
    List<Timesheet> timesheets;

    public Client(Long id, String clientName, long houseNo, String streetName, String city, String postCode, String email) {
        this.id = id;
        this.clientName = clientName;
        this.houseNo = houseNo;
        this.streetName = streetName;
        this.city = city;
        this.postCode = postCode;
        this.email = email;
    }

    public Client() {
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", clientName='" + clientName + '\'' +
                ", houseNo=" + houseNo +
                ", streetName='" + streetName + '\'' +
                ", city='" + city + '\'' +
                ", postCode='" + postCode + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client client = (Client) o;
        return houseNo == client.houseNo && Objects.equals(id, client.id) && Objects.equals(clientName, client.clientName) && Objects.equals(streetName, client.streetName) && Objects.equals(city, client.city) && Objects.equals(postCode, client.postCode) && Objects.equals(email, client.email);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(clientName);
        result = 31 * result + Long.hashCode(houseNo);
        result = 31 * result + Objects.hashCode(streetName);
        result = 31 * result + Objects.hashCode(city);
        result = 31 * result + Objects.hashCode(postCode);
        result = 31 * result + Objects.hashCode(email);
        return result;
    }
}
