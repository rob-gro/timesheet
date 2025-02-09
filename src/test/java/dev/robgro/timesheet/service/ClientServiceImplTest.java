//package dev.robgro.timesheet.service;
//
//import dev.robgro.timesheet.model.dto.ClientDto;
//import dev.robgro.timesheet.model.dto.ClientDtoMapper;
//import dev.robgro.timesheet.model.entity.Client;
//import dev.robgro.timesheet.model.entity.Invoice;
//import dev.robgro.timesheet.repository.ClientRepository;
//import dev.robgro.timesheet.repository.InvoiceRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
//import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ClientServiceImplTest {
//
//    @Mock
//    private ClientRepository clientRepository;
//
//    @Mock
//    private ClientDtoMapper clientDtoMapper;
//
//    @Mock
//    private InvoiceRepository invoiceRepository;
//
//    @InjectMocks
//    private ClientServiceImpl clientService;
//
//    @Test
//    void shouldGetAllClients() {
//        // given
//        Client client1 = new Client();
//        client1.setId(1L);
//        client1.setClientName("Test Client 1");
//        client1.setHourlyRate(50.0);
//
//        Client client2 = new Client();
//        client2.setId(2L);
//        client2.setClientName("Test Client 2");
//        client2.setHourlyRate(60.0);
//
//        ClientDto clientDto1 = new ClientDto(1L, "Test Client 1", 50.0, 1L, "Street", "City", "12345", "test1@email.com");
//        ClientDto clientDto2 = new ClientDto(2L, "Test Client 2", 60.0, 2L, "Street", "City", "12345", "test2@email.com");
//
//        when(clientRepository.findAll()).thenReturn(List.of(client1, client2));
//        when(clientDtoMapper.apply(client1)).thenReturn(clientDto1);
//        when(clientDtoMapper.apply(client2)).thenReturn(clientDto2);
//
//        // when
//        List<ClientDto> result = clientService.getAllClients();
//
//        // then
//        assertThat(result).hasSize(2);
//        assertThat(result).containsExactly(clientDto1, clientDto2);
//        verify(clientRepository).findAll();
//        verify(clientDtoMapper, times(2)).apply(any(Client.class));
//    }
//
//    @Test
//    void shouldGetClientById() {
//        // given
//        Long clientId = 1L;
//        Client client = new Client();
//        client.setId(clientId);
//        client.setClientName("Test Client");
//        client.setHourlyRate(50.0);
//
//        ClientDto expectedDto = new ClientDto(clientId, "Test Client", 50.0, 1L, "Street", "City", "12345", "test@email.com");
//
//        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
//        when(clientDtoMapper.apply(client)).thenReturn(expectedDto);
//
//        // when
//        ClientDto result = clientService.getClientById(clientId);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.id()).isEqualTo(clientId);
//        verify(clientRepository).findById(clientId);
//        verify(clientDtoMapper).apply(client);
//    }
//
//    @Test
//    void shouldThrowExceptionWhenClientNotFound() {
//        // given
//        Long clientId = 1L;
//        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());
//
//        // when/then
//        assertThatThrownBy(() -> clientService.getClientById(clientId))
//                .isInstanceOf(ResponseStatusException.class)
//                .hasMessageContaining("Client with id " + clientId + " not found");
//    }
//
//    @Test
//    void shouldCreateClient() {
//        // given
//        ClientDto clientDto = new ClientDto(null, "New Client", 50.0, 1L, "Street", "City", "12345", "test@email.com");
//        Client newClient = new Client();
//        newClient.setClientName(clientDto.clientName());
//        newClient.setHourlyRate(clientDto.hourlyRate());
//
//        Client savedClient = new Client();
//        savedClient.setId(1L);
//        savedClient.setClientName(clientDto.clientName());
//        savedClient.setHourlyRate(clientDto.hourlyRate());
//
//        ClientDto expectedDto = new ClientDto(1L, "New Client", 50.0, 1L, "Street", "City", "12345", "test@email.com");
//
//        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
//        when(clientDtoMapper.apply(savedClient)).thenReturn(expectedDto);
//
//        // when
//        ClientDto result = clientService.createClient(clientDto);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.id()).isEqualTo(1L);
//        verify(clientRepository).save(any(Client.class));
//    }
//
//    @Test
//    void shouldNotDeleteClientWithInvoices() {
//        // given
//        Long clientId = 1L;
//        Client client = new Client();
//        client.setId(clientId);
//
//        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
//        when(invoiceRepository.findByClientId(clientId)).thenReturn(List.of(new Invoice()));
//
//        // when/then
//        assertThatThrownBy(() -> clientService.deleteClient(clientId))
//                .isInstanceOf(ResponseStatusException.class)
//                .hasMessageContaining("Cannot delete client with associated invoices");
//
//        verify(clientRepository, never()).delete(any());
//    }
//}
