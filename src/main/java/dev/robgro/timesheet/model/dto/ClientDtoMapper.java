package dev.robgro.timesheet.model.dto;

import dev.robgro.timesheet.model.entity.Client;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ClientDtoMapper implements Function<Client, ClientDto> {
    @Override
    public ClientDto apply(Client client) {
        return new ClientDto(
                client.getId(),
                client.getClientName(),
                client.getHourlyRate(),
                client.getHouseNo(),
                client.getStreetName(),
                client.getCity(),
                client.getPostCode(),
                client.getEmail()
        );
    }
}
