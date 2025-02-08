package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.DateRangeRequest;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("invoice-archive")
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
            @RequestParam(required = false, defaultValue = "invoiceNumber") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        DateRangeRequest dateRange = new DateRangeRequest(fromYear, fromMonth, toYear, toMonth);

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<InvoiceDto> invoicesPage = invoiceService.searchInvoices(dateRange, clientId, pageable);

        model.addAttribute("invoices", invoicesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", invoicesPage.getTotalPages());
        model.addAttribute("totalItems", invoicesPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("clients", clientService.getAllClients());

        return "invoice-archive";
    }
}
