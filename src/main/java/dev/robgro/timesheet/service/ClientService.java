package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.OperationResult;

import java.util.List;

public interface ClientService {
    List<ClientDto> getAllClients();

    ClientDto getClientById(Long id);

    ClientDto createClient(ClientDto clientDto);

    ClientDto updateClient(Long id, ClientDto clientDto);

    void deleteClient(Long id);

    ClientDto saveClient(ClientDto clientDto);

    OperationResult deactivateClient(Long id);
}
