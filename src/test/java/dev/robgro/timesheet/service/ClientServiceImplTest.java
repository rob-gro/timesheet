package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.ClientDtoMapper;
import dev.robgro.timesheet.model.dto.OperationResult;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientDtoMapper clientDtoMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    public ClientServiceImplTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldGetAllClients() {
        // given
        Client client1 = new Client();
        Client client2 = new Client();
        when(clientRepository.findAllActiveOrderByName()).thenReturn(List.of(client1, client2));
        when(clientDtoMapper.apply(client1)).thenReturn(new ClientDto(1L, "Client 1", 50.0, 1L, "Street", "City", "12345", "email1@test.com", true));
        when(clientDtoMapper.apply(client2)).thenReturn(new ClientDto(2L, "Client 2", 60.0, 2L, "Street", "City", "54321", "email2@test.com", true));

        // when
        List<ClientDto> result = clientService.getAllClients();

        // then
        assertThat(result).hasSize(2);
        verify(clientRepository).findAllActiveOrderByName();
        verify(clientDtoMapper, times(2)).apply(any(Client.class));
    }

    @Test
    void shouldGetClientById() {
        // given
        Long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientDtoMapper.apply(client)).thenReturn(new ClientDto(clientId, "Client", 50.0, 1L, "Street", "City", "12345", "email@test.com", true));

        // when
        ClientDto result = clientService.getClientById(clientId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(clientId);
        verify(clientRepository).findById(clientId);
        verify(clientDtoMapper).apply(client);
    }

    @Test
    void shouldThrowExceptionWhenClientNotFound() {
        // given
        Long clientId = 1L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> clientService.getClientById(clientId))
                .isInstanceOf(EntityNotFoundException.class)
//                .hasMessageContaining("Client " + clientId);
                .hasMessageContaining("Client with id 1 not found");
    }

    @Test
    void shouldSaveNewClient() {
        // given
        ClientDto clientDto = new ClientDto(null, "New Client", 50.0, 1L, "Street", "City", "12345", "email@test.com", true);
        Client client = new Client();
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientDtoMapper.apply(client)).thenReturn(new ClientDto(1L, "New Client", 50.0, 1L, "Street", "City", "12345", "email@test.com", true));

        // when
        ClientDto result = clientService.saveClient(clientDto);

        // then
        assertThat(result).isNotNull();
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void shouldUpdateExistingClient() {
        // given
        Long clientId = 1L;
        ClientDto clientDto = new ClientDto(clientId, "Updated Client", 60.0, 1L, "Street", "City", "12345", "email@test.com", true);
        Client client = new Client();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);
        when(clientDtoMapper.apply(client)).thenReturn(clientDto);

        // when
        ClientDto result = clientService.updateClient(clientId, clientDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.clientName()).isEqualTo("Updated Client");
        verify(clientRepository).findById(clientId);
        verify(clientRepository).save(client);
    }

    @Test
    void shouldDeleteClient() {
        // given
        Long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // when
        clientService.deleteClient(clientId);

        // then
        assertThat(client.isActive()).isFalse();
        verify(clientRepository).save(client);
    }

    @Test
    void shouldDeactivateClient() {
        // given
        Long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // when
        OperationResult result = clientService.deactivateClient(clientId);

        // then
        assertThat(result.success()).isTrue();
        verify(clientRepository).save(client);
    }
}
