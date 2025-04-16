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
import dev.robgro.timesheet.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    @Transactional(readOnly = true)
    public List<TimesheetDto> getAllTimesheets() {
        return timesheetRepository.findAll().stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TimesheetDto getTimesheetById(Long id) {
        return timesheetDtoMapper.apply(getTimesheetOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetDto> getUnbilledTimesheetsByClientId(Long clientId) {
//        return timesheetRepository.findByClientIdAndInvoicedFalse(clientId)
        return timesheetRepository.findUnbilledTimesheetsByClientId(clientId).stream()
                .map(timesheetDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<TimesheetDto> getTimesheetsByFilters(Long clientId, String paymentStatus) {
        List<Timesheet> timesheets;

        if (clientId != null && paymentStatus != null) {
            if (paymentStatus.equals("true")) {
                timesheets = timesheetRepository.findByClientIdAndPaymentDateIsNotNull(clientId);
            } else {
                timesheets = timesheetRepository.findByClientIdAndPaymentDateIsNull(clientId);
            }
        } else if (clientId != null) {
            timesheets = timesheetRepository.findAllByClientId(clientId);
        } else {
            timesheets = timesheetRepository.findAll();
        }

        return timesheets.stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetDto> searchAndSortTimesheets(Long clientId, String sortBy, String sortDir) {
        List<Timesheet> timesheets;
        if (clientId != null) {
            timesheets = timesheetRepository.findAllByClientId(clientId);
        } else {
            timesheets = timesheetRepository.findAll();
        }

        List<TimesheetDto> dtos = timesheets.stream()
                .map(timesheetDtoMapper)
                .collect(toList());

        return sortTimesheets(dtos, sortBy, sortDir);
    }

    private List<TimesheetDto> sortTimesheets(List<TimesheetDto> timesheets, String sortBy, String sortDir) {

        log.debug("Sorting timesheets by: {}, direction: {}", sortBy, sortDir);
        if (sortBy.equals("invoiceNumber")) {
            log.debug("First timesheet invoice number: {}",
                    timesheets.isEmpty() ? "none" : timesheets.get(0).invoiceNumber());
        }

        Comparator<TimesheetDto> comparator = switch (sortBy) {
            case "invoiceNumber" -> Comparator
                    .comparing((TimesheetDto i) -> {
                        String year = i.invoiceNumber() != null ?
                                i.invoiceNumber().substring(i.invoiceNumber().length() - 4) : "";
                        log.debug("Comparing year: {} for invoice {}", year, i.invoiceNumber());
                        return year;
                    })
                    .thenComparing(i -> {
                        String month = i.invoiceNumber() != null ?
                                i.invoiceNumber().substring(4, 6) : "";
                        log.debug("Comparing month: {} for invoice {}", month, i.invoiceNumber());
                        return month;
                    })
                    .thenComparing(i -> {
                        String number = i.invoiceNumber() != null ?
                                i.invoiceNumber().substring(0, 3) : "";
                        log.debug("Comparing number: {} for invoice {}", number, i.invoiceNumber());
                        return number;
                    });
            case "serviceDate" -> Comparator.comparing(TimesheetDto::serviceDate);
            case "duration" -> Comparator.comparing(TimesheetDto::duration);
            default -> Comparator.comparing(TimesheetDto::serviceDate);
        };

        return timesheets.stream()
                .sorted(sortDir.equals("desc") ? comparator.reversed() : comparator)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetDto> getFilteredAndPaginatedTimesheets(
            Long clientId,
            String paymentStatus,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        sortBy = (sortBy == null || sortBy.isEmpty()) ? "serviceDate" : sortBy;
        sortDir = (sortDir == null || sortDir.isEmpty()) ? "desc" : sortDir;

        if (paymentStatus != null && paymentStatus.isEmpty()) {
            paymentStatus = null;
        }

        log.debug("Retrieving paginated timesheets with params: clientId={}, paymentStatus={}, sortBy={}, sortDir={}, page={}, size={}",
                clientId, paymentStatus, sortBy, sortDir, page, size);

        Pageable pageable = PaginationUtils.createPageable(sortBy, sortDir, page, size);

        Page<Timesheet> timesheetsPage = timesheetRepository.findByClientIdAndPaymentStatus(clientId, paymentStatus, pageable);

        log.debug("Found {} timesheets in page {} of {}",
                timesheetsPage.getNumberOfElements(),
                timesheetsPage.getNumber(),
                timesheetsPage.getTotalPages());

        return timesheetsPage.map(timesheetDtoMapper);
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

    @Transactional
    @Override
    public void deleteTimesheet(Long id) {
        Timesheet timesheet = getTimesheetOrThrow(id);

        if (timesheet.isInvoiced()) {
            throw new BusinessRuleViolationException("Cannot delete timesheet that is attached to an invoice");
        }

        if (timesheet.getInvoice() != null) {
            Invoice invoice = timesheet.getInvoice();
            invoice.getItemsList().removeIf(item -> item.getTimesheetId().equals(timesheet.getId()));
            invoice.setTotalAmount(calculateTotalAmount(invoice.getItemsList()));
            timesheet.setInvoice(null);
            timesheet.setInvoiced(false);
        }
        timesheetRepository.delete(timesheet);
    }

    private BigDecimal calculateTotalAmount(List<InvoiceItem> items) {
        return items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetDto> getTimesheetByClientId(Long clientId) {
        return timesheetRepository.findAllByClientId(clientId).stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetDto> getUnbilledTimesheets() {
        return timesheetRepository.findByInvoiced(false)
                .stream()
                .map(timesheetDtoMapper)
                .collect(toList());
    }

    @Override
    @Transactional(readOnly = true)
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
                .orElseThrow(() -> new EntityNotFoundException("Timesheet", id));
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
        Timesheet timesheet = getTimesheetOrThrow(id);
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

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetDto> getAllTimesheetsPageable(Pageable pageable) {
        return timesheetRepository.findAll(pageable)
                .map(timesheetDtoMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetDto> getTimesheetsByClientIdPageable(Long clientId, Pageable pageable) {
        return timesheetRepository.findAllByClientId(clientId, pageable)
                .map(timesheetDtoMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetDto> getAllTimesheetsSortedByInvoiceNumber(Long clientId, Pageable pageable) {
        return timesheetRepository.findAllSortedByInvoiceNumber(clientId, pageable)
                .map(timesheetDtoMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetDto> getAllTimesheetsPageable(Long clientId, Pageable pageable) {
        if (clientId != null) {
            return timesheetRepository.findAllByClientId(clientId, pageable)
                    .map(timesheetDtoMapper);
        }
        return timesheetRepository.findAll(pageable)
                .map(timesheetDtoMapper);
    }

    @Override
    @Transactional
    public void updatePaymentDate(Long id, LocalDate paymentDate) {
        Timesheet timesheet = getTimesheetOrThrow(id);
        timesheet.setPaymentDate(paymentDate);
        timesheetRepository.save(timesheet);
    }

    @Override
    @Transactional(readOnly = true)
    public TimesheetDto createEmptyTimesheetDto() {
        return new TimesheetDto(null, null, null, 0.5, false, null, 0.0, null, null);
    }
}
