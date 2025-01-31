package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.model.dto.TimesheetDtoMapper;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.model.entity.Timesheet;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetServiceImpl implements TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final ClientRepository clientRepository;
    private final TimesheetDtoMapper timesheetDtoMapper;

    @Override
    public List<TimesheetDto> getAllTimesheets() {
        return timesheetRepository.findAll().stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    public TimesheetDto getTimesheetById(Long id) {
        return timesheetDtoMapper.apply(getTimesheetOrThrow(id));
    }

    @Override
    public List<TimesheetDto> getUnbilledTimesheetsByClientId(Long clientId) {
        return timesheetRepository.findByClientIdAndInvoicedFalse(clientId)
                .stream()
                .map(timesheetDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimesheetDto> getTimesheetsByClientAndInvoiceStatus(Long clientId, boolean invoiced) {
        return timesheetRepository.findByClientIdAndInvoiced(clientId, invoiced).stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    @Transactional
    public TimesheetDto createTimesheet(Long clientId, LocalDate date, double duration) {
        Client client = clientRepository.getReferenceById(clientId);

        Timesheet timesheet = new Timesheet();
        timesheet.setClient(client);
        timesheet.setServiceDate(date);
        timesheet.setDuration(duration);
        timesheet.setInvoiced(false);
        return timesheetDtoMapper.apply(timesheetRepository.save(timesheet));
    }

    @Override
    @Transactional
    public TimesheetDto updateTimesheet(Long id, Long clientId, LocalDate date, double duration) {
        Timesheet timesheet = getTimesheetOrThrow(id);

        if (!timesheet.getClient().getId().equals(clientId)) {
            Client client = clientRepository.getReferenceById(clientId);
            timesheet.setClient(client);
        }

        timesheet.setServiceDate(date);
        timesheet.setDuration(duration);
        return timesheetDtoMapper.apply(timesheetRepository.save(timesheet));
    }

    @Override
    @Transactional
    public void deleteTimesheet(Long id) {
        Timesheet timesheet = getTimesheetOrThrow(id);
        log.debug("Deleting timesheet ID: {}", id);

        if (timesheet.isInvoiced() && timesheet.getInvoice() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete timesheet that is attached to an invoice"
            );
        }

        timesheet.setClient(null);
        timesheetRepository.save(timesheet);
        timesheetRepository.delete(timesheet);
    }

    @Override
    public List<TimesheetDto> getTimesheetByClientId(Long clientId) {
        return timesheetRepository.findAllByClientId(clientId).stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    public List<TimesheetDto> getUnbilledTimesheets() {
        return timesheetRepository.findByInvoiced(false)
                .stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    public List<TimesheetDto> getMonthlyTimesheets(Long clientId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return timesheetRepository.findByClient_IdAndServiceDateBetween(clientId, startDate, endDate)
                .stream()
                .map(timesheetDtoMapper)
                .sorted(Comparator.comparing(TimesheetDto::serviceDate))
                .collect(toList());
    }

    private Timesheet getTimesheetOrThrow(Long id) {
        return timesheetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timesheet with id " + id + "not found"));
    }

    @Override
    public void markAsInvoiced(Long id) {
        Timesheet timesheet = getTimesheetOrThrow(id);
        timesheet.setInvoiced(true);
        timesheetRepository.save(timesheet);
    }

    @Transactional
    @Override
    public void updateInvoiceFlag(Long id, boolean isInvoiced) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Timesheet with id: " + id + " not found"));

        timesheet.setInvoiced(isInvoiced);
        timesheetRepository.save(timesheet);
    }

    @Override
    @Transactional
    public void detachFromInvoice(Long id) {
        Timesheet timesheet = getTimesheetOrThrow(id);
        timesheet.setInvoiced(false);
        timesheet.setInvoice(null);
        timesheetRepository.save(timesheet);
    }
}
