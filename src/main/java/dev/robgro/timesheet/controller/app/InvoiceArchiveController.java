package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "invoiceNumber") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {

        List<InvoiceDto> invoices = invoiceService.searchAndSortInvoices(clientId, year, month, sortBy, sortDir);
        model.addAttribute("invoices", invoices);
        model.addAttribute("clients", clientService.getAllClients());
        return "invoice-archive";
    }
}
