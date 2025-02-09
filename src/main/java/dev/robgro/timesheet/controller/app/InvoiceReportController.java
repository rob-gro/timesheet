package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.DateRangeRequest;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/invoice-reports")
@RequiredArgsConstructor
public class InvoiceReportController {

    private final InvoiceService invoiceService;
    private final ClientService clientService;

    @GetMapping("/generate")
    public String generateInvoiceReport(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromMonth,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toMonth,
            Model model) {

        log.debug("Generating report for: clientId={}, from year={}, from month={}, to year{}, to month{}", clientId, fromYear, fromMonth, toYear, toMonth);

        DateRangeRequest dateRange = new DateRangeRequest(fromYear, fromMonth, toYear, toMonth);
        List<InvoiceDto> invoices = invoiceService.searchInvoices(dateRange, clientId, null).getContent();

        List<InvoiceDto> sortableInvoices = new ArrayList<>(invoices);
        sortableInvoices.sort(Comparator.comparing(InvoiceDto::issueDate));

        BigDecimal totalAmount = invoices.stream()
                .map(InvoiceDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String period = generatePeriodLabel(fromYear, fromMonth, toYear, toMonth);
        model.addAttribute("period", period);
        model.addAttribute("invoices", sortableInvoices);
        model.addAttribute("totalAmount", totalAmount);

        if (clientId != null) {
            model.addAttribute("clientName", clientService.getClientById(clientId).clientName());
        }
        return "invoice-report";
    }

    private String generatePeriodLabel(Integer fromYear, Integer fromMonth, Integer toYear, Integer toMonth) {
        if (fromYear == null || fromMonth == null || toYear == null || toMonth == null) {
            return "all dates";
        }

        return Month.of(fromMonth) + " " + fromYear + " - " + Month.of(toMonth) + " " + toYear;
    }
}
