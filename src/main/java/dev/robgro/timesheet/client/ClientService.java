package dev.robgro.timesheet.client;

import java.util.List;

public interface ClientService {
    List<ClientDto> getAllClients();

    ClientDto getClientById(Long id);

    List<ClientDto> searchClientsByName(String name);

    ClientDto createClient(ClientDto clientDto);

    ClientDto updateClient(Long id, ClientDto clientDto);

    void deleteClient(Long id);

    ClientDto saveClient(ClientDto clientDto);

    OperationResult deactivateClient(Long id);

    ClientDto createEmptyClientDto();
}
