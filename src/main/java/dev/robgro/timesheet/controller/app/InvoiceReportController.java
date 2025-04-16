package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.DateRangeRequest;
import dev.robgro.timesheet.model.dto.InvoiceReportData;
import dev.robgro.timesheet.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/invoices/reports")
@RequiredArgsConstructor
public class InvoiceReportController {

    private final InvoiceService invoiceService;

    @GetMapping("/generate")
    public String generateInvoiceReport(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromMonth,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toMonth,
            Model model) {

        log.debug("Generating report for: clientId={}, from year={}, from month={}, to year={}, to month={}",
                clientId, fromYear, fromMonth, toYear, toMonth);

        DateRangeRequest dateRange = new DateRangeRequest(fromYear, fromMonth, toYear, toMonth);
        InvoiceReportData reportData = invoiceService.generateReport(dateRange, clientId);

        model.addAttribute("period", reportData.period());
        model.addAttribute("invoices", reportData.invoices());
        model.addAttribute("totalAmount", reportData.totalAmount());

        if (reportData.clientName() != null) {
            model.addAttribute("clientName", reportData.clientName());
        }

        return "invoices/reports";
    }
}
