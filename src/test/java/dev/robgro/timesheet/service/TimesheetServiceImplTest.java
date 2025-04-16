package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.EntityNotFoundException;
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

import java.time.LocalDate;
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
    void shouldThrowExceptionWhenTimesheetNotFound() {
        // given
        Long timesheetId = 1L;

        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> timesheetService.getTimesheetById(timesheetId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Timesheet with id 1 not found");
    }

//    @Test
//    void shouldThrowExceptionWhenTimesheetNotFound() {
//        // given
//        Long timesheetId = 1L;
//
//        when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());
//
//        // when/then
//        assertThatThrownBy(() -> timesheetService.getTimesheetById(timesheetId))
//                .isInstanceOf(EntityNotFoundException.class)
//                .hasMessageContaining("Timesheet with id " + timesheetId + " not found");
//    }

//    @Test
//    void shouldThrowExceptionWhenClientNotFound() {
//        // given
//        Long clientId = 1L;
//
//        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());
//
//        // when/then
//        assertThatThrownBy(() -> clientService.getClientOrThrow(clientId))
//                .isInstanceOf(EntityNotFoundException.class)
//                .hasMessageContaining("Client 1");
//    }
}
