package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.DateRangeRequest;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import dev.robgro.timesheet.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("invoices/archive")
@RequiredArgsConstructor
public class InvoiceArchiveController {

    private final InvoiceService invoiceService;
    private final ClientService clientService;

    @GetMapping
    public String showArchive(
            Model model,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromMonth,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toMonth,
            @RequestParam(required = false, defaultValue = "issueDate") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Showing invoice archive with date range: from {}-{} to {}-{}, clientId={}",
                fromYear, fromMonth, toYear, toMonth, clientId);

        if ("clientName".equals(sortBy)) {
            sortBy = "client.clientName";
        }

        DateRangeRequest dateRange = new DateRangeRequest(fromYear, fromMonth, toYear, toMonth);
        Pageable pageable = PaginationUtils.createPageable(sortBy, sortDir, page, size);

        Page<InvoiceDto> invoicesPage = invoiceService.searchInvoices(dateRange, clientId, pageable);

        populateModel(model, invoicesPage, page, size, sortBy, sortDir);
        return "invoices/archive";
    }

    @GetMapping("/pdf")
    public String showPdfList(
            Model model,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "issueDate") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Showing PDF invoice list with filters: clientId={}, year={}, month={}, sortBy={}, sortDir={}, page={}, size={}",
                clientId, year, month, sortBy, sortDir, page, size);

        Pageable pageable = PaginationUtils.createPageable(sortBy, sortDir, page, size);
        Page<InvoiceDto> invoicesPage = invoiceService.getAllInvoicesPageable(clientId, year, month, pageable);

        populateModel(model, invoicesPage, page, size, sortBy, sortDir);
        model.addAttribute("clientId", clientId);
        model.addAttribute("year", year);
        model.addAttribute("month", month);

        return "invoices/pdf";
    }

    private void populateModel(Model model, Page<InvoiceDto> invoicesPage, int page, int size, String sortBy, String sortDir) {
        model.addAttribute("invoices", invoicesPage.getContent());
        PaginationUtils.setPaginationAttributesWithSort(model, invoicesPage, page, size, sortBy, sortDir);
        model.addAttribute("clients", clientService.getAllClients());
    }
}
