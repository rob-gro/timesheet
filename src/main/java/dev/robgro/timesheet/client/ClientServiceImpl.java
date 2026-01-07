package dev.robgro.timesheet.client;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientDtoMapper clientDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> getAllClients() {
        return clientRepository.findAllActiveOrderByName().stream()
                .map(clientDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> getAllClients(boolean includeInactive) {
        List<Client> clients = includeInactive
                ? clientRepository.findAllOrderByActiveAndName()
                : clientRepository.findAllActiveOrderByName();
        return clients.stream()
                .map(clientDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getClientById(Long id) {
        return clientDtoMapper.apply(getClientOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> searchClientsByName(String name) {
        return clientRepository.findActiveClientsByName(name).stream()
                .map(clientDtoMapper)
                .collect(Collectors.toList());
    }

    private Client getClientOrThrow(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client", id));
    }

    @Override
    public ClientDto saveClient(ClientDto clientDto) {
        if (clientDto.id() == null) {
            return createClient(clientDto);
        } else {
            return updateClient(clientDto.id(), clientDto);
        }
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
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client", id));
        client.setActive(false);
        clientRepository.save(client);
        log.info("Client with id {} has been deactivated", id);
    }

    @Transactional
    @Override
    public OperationResult deactivateClient(Long id) {
        try {
            Client client = getClientOrThrow(id);
            client.setActive(false);
            clientRepository.save(client);
            log.info("Client with id {} has been deactivated", id);
            return new OperationResult(true, "Client has been successfully deactivated");
        } catch (Exception e) {
            log.error("Failed to deactivate client with id: {}", id, e);
            return new OperationResult(false, "Unable to deactivate client");
        }
    }

    @Transactional
    @Override
    public OperationResult setActiveStatus(Long id, boolean active) {
        try {
            Client client = getClientOrThrow(id);
            client.setActive(active);
            clientRepository.save(client);
            String action = active ? "reactivated" : "deactivated";
            log.info("Client {} has been {}", id, action);
            return new OperationResult(true, "Client has been successfully " + action);
        } catch (Exception e) {
            log.error("Failed to set active status for client {}", id, e);
            return new OperationResult(false, "Unable to update client status");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto createEmptyClientDto() {
        return new ClientDto(
                null,    // id
                "",         // clientName
                0.0,        // hourlyRate
                0L,         // houseNo
                "",         // streetName
                "",         // city
                "",         // postCode
                "",         // email
                true        // is active
        );
    }
}
