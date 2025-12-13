package dev.robgro.timesheet.fixtures;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.client.ClientDto;

public class ClientTestDataBuilder {
    private Long id = 1L;
    private String clientName = "Test Client";
    private double hourlyRate = 50.0;
    private long houseNo = 1L;
    private String streetName = "Main Street";
    private String city = "Test City";
    private String postCode = "12345";
    private String email = "test@client.com";
    private boolean active = true;

    private ClientTestDataBuilder() {
    }

    public static ClientTestDataBuilder aClient() {
        return new ClientTestDataBuilder();
    }

    public ClientTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ClientTestDataBuilder withName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public ClientTestDataBuilder withHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
        return this;
    }

    public ClientTestDataBuilder withAddress(long houseNo, String streetName, String city, String postCode) {
        this.houseNo = houseNo;
        this.streetName = streetName;
        this.city = city;
        this.postCode = postCode;
        return this;
    }

    public ClientTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public ClientTestDataBuilder inactive() {
        this.active = false;
        return this;
    }

    public ClientTestDataBuilder active() {
        this.active = true;
        return this;
    }

    public Client build() {
        Client client = new Client();
        client.setId(id);
        client.setClientName(clientName);
        client.setHourlyRate(hourlyRate);
        client.setHouseNo(houseNo);
        client.setStreetName(streetName);
        client.setCity(city);
        client.setPostCode(postCode);
        client.setEmail(email);
        client.setActive(active);
        return client;
    }

    public ClientDto buildDto() {
        return new ClientDto(
            id,
            clientName,
            hourlyRate,
            houseNo,
            streetName,
            city,
            postCode,
            email,
            active
        );
    }
}