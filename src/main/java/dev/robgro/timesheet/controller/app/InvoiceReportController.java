package dev.robgro.timesheet.controller.app;

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
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {

        log.debug("Generating report for: clientId={}, year={}, month={}", clientId, year, month);

        List<InvoiceDto> invoices = invoiceService.searchInvoices(clientId, year, month);
        log.debug("Found {} invoices", invoices.size());

        invoices.sort(Comparator.comparing(InvoiceDto::issueDate));

        BigDecimal totalAmount = invoices.stream()
                .map(InvoiceDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String monthName = month != null ? Month.of(month).toString() : "all months";
        String yearStr = year != null ? year.toString() : "";
        model.addAttribute("period", month != null ? monthName + " " + yearStr : monthName);
        model.addAttribute("invoices", invoices);
        model.addAttribute("totalAmount", totalAmount);

        if (clientId != null) {
            model.addAttribute("clientName", clientService.getClientById(clientId).clientName());
        }
        return "invoice-report";
    }
}
