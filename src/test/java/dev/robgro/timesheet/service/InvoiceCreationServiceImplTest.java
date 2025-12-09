package dev.robgro.timesheet.service;

import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.invoice.*;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.timesheet.Timesheet;
import dev.robgro.timesheet.client.ClientRepository;
import dev.robgro.timesheet.timesheet.TimesheetRepository;
import dev.robgro.timesheet.timesheet.TimesheetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class  InvoiceCreationServiceImplTest {

    @Mock
    private ClientService clientService;

    @Mock
    private TimesheetService timesheetService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private InvoiceDtoMapper invoiceDtoMapper;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @InjectMocks
    private InvoiceCreationServiceImpl invoiceCreationService;

    // ----- Invoice Creation from Timesheets -----

    @Test
    void shouldCreateInvoiceFromTimesheets() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();

        ClientDto clientDto = new ClientDto(clientId, "Test Client", 50.0, 123, "Address", "City", "12345", "contact@test.com", true);
        Client client = new Client();
        client.setId(clientId);
        client.setClientName("Test Client");
        client.setHourlyRate(50.0);

        List<TimesheetDto> timesheets = List.of(
                new TimesheetDto(1L, "Test Client", LocalDate.now().minusDays(1), 2.0, false, clientId, 50.0, null, null),
                new TimesheetDto(2L, "Test Client", LocalDate.now().minusDays(2), 3.0, false, clientId, 50.0, null, null)
        );

        String invoiceNumber = "001-01-2023";

        Timesheet timesheet1 = new Timesheet();
        timesheet1.setId(1L);

        Timesheet timesheet2 = new Timesheet();
        timesheet2.setId(2L);

        Invoice savedInvoice = new Invoice();
        savedInvoice.setId(1L);
        savedInvoice.setClient(client);
        savedInvoice.setIssueDate(issueDate);
        savedInvoice.setInvoiceNumber(invoiceNumber);
        savedInvoice.setTotalAmount(BigDecimal.valueOf(250.0));

        InvoiceDto expectedDto = new InvoiceDto(
                1L,
                clientId,
                "Test Client",
                "001-01-2023",
                issueDate,
                BigDecimal.valueOf(250.0),
                LocalDateTime.now().toString(),
                List.of(),
                null,
                null
        );

        when(clientRepository.getReferenceById(clientId)).thenReturn(client);
        when(invoiceNumberGenerator.generateInvoiceNumber(issueDate)).thenReturn(invoiceNumber);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet1));
        when(timesheetRepository.findById(2L)).thenReturn(Optional.of(timesheet2));
        when(invoiceRepository.findById(savedInvoice.getId())).thenReturn(Optional.of(savedInvoice));
        when(invoiceDtoMapper.apply(savedInvoice)).thenReturn(expectedDto);

        // when
        InvoiceDto result = invoiceCreationService.createInvoiceFromTimesheets(clientDto, timesheets, issueDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.clientName()).isEqualTo("Test Client");
        assertThat(result.invoiceNumber()).isEqualTo(invoiceNumber);

        verify(invoiceRepository).save(any(Invoice.class));
        verify(timesheetRepository).saveAll(anyList());
        verify(invoiceRepository).findById(savedInvoice.getId());
        verify(invoiceDtoMapper).apply(savedInvoice);
    }

    // ----- Invoice Creation from TimeSheet IDs -----

    @Test
    void shouldCreateInvoiceFromTimesheetIds() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);

        ClientDto clientDto = new ClientDto(clientId, "Test Client", 50.0, 123, "Address", "City", "12345", "contact@test.com", true);

        TimesheetDto timesheet1 = new TimesheetDto(1L, "Test Client", LocalDate.now().minusDays(1), 2.0, false, clientId, 50.0, null, null);
        TimesheetDto timesheet2 = new TimesheetDto(2L, "Test Client", LocalDate.now().minusDays(2), 3.0, false, clientId, 50.0, null, null);
        List<TimesheetDto> timesheets = List.of(timesheet1, timesheet2);

        InvoiceDto expectedDto = new InvoiceDto(
                1L, clientId, "Test Client", "001-01-2023", issueDate,
                BigDecimal.valueOf(250.0), LocalDateTime.now().toString(), List.of(), null, null
        );

        when(clientService.getClientById(clientId)).thenReturn(clientDto);
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);

        // Mock the internal method call to createInvoiceFromTimesheets
        InvoiceCreationServiceImpl serviceSpy = spy(invoiceCreationService);
        doReturn(expectedDto).when(serviceSpy).createInvoiceFromTimesheets(eq(clientDto), anyList(), eq(issueDate));

        // when
        InvoiceDto result = serviceSpy.createInvoice(clientId, issueDate, timesheetIds);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);

        verify(clientService).getClientById(clientId);
        verify(timesheetService, times(2)).getTimesheetById(anyLong());
        verify(serviceSpy).createInvoiceFromTimesheets(eq(clientDto), anyList(), eq(issueDate));
    }

    @Test
    void shouldThrowExceptionWhenNoTimesheetsSelected() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> emptyTimesheetIds = List.of();

        // when/then
        assertThatThrownBy(() -> invoiceCreationService.createInvoice(clientId, issueDate, emptyTimesheetIds))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No timesheets selected for invoice");

        verify(clientService, never()).getClientById(anyLong());
        verify(timesheetService, never()).getTimesheetById(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenAllTimesheetsAlreadyInvoiced() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);

        ClientDto clientDto = new ClientDto(clientId, "Test Client", 50.0, 123, "Address", "City", "12345", "contact@test.com", true);

        TimesheetDto timesheet1 = new TimesheetDto(1L, "Test Client", LocalDate.now().minusDays(1), 2.0, true, clientId, 50.0, null, null);
        TimesheetDto timesheet2 = new TimesheetDto(2L, "Test Client", LocalDate.now().minusDays(2), 3.0, true, clientId, 50.0, null, null);

        when(clientService.getClientById(clientId)).thenReturn(clientDto);
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);

        // when/then
        assertThatThrownBy(() -> invoiceCreationService.createInvoice(clientId, issueDate, timesheetIds))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("All selected timesheets are already invoiced");

        verify(clientService).getClientById(clientId);
        verify(timesheetService, times(2)).getTimesheetById(anyLong());
    }

    // ----- Invoice Item Creation -----

    @Test
    void shouldCreateInvoiceItem() {
        // given
        TimesheetDto timesheet = new TimesheetDto(1L, "Test Client", LocalDate.now(), 2.5, false, 1L, 50.0, null, null);

        Invoice invoice = new Invoice();
        Client client = new Client();
        client.setHourlyRate(50.0);
        invoice.setClient(client);

        Method createItemMethod = ReflectionUtils.findMethod(InvoiceCreationServiceImpl.class, "createInvoiceItem", TimesheetDto.class, Invoice.class)
                .orElseThrow(() -> new EntityNotFoundException("Method", "createInvoiceItem"));
        createItemMethod.setAccessible(true);

        // when
        InvoiceItem result = (InvoiceItem) ReflectionUtils.invokeMethod(createItemMethod, invoiceCreationService, timesheet, invoice);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getInvoice()).isEqualTo(invoice);
        assertThat(result.getServiceDate()).isEqualTo(timesheet.serviceDate());
        assertThat(result.getDuration()).isEqualTo(timesheet.duration());
        assertThat(result.getTimesheetId()).isEqualTo(timesheet.id());
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(125.00).setScale(2, RoundingMode.HALF_UP));
    }

    // ----- Amount Calculation -----

    @Test
    void shouldCalculateAmount() {
        // given
        double duration = 2.5;
        double hourlyRate = 50.0;

        Method calculateMethod = ReflectionUtils.findMethod(InvoiceCreationServiceImpl.class, "calculateAmount", double.class, double.class)
                .orElseThrow(() -> new EntityNotFoundException("Method", "calculateAmount"));
        calculateMethod.setAccessible(true);

        // when
        BigDecimal result = (BigDecimal) ReflectionUtils.invokeMethod(calculateMethod, invoiceCreationService, duration, hourlyRate);

        // then
        assertThat(result).isEqualTo(BigDecimal.valueOf(125.00).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void shouldCalculateTotalAmount() {
        // given
        List<InvoiceItem> items = new ArrayList<>();
        InvoiceItem item1 = new InvoiceItem();
        item1.setAmount(BigDecimal.valueOf(100.50));
        InvoiceItem item2 = new InvoiceItem();
        item2.setAmount(BigDecimal.valueOf(200.75));
        items.add(item1);
        items.add(item2);

        Method calculateTotalMethod = ReflectionUtils.findMethod(InvoiceCreationServiceImpl.class, "calculateTotalAmount", List.class)
                .orElseThrow(() -> new EntityNotFoundException("Method", "calculateTotalAmount"));
        calculateTotalMethod.setAccessible(true);

        // when
        BigDecimal result = (BigDecimal) ReflectionUtils.invokeMethod(calculateTotalMethod, invoiceCreationService, items);

        // then
        assertThat(result).isEqualTo(BigDecimal.valueOf(301.25));
    }

    // ----- Invoice Preview -----

    @Test
    void shouldBuildInvoicePreview() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);

        ClientDto clientDto = new ClientDto(clientId, "Test Client", 50.0, 123, "Address", "City", "12345", "contact@test.com", true);

        TimesheetDto timesheet1 = new TimesheetDto(1L, "Test Client", LocalDate.now().minusDays(1), 2.0, false, clientId, 50.0, null, null);
        TimesheetDto timesheet2 = new TimesheetDto(2L, "Test Client", LocalDate.now().minusDays(2), 3.0, false, clientId, 50.0, null, null);

        String invoiceNumber = "001-01-2023";

        when(clientService.getClientById(clientId)).thenReturn(clientDto);
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);
        when(invoiceNumberGenerator.generateInvoiceNumber(issueDate)).thenReturn(invoiceNumber);

        // when
        InvoiceDto result = invoiceCreationService.buildInvoicePreview(clientId, issueDate, timesheetIds);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isNull(); // Preview has no ID
        assertThat(result.clientId()).isEqualTo(clientId);
        assertThat(result.clientName()).isEqualTo("Test Client");
        assertThat(result.invoiceNumber()).isEqualTo(invoiceNumber);
        assertThat(result.issueDate()).isEqualTo(issueDate);
        assertThat(result.totalAmount()).isEqualTo(BigDecimal.valueOf(250.00).setScale(2, RoundingMode.HALF_UP));
        assertThat(result.pdfPath()).isNull(); // Preview has no PDF path
        assertThat(result.itemsList()).hasSize(2);
        assertThat(result.pdfGeneratedAt()).isNull();
        assertThat(result.emailSentAt()).isNull();

        verify(clientService).getClientById(clientId);
        verify(timesheetService, times(2)).getTimesheetById(anyLong());
        verify(invoiceNumberGenerator).generateInvoiceNumber(issueDate);
        verifyNoInteractions(invoiceRepository); // Preview should not save to DB
        verifyNoInteractions(timesheetRepository); // Preview should not modify timesheets
    }

    @Test
    void shouldThrowExceptionWhenBuildingPreviewWithEmptyTimesheets() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> emptyTimesheetIds = List.of();

        // when/then
        assertThatThrownBy(() -> invoiceCreationService.buildInvoicePreview(clientId, issueDate, emptyTimesheetIds))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No timesheets selected for invoice");

        verify(clientService, never()).getClientById(anyLong());
        verify(timesheetService, never()).getTimesheetById(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenBuildingPreviewWithAllTimesheetsAlreadyInvoiced() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);

        ClientDto clientDto = new ClientDto(clientId, "Test Client", 50.0, 123, "Address", "City", "12345", "contact@test.com", true);

        TimesheetDto timesheet1 = new TimesheetDto(1L, "Test Client", LocalDate.now().minusDays(1), 2.0, true, clientId, 50.0, null, null);
        TimesheetDto timesheet2 = new TimesheetDto(2L, "Test Client", LocalDate.now().minusDays(2), 3.0, true, clientId, 50.0, null, null);

        when(clientService.getClientById(clientId)).thenReturn(clientDto);
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);

        // when/then
        assertThatThrownBy(() -> invoiceCreationService.buildInvoicePreview(clientId, issueDate, timesheetIds))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("All selected timesheets are already invoiced");

        verify(clientService).getClientById(clientId);
        verify(timesheetService, times(2)).getTimesheetById(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenBuildingPreviewWithTimesheetsFromDifferentClients() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);

        ClientDto clientDto = new ClientDto(clientId, "Test Client", 50.0, 123, "Address", "City", "12345", "contact@test.com", true);

        TimesheetDto timesheet1 = new TimesheetDto(1L, "Test Client", LocalDate.now().minusDays(1), 2.0, false, clientId, 50.0, null, null);
        TimesheetDto timesheet2 = new TimesheetDto(2L, "Other Client", LocalDate.now().minusDays(2), 3.0, false, 999L, 50.0, null, null); // Different client

        when(clientService.getClientById(clientId)).thenReturn(clientDto);
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);

        // when/then
        assertThatThrownBy(() -> invoiceCreationService.buildInvoicePreview(clientId, issueDate, timesheetIds))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot create invoice: selected timesheets belong to different clients");

        verify(clientService).getClientById(clientId);
        verify(timesheetService, times(2)).getTimesheetById(anyLong());
    }
}
