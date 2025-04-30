package dev.robgro.timesheet.service;

import dev.robgro.timesheet.client.ClientServiceImpl;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.client.ClientDtoMapper;
import dev.robgro.timesheet.client.OperationResult;
import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.client.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientDtoMapper clientDtoMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    // ----- Basic Client Retrieval -----

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
                .hasMessageContaining("Client with id 1 not found");
    }

    // ----- Client Creation and Update -----

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
    void shouldCreateClient() {
        // given
        ClientDto clientDto = new ClientDto(null, "New Client", 50.0, 1L, "Street", "City", "12345", "email@test.com", true);
        Client savedClient = new Client();
        savedClient.setId(1L);
        savedClient.setClientName("New Client");
        savedClient.setActive(true);

        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(clientDtoMapper.apply(savedClient)).thenReturn(new ClientDto(1L, "New Client", 50.0, 1L, "Street", "City", "12345", "email@test.com", true));

        // when
        ClientDto result = clientService.createClient(clientDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.clientName()).isEqualTo("New Client");

        verify(clientRepository).save(argThat(client -> client.isActive()));
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
    void shouldUpdateExistingClientWhenSavingWithId() {
        // given
        Long clientId = 1L;
        ClientDto clientDto = new ClientDto(clientId, "Updated Client", 50.0, 1L, "Street", "City", "12345", "email@test.com", true);
        Client existingClient = new Client();
        existingClient.setId(clientId);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(Client.class))).thenReturn(existingClient);
        when(clientDtoMapper.apply(existingClient)).thenReturn(clientDto);

        // when
        ClientDto result = clientService.saveClient(clientDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(clientId);
        verify(clientRepository).findById(clientId);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void shouldUpdateClientFields() {
        // given
        Long clientId = 1L;
        ClientDto clientDto = new ClientDto(clientId, "Updated Client", 60.0, 2L, "New Street", "New City", "54321", "new@test.com", true);
        Client client = new Client();
        client.setId(clientId);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientDtoMapper.apply(client)).thenReturn(clientDto);

        // when
        clientService.updateClient(clientId, clientDto);

        // then
        verify(clientRepository).save(argThat(updatedClient ->
                updatedClient.getClientName().equals("Updated Client") &&
                        updatedClient.getHourlyRate() == 60.0 &&
                        updatedClient.getHouseNo() == 2L &&
                        updatedClient.getStreetName().equals("New Street") &&
                        updatedClient.getCity().equals("New City") &&
                        updatedClient.getPostCode().equals("54321") &&
                        updatedClient.getEmail().equals("new@test.com")
        ));
    }

    // ----- Client Deletion and Deactivation -----

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
    void shouldThrowExceptionWhenDeletingNonExistentClient() {
        // given
        Long clientId = 999L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> clientService.deleteClient(clientId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client with id 999 not found");

        verify(clientRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateAlreadyInactiveClient() {
        // given
        Long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        client.setActive(false);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // when
        OperationResult result = clientService.deactivateClient(clientId);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Client has been successfully deactivated");
        verify(clientRepository).save(client);
    }

    @Test
    void shouldHandleExceptionWhenDeactivatingClient() {
        // given
        Long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        client.setActive(true);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenThrow(new RuntimeException("Database error"));

        // when
        OperationResult result = clientService.deactivateClient(clientId);

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Unable to deactivate client");
        verify(clientRepository).findById(clientId);
        verify(clientRepository).save(client);
    }

    @Test
    void shouldReturnFailureWhenClientNotFound() {
        // given
        Long clientId = 999L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // when
        OperationResult result = clientService.deactivateClient(clientId);

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Unable to deactivate client");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void shouldReturnFailureWhenSaveFails() {
        // given
        Long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        client.setActive(true);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenThrow(new RuntimeException("Database error"));

        // when
        OperationResult result = clientService.deactivateClient(clientId);

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Unable to deactivate client");
        verify(clientRepository).findById(clientId);
        verify(clientRepository).save(client);
    }

    // ----- Client Search Operations -----

    @Test
    void shouldSearchClientsByName() {
        // given
        String searchName = "Test";
        Client client1 = new Client();
        Client client2 = new Client();

        when(clientRepository.findActiveClientsByName(searchName)).thenReturn(List.of(client1, client2));
        when(clientDtoMapper.apply(client1)).thenReturn(new ClientDto(1L, "Test Client 1", 50.0, 1L, "Street", "City", "12345", "email1@test.com", true));
        when(clientDtoMapper.apply(client2)).thenReturn(new ClientDto(2L, "Test Client 2", 60.0, 2L, "Street", "City", "54321", "email2@test.com", true));

        // when
        List<ClientDto> result = clientService.searchClientsByName(searchName);

        // then
        assertThat(result).hasSize(2);
        verify(clientRepository).findActiveClientsByName(searchName);
        verify(clientDtoMapper, times(2)).apply(any(Client.class));
    }

    // ----- Utility Methods -----

    @Test
    void shouldCreateEmptyClientDto() {
        // when
        ClientDto result = clientService.createEmptyClientDto();

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isNull();
        assertThat(result.clientName()).isEmpty();
        assertThat(result.hourlyRate()).isEqualTo(0.0);
        assertThat(result.houseNo()).isEqualTo(0L);
        assertThat(result.streetName()).isEmpty();
        assertThat(result.city()).isEmpty();
        assertThat(result.postCode()).isEmpty();
        assertThat(result.email()).isEmpty();
        assertThat(result.active()).isTrue();
    }
}
