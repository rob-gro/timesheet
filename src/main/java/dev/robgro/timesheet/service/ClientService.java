package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;

import java.util.List;

public interface ClientService {
    List<ClientDto> getAllClients();
    ClientDto getClientById(Long id);
}
