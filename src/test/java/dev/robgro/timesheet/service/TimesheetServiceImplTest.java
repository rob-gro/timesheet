package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.model.dto.TimesheetDtoMapper;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.model.entity.Timesheet;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.TimesheetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceImplTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TimesheetDtoMapper timesheetDtoMapper;

    @InjectMocks
    private TimesheetServiceImpl timesheetService;

    @Test
    void shouldCreateTimesheet() {
        // given
        Long clientId = 1L;
        LocalDate serviceDate = LocalDate.now();
        double duration = 2.5;

        Client client = new Client();
        client.setId(clientId);
        client.setClientName("Test Client");
        client.setHourlyRate(50.0);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setClient(client);
        timesheet.setServiceDate(serviceDate);
        timesheet.setDuration(duration);
        timesheet.setInvoice(false);

        TimesheetDto expectedDto = new TimesheetDto(
                1L,
                client.getClientName(),
                serviceDate,
                duration,
                false,
                clientId,
                client.getHourlyRate(),
                null
        );

        when(clientRepository.getReferenceById(clientId)).thenReturn(client);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(timesheet);
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(expectedDto);

        // when
        TimesheetDto result = timesheetService.createTimesheet(clientId, serviceDate, duration);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.clientId()).isEqualTo(clientId);
        assertThat(result.duration()).isEqualTo(duration);
        assertThat(result.serviceDate()).isEqualTo(serviceDate);
        assertThat(result.isInvoice()).isFalse();
    }

    @Test
    void shouldGetMonthlyTimesheets() {
        // given
        Long clientId = 1L;
        int year = 2024;
        int month = 1;
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        Client client = new Client();
        client.setId(clientId);
        client.setClientName("Test Client");
        client.setHourlyRate(50.0);

        Timesheet timesheet1 = new Timesheet();
        timesheet1.setId(1L);
        timesheet1.setClient(client);
        timesheet1.setServiceDate(LocalDate.of(2024, 1, 15));
        timesheet1.setDuration(2.0);

        Timesheet timesheet2 = new Timesheet();
        timesheet2.setId(2L);
        timesheet2.setClient(client);
        timesheet2.setServiceDate(LocalDate.of(2024, 1, 16));
        timesheet2.setDuration(3.0);

        TimesheetDto dto1 = new TimesheetDto(1L, client.getClientName(), timesheet1.getServiceDate(),
                2.0, false, clientId, client.getHourlyRate(), null);
        TimesheetDto dto2 = new TimesheetDto(2L, client.getClientName(), timesheet2.getServiceDate(),
                3.0, false, clientId, client.getHourlyRate(), null);

        when(timesheetRepository.findByClient_IdAndServiceDateBetween(clientId, startDate, endDate))
                .thenReturn(List.of(timesheet1, timesheet2));
        when(timesheetDtoMapper.apply(timesheet1)).thenReturn(dto1);
        when(timesheetDtoMapper.apply(timesheet2)).thenReturn(dto2);

        // when
        List<TimesheetDto> result = timesheetService.getMonthlyTimesheets(clientId, year, month);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);
    }

    @Test
    void shouldMarkAsInvoiced() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoice(false);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.save(timesheet)).thenReturn(timesheet);

        // when
        timesheetService.markAsInvoiced(timesheetId);

        // then
        verify(timesheetRepository).findById(timesheetId);
        verify(timesheetRepository).save(timesheet);
        assertThat(timesheet.isInvoice()).isTrue();
    }

    @Test
    void shouldNotDeleteInvoicedTimesheet() {
        // given
        Long timesheetId = 1L;
        Timesheet timesheet = new Timesheet();
        timesheet.setId(timesheetId);
        timesheet.setInvoice(true);

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

        // when/then
        assertThatThrownBy(() -> timesheetService.deleteTimesheet(timesheetId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot delete timesheet that is attached to an invoice");

        verify(timesheetRepository, never()).delete(any());
    }

    @Test
    void shouldGetUnbilledTimesheets() {
        // given
        Long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        client.setClientName("Test Client");
        client.setHourlyRate(50.0);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setClient(client);
        timesheet.setServiceDate(LocalDate.now());
        timesheet.setDuration(2.0);
        timesheet.setInvoice(false);

        TimesheetDto expectedDto = new TimesheetDto(
                1L,
                client.getClientName(),
                timesheet.getServiceDate(),
                2.0,
                false,
                clientId,
                client.getHourlyRate(),
                null
        );

        when(timesheetRepository.findByIsInvoice(false)).thenReturn(List.of(timesheet));
        when(timesheetDtoMapper.apply(timesheet)).thenReturn(expectedDto);

        // when
        List<TimesheetDto> result = timesheetService.getUnbilledTimesheets();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(timesheet.getId());
        assertThat(result.get(0).isInvoice()).isFalse();
    }
}
