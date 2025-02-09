package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.ClientDtoMapper;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientDtoMapper clientDtoMapper;

    @Override
    public List<ClientDto> getAllClients() {
        return clientRepository.findByActiveTrue().stream()
                .map(clientDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    public ClientDto getClientById(Long id) {
        return clientDtoMapper.apply(getClientOrThrow(id));
    }

    private Client getClientOrThrow(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Client with id " + id + " not found"
                ));
    }

    @Override
    public ClientDto createClient(ClientDto clientDto) {
        Client client = new Client();
        client.setActive(true);
        updateClientFields(client, clientDto);
        return clientDtoMapper.apply(clientRepository.save(client));
    }

    @Override
    public ClientDto updateClient(Long id, ClientDto clientDto) {
        Client client = getClientOrThrow(id);
        updateClientFields(client, clientDto);
        return clientDtoMapper.apply(clientRepository.save(client));
    }

    private void updateClientFields(Client client, ClientDto clientDto) {
        client.setClientName(clientDto.clientName());
        client.setEmail(clientDto.email());
        client.setHouseNo(clientDto.houseNo());
        client.setStreetName(clientDto.streetName());
        client.setPostCode(clientDto.postCode());
        client.setCity(clientDto.city());
        client.setHourlyRate(clientDto.hourlyRate());
    }

    @Transactional
    @Override
    public void deleteClient(Long id) {
        Client client = getClientOrThrow(id);
        client.setActive(false);
        clientRepository.save(client);
        log.info("Client with id {} has been deactivated", id);
    }
}
