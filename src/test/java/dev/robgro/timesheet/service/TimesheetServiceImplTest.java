package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.model.dto.TimesheetDtoMapper;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.model.entity.InvoiceItem;
import dev.robgro.timesheet.model.entity.Timesheet;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.TimesheetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceImplTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TimesheetDtoMapper timesheetDtoMapper;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private TimesheetServiceImpl timesheetService;

    // ----- Basic CRUD operations -----

    @Test
    void shouldGetAllTimesheets() {
        // given
        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);

        TimesheetDto dto = new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, 1L, 50.0, null, null);

        when(timesheetRepository.findAll()).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(dto);

        // when
        List<TimesheetDto> result = timesheetService.getAllTimesheets();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        verify(timesheetRepository).findAll();
    }

    @Test
    void shouldGetTimesheetById() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);

        TimesheetDto dto = new TimesheetDto(timesheetId, "Client", LocalDate.now(), 2.0, false, 1L, 50.0, null, null);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(dto);

        // when
        TimesheetDto result = timesheetService.getTimesheetById(timesheetId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(timesheetId);
        verify(timesheetRepository).findById(timesheetId);
    }

    @Test
    void shouldGetMonthlyTimesheets() {
        Long clientId = 1L;
        int year = 2023;
        int month = 4;

        Timesheet timesheet = new Timesheet();
        timesheet.setServiceDate(LocalDate.of(year, month, 15));

        when(timesheetRepository.findByClient_IdAndServiceDateBetween(eq(clientId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(timesheet));

        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", timesheet.getServiceDate(), 2.0, false, clientId, 50.0, null, null));

        List<TimesheetDto> result = timesheetService.getMonthlyTimesheets(clientId, year, month);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).serviceDate()).isEqualTo(LocalDate.of(year, month, 15));
        verify(timesheetRepository).findByClient_IdAndServiceDateBetween(eq(clientId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void shouldCreateTimesheet() {
        // given
        Long clientId = 1L;
        LocalDate serviceDate = LocalDate.now();
        double duration = 2.5;

        Client client = new Client();
        client.setId(clientId);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);

        TimesheetDto dto = new TimesheetDto(1L, "Client", serviceDate, duration, false, clientId, 50.0, null, null);

        when(clientRepository.getReferenceById(clientId)).thenReturn(client);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(timesheet);
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(dto);

        // when
        TimesheetDto result = timesheetService.createTimesheet(clientId, serviceDate, duration);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(clientRepository).getReferenceById(clientId);
        verify(timesheetRepository).save(any(Timesheet.class));
    }

    @Test
    void shouldUpdateTimesheet() {
        // given
        Long timesheetId = 1L;
        Long clientId = 1L;
        LocalDate date = LocalDate.now();
        double duration = 2.5;

        Client client = new Client();
        client.setId(clientId);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setClient(client);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.save(timesheet)).thenReturn(timesheet);
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(timesheetId, "Client", date, duration, false, clientId, 50.0, null, null));

        // when
        TimesheetDto result = timesheetService.updateTimesheet(timesheetId, clientId, date, duration);

        // then
        assertThat(result.id()).isEqualTo(timesheetId);
        assertThat(result.serviceDate()).isEqualTo(date);
        assertThat(result.duration()).isEqualTo(duration);
        verify(timesheetRepository).findById(timesheetId);
        verify(timesheetRepository).save(timesheet);
    }

    @Test
    void shouldUpdateTimesheetWithoutChangingClient() {
        Long timesheetId = 1L;
        Long clientId = 1L;
        LocalDate date = LocalDate.now();
        double duration = 3.0;

        Client client = new Client();
        client.setId(clientId);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setClient(client);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.save(timesheet)).thenReturn(timesheet);
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(timesheetId, "Client", date, duration, false, clientId, 50.0, null, null));

        TimesheetDto result = timesheetService.updateTimesheet(timesheetId, clientId, date, duration);

        assertThat(result.id()).isEqualTo(timesheetId);
        verify(clientRepository, never()).getReferenceById(anyLong());
        verify(timesheetRepository).save(timesheet);
    }

    @Test
    void shouldUpdateTimesheetWithDifferentClient() {
        // given
        Long timesheetId = 1L;
        Long oldClientId = 1L;
        Long newClientId = 2L;
        LocalDate date = LocalDate.now();
        double duration = 2.5;

        Client oldClient = new Client();
        oldClient.setId(oldClientId);

        Client newClient = new Client();
        newClient.setId(newClientId);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setClient(oldClient);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));
        when(clientRepository.getReferenceById(newClientId)).thenReturn(newClient);
        when(timesheetRepository.save(timesheet)).thenReturn(timesheet);
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(timesheetId, "Client", date, duration, false, newClientId, 50.0, null, null));

        // when
        TimesheetDto result = timesheetService.updateTimesheet(timesheetId, newClientId, date, duration);

        // then
        assertThat(result.id()).isEqualTo(timesheetId);
        assertThat(result.clientId()).isEqualTo(newClientId);
        verify(timesheetRepository).findById(timesheetId);
        verify(clientRepository).getReferenceById(newClientId);
        verify(timesheetRepository).save(timesheet);
    }

    @Test
    void shouldDeleteTimesheet() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoiced(false);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when
        timesheetService.deleteTimesheet(timesheetId);

        // then
        verify(timesheetRepository).delete(timesheet);
    }

    @Test
    void shouldDeleteTimesheetWithInvoice() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoiced(false);

        Invoice invoice = new Invoice();
        List<InvoiceItem> itemsList = new ArrayList<>();
        InvoiceItem item = new InvoiceItem();
        item.setTimesheetId(timesheetId);
        itemsList.add(item);
        invoice.setItemsList(itemsList);

        timesheet.setInvoice(invoice);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when
        timesheetService.deleteTimesheet(timesheetId);

        // then
        assertThat(invoice.getItemsList()).isEmpty();
        assertThat(timesheet.getInvoice()).isNull();
        assertThat(timesheet.isInvoiced()).isFalse();
        verify(timesheetRepository).findById(timesheetId);
        verify(timesheetRepository).delete(timesheet);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentTimesheet() {
        Long timesheetId = 99L;
        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.deleteTimesheet(timesheetId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Timesheet");
    }

    // ----- Filtering and listing operations -----

    @Test
    void shouldGetTimesheetByClientId() {
        // given
        Long clientId = 1L;
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findAllByClientId(clientId)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.getTimesheetByClientId(clientId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).clientId()).isEqualTo(clientId);
        verify(timesheetRepository).findAllByClientId(clientId);
    }

    @Test
    void shouldGetUnbilledTimesheetsByClientId() {
        // given
        Long clientId = 1L;
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findUnbilledTimesheetsByClientId(clientId)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.getUnbilledTimesheetsByClientId(clientId);

        // then
        assertThat(result).hasSize(1);
        verify(timesheetRepository).findUnbilledTimesheetsByClientId(clientId);
    }

    @Test
    void shouldGetTimesheetsByClientAndInvoiceStatus() {
        // given
        Long clientId = 1L;
        boolean invoiced = false;
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findByClientIdAndInvoiced(clientId, invoiced)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.getTimesheetsByClientAndInvoiceStatus(clientId, invoiced);

        // then
        assertThat(result).hasSize(1);
        verify(timesheetRepository).findByClientIdAndInvoiced(clientId, invoiced);
    }

    @Test
    void shouldGetTimesheetsByFiltersWithClientIdAndPaid() {
        // given
        Long clientId = 1L;
        String paymentStatus = "true";
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findByClientIdAndPaymentDateIsNotNull(clientId)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, LocalDate.now()));

        // when
        List<TimesheetDto> result = timesheetService.getTimesheetsByFilters(clientId, paymentStatus);

        // then
        assertThat(result).hasSize(1);
        verify(timesheetRepository).findByClientIdAndPaymentDateIsNotNull(clientId);
    }

    @Test
    void shouldGetTimesheetsByFiltersWithClientIdAndUnpaid() {
        // given
        Long clientId = 1L;
        String paymentStatus = "false";
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findByClientIdAndPaymentDateIsNull(clientId)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.getTimesheetsByFilters(clientId, paymentStatus);

        // then
        assertThat(result).hasSize(1);
        verify(timesheetRepository).findByClientIdAndPaymentDateIsNull(clientId);
    }

    @Test
    void shouldGetTimesheetsByFiltersWithOnlyClientId() {
        // given
        Long clientId = 1L;
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findAllByClientId(clientId)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.getTimesheetsByFilters(clientId, null);

        // then
        assertThat(result).hasSize(1);
        verify(timesheetRepository).findAllByClientId(clientId);
    }

    @Test
    void shouldGetTimesheetsByFiltersWithNoParams() {
        // given
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findAll()).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, 1L, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.getTimesheetsByFilters(null, null);

        // then
        assertThat(result).hasSize(1);
        verify(timesheetRepository).findAll();
    }

    @Test
    void shouldGetUnbilledTimesheets() {
        // given
        Timesheet timesheet = new Timesheet();

        when(timesheetRepository.findByInvoiced(false)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, 1L, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.getUnbilledTimesheets();

        // then
        assertThat(result).hasSize(1);
        verify(timesheetRepository).findByInvoiced(false);
    }

    // ----- Sorting and pagination operations -----

    @Test
    void shouldSearchAndSortTimesheetsByInvoiceNumber() {
        // given
        Long clientId = 1L;
        String sortBy = "invoiceNumber";
        String sortDir = "asc";

        String invoice1 = "001-04-2023";
        String invoice2 = "002-05-2023";

        Timesheet timesheet1 = new Timesheet();
        timesheet1.setId(1L);
        timesheet1.setInvoiceNumber(invoice1);

        Timesheet timesheet2 = new Timesheet();
        timesheet2.setId(2L);
        timesheet2.setInvoiceNumber(invoice2);

        when(timesheetRepository.findAllByClientId(clientId))
                .thenReturn(List.of(timesheet2, timesheet1));

        when(timesheetDtoMapper.apply(timesheet1)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, true, clientId, 50.0, invoice1, null));
        when(timesheetDtoMapper.apply(timesheet2)).thenReturn(
                new TimesheetDto(2L, "Client", LocalDate.now(), 3.0, true, clientId, 50.0, invoice2, null));

        // when
        List<TimesheetDto> result = timesheetService.searchAndSortTimesheets(clientId, sortBy, sortDir);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).invoiceNumber()).isEqualTo(invoice1);
        assertThat(result.get(1).invoiceNumber()).isEqualTo(invoice2);
        verify(timesheetRepository).findAllByClientId(clientId);
    }

    @Test
    void shouldSortTimesheetsWhenInvoiceNumberIsNull() {
        Long clientId = 1L;
        String sortBy = "invoiceNumber";
        String sortDir = "asc";

        Timesheet timesheet1 = new Timesheet();
        timesheet1.setId(1L);
        timesheet1.setInvoiceNumber(null); // null faktura

        Timesheet timesheet2 = new Timesheet();
        timesheet2.setId(2L);
        timesheet2.setInvoiceNumber("001-01-2023");

        when(timesheetRepository.findAllByClientId(clientId))
                .thenReturn(List.of(timesheet1, timesheet2));

        when(timesheetDtoMapper.apply(timesheet1))
                .thenReturn(new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));
        when(timesheetDtoMapper.apply(timesheet2))
                .thenReturn(new TimesheetDto(2L, "Client", LocalDate.now(), 3.0, false, clientId, 50.0, "001-01-2023", null));

        List<TimesheetDto> result = timesheetService.searchAndSortTimesheets(clientId, sortBy, sortDir);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).invoiceNumber()).isNull();
        assertThat(result.get(1).invoiceNumber()).isEqualTo("001-01-2023");
    }


    @Test
    void shouldSearchAndSortTimesheetsByServiceDate() {
        // given
        Long clientId = 1L;
        String sortBy = "serviceDate";
        String sortDir = "desc";

        Timesheet timesheet1 = new Timesheet();
        timesheet1.setServiceDate(LocalDate.of(2023, 1, 1));

        Timesheet timesheet2 = new Timesheet();
        timesheet2.setServiceDate(LocalDate.of(2023, 2, 1));

        when(timesheetRepository.findAllByClientId(clientId)).thenReturn(List.of(timesheet1, timesheet2));
        when(timesheetDtoMapper.apply(timesheet1)).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.of(2023, 1, 1), 2.0, false, clientId, 50.0, null, null));
        when(timesheetDtoMapper.apply(timesheet2)).thenReturn(
                new TimesheetDto(2L, "Client", LocalDate.of(2023, 2, 1), 3.0, false, clientId, 50.0, null, null));

        // when
        List<TimesheetDto> result = timesheetService.searchAndSortTimesheets(clientId, sortBy, sortDir);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).serviceDate()).isEqualTo(LocalDate.of(2023, 2, 1));
        assertThat(result.get(1).serviceDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        verify(timesheetRepository).findAllByClientId(clientId);
    }

    @Test
    void shouldSortTimesheetsByDefaultWhenSortByUnknown() {
        Long clientId = 1L;

        Timesheet timesheet1 = new Timesheet();
        timesheet1.setServiceDate(LocalDate.of(2023, 5, 15));
        Timesheet timesheet2 = new Timesheet();
        timesheet2.setServiceDate(LocalDate.of(2023, 4, 15));

        when(timesheetRepository.findAllByClientId(clientId)).thenReturn(List.of(timesheet1, timesheet2));
        when(timesheetDtoMapper.apply(any())).thenAnswer(invocation -> {
            Timesheet ts = invocation.getArgument(0);
            return new TimesheetDto(1L, "Client", ts.getServiceDate(), 2.0, false, clientId, 50.0, null, null);
        });

        List<TimesheetDto> result = timesheetService.searchAndSortTimesheets(clientId, "unknownField", "asc");

        assertThat(result.get(0).serviceDate()).isEqualTo(LocalDate.of(2023, 4, 15));
    }

    @Test
    void shouldReturnEmptyListWhenNoTimesheetsForSorting() {
        Long clientId = 1L;
        String sortBy = "invoiceNumber";
        String sortDir = "asc";

        when(timesheetRepository.findAllByClientId(clientId)).thenReturn(List.of());

        List<TimesheetDto> result = timesheetService.searchAndSortTimesheets(clientId, sortBy, sortDir);

        assertThat(result).isEmpty();
        verify(timesheetRepository).findAllByClientId(clientId);
    }

    @Test
    void shouldGetFilteredAndPaginatedTimesheets() {
        // given
        Long clientId = 1L;
        String paymentStatus = "false";
        String sortBy = "serviceDate";
        String sortDir = "desc";
        int page = 0;
        int size = 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        Page<Timesheet> timesheetPage = new PageImpl<>(
                List.of(new Timesheet()),
                pageable,
                1
        );

        when(timesheetRepository.findByClientIdAndPaymentStatus(eq(clientId), eq(paymentStatus), any(Pageable.class)))
                .thenReturn(timesheetPage);
        when(timesheetDtoMapper.apply(any(Timesheet.class)))
                .thenReturn(new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));

        // when
        Page<TimesheetDto> result = timesheetService.getFilteredAndPaginatedTimesheets(
                clientId, paymentStatus, sortBy, sortDir, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(timesheetRepository).findByClientIdAndPaymentStatus(eq(clientId), eq(paymentStatus), any(Pageable.class));
    }

    @Test
    void shouldHandleEmptyPaymentStatus() {
        Long clientId = 1L;
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "serviceDate");
        Page<Timesheet> timesheetPage = new PageImpl<>(List.of(new Timesheet()), pageable, 1);

        when(timesheetRepository.findByClientIdAndPaymentStatus(eq(clientId), isNull(), eq(pageable)))
                .thenReturn(timesheetPage);

        when(timesheetDtoMapper.apply(any())).thenReturn(
                new TimesheetDto(1L, "Client", LocalDate.now(), 2.0, false, clientId, 50.0, null, null));

        Page<TimesheetDto> result = timesheetService.getFilteredAndPaginatedTimesheets(clientId, "", "serviceDate", "desc", 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(timesheetRepository).findByClientIdAndPaymentStatus(eq(clientId), isNull(), eq(pageable));
    }

    @Test
    void shouldGetAllTimesheetsPageable() {
        // given
        Pageable pageable = mock(Pageable.class);
        Page<Timesheet> timesheetPage = mock(Page.class);

        when(timesheetRepository.findAll(pageable)).thenReturn(timesheetPage);
        when(timesheetPage.map(any())).thenReturn(Page.empty());

        // when
        Page<TimesheetDto> result = timesheetService.getAllTimesheetsPageable(pageable);

        // then
        assertThat(result).isNotNull();
        verify(timesheetRepository).findAll(pageable);
    }

    @Test
    void shouldGetTimesheetsByClientIdPageable() {
        // given
        Long clientId = 1L;
        Pageable pageable = mock(Pageable.class);
        Page<Timesheet> timesheetPage = mock(Page.class);

        when(timesheetRepository.findAllByClientId(clientId, pageable)).thenReturn(timesheetPage);
        when(timesheetPage.map(any())).thenReturn(Page.empty());

        // when
        Page<TimesheetDto> result = timesheetService.getTimesheetsByClientIdPageable(clientId, pageable);

        // then
        assertThat(result).isNotNull();
        verify(timesheetRepository).findAllByClientId(clientId, pageable);
    }

    @Test
    void shouldGetAllTimesheetsSortedByInvoiceNumber() {
        // given
        Long clientId = 1L;
        Pageable pageable = mock(Pageable.class);
        Page<Timesheet> timesheetPage = mock(Page.class);

        when(timesheetRepository.findAllSortedByInvoiceNumber(clientId, pageable)).thenReturn(timesheetPage);
        when(timesheetPage.map(any())).thenReturn(Page.empty());

        // when
        Page<TimesheetDto> result = timesheetService.getAllTimesheetsSortedByInvoiceNumber(clientId, pageable);

        // then
        assertThat(result).isNotNull();
        verify(timesheetRepository).findAllSortedByInvoiceNumber(clientId, pageable);
    }

    @Test
    void shouldGetAllTimesheetsPageableWithClientId() {
        // given
        Long clientId = 1L;
        Pageable pageable = mock(Pageable.class);
        Page<Timesheet> timesheetPage = mock(Page.class);

        when(timesheetRepository.findAllByClientId(clientId, pageable)).thenReturn(timesheetPage);
        when(timesheetPage.map(any())).thenReturn(Page.empty());

        // when
        Page<TimesheetDto> result = timesheetService.getAllTimesheetsPageable(clientId, pageable);

        // then
        assertThat(result).isNotNull();
        verify(timesheetRepository).findAllByClientId(clientId, pageable);
    }

    @Test
    void shouldGetAllTimesheetsPageableWithoutClientId() {
        // given
        Pageable pageable = mock(Pageable.class);
        Page<Timesheet> timesheetPage = mock(Page.class);

        when(timesheetRepository.findAll(pageable)).thenReturn(timesheetPage);
        when(timesheetPage.map(any())).thenReturn(Page.empty());

        // when
        Page<TimesheetDto> result = timesheetService.getAllTimesheetsPageable(null, pageable);

        // then
        assertThat(result).isNotNull();
        verify(timesheetRepository).findAll(pageable);
    }

    // ----- Invoice operations -----

    @Test
    void shouldNotDeleteInvoicedTimesheet() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoiced(true);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when/then
        assertThatThrownBy(() -> timesheetService.deleteTimesheet(timesheetId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot delete timesheet that is attached to an invoice");

        verify(timesheetRepository, never()).delete(any());
    }

    @Test
    void shouldMarkAsInvoiced() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoiced(false);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when
        timesheetService.markAsInvoiced(timesheetId);

        // then
        assertThat(timesheet.isInvoiced()).isTrue();
        verify(timesheetRepository).findById(timesheetId);
        verify(timesheetRepository).save(timesheet);
    }

    @Test
    void shouldUpdateInvoiceFlag() {
        // given
        Long timesheetId = 1L;
        boolean isInvoiced = true;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoiced(false);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when
        timesheetService.updateInvoiceFlag(timesheetId, isInvoiced);

        // then
        assertThat(timesheet.isInvoiced()).isEqualTo(isInvoiced);
        verify(timesheetRepository).findById(timesheetId);
        verify(timesheetRepository).save(timesheet);
    }

    @Test
    void shouldDetachFromInvoice() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoiced(true);

        Invoice invoice = new Invoice();
        timesheet.setInvoice(invoice);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when
        timesheetService.detachFromInvoice(timesheetId);

        // then
        assertThat(timesheet.isInvoiced()).isFalse();
        assertThat(timesheet.getInvoice()).isNull();
        verify(timesheetRepository).findById(timesheetId);
        verify(timesheetRepository).save(timesheet);
    }

    // ----- Payment operations -----

    @Test
    void shouldUpdatePaymentDate() {
        // given
        Long timesheetId = 1L;
        LocalDate paymentDate = LocalDate.now();
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when
        timesheetService.updatePaymentDate(timesheetId, paymentDate);

        // then
        assertThat(timesheet.getPaymentDate()).isEqualTo(paymentDate);
        verify(timesheetRepository).findById(timesheetId);
        verify(timesheetRepository).save(timesheet);
    }

    // ----- Utility methods -----

    @Test
    void shouldCreateEmptyTimesheetDto() {
        // when
        TimesheetDto result = timesheetService.createEmptyTimesheetDto();

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isNull();
        assertThat(result.duration()).isEqualTo(0.5);
        assertThat(result.invoiced()).isFalse();
    }

    // ----- Exception tests -----

    @Test
    void shouldThrowExceptionWhenTimesheetNotFound() {
        // given
        Long timesheetId = 1L;

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> timesheetService.getTimesheetById(timesheetId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Timesheet with id 1 not found");
    }
}
