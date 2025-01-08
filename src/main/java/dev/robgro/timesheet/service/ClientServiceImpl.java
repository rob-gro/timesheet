package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.ClientDtoMapper;
import dev.robgro.timesheet.model.dto.InvoiceDtoMapper;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientDtoMapper clientDtoMapper;
    private final InvoiceRepository invoiceRepository;

    @Override
    public List<ClientDto> getAllClients() {
        return clientRepository.findAll().stream()
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
        List<Invoice> invoices = invoiceRepository.findByClientId(id);

        if (!invoices.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete client with associated invoices. Delete invoices first."
            );
        }
        clientRepository.delete(client);
    }
}
