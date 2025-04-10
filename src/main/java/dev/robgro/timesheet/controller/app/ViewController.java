package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import dev.robgro.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final TimesheetService timesheetService;
    private final ClientService clientService;
    private final InvoiceService invoiceService;
    private final InvoiceSeller invoiceSeller;

    @GetMapping("/")
    public String showIndex() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }
        log.debug("Authenticated user: {}", auth.getName());
        return "index";
    }

    @GetMapping("/timesheet")
    public String showTimesheet(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("timesheet", new TimesheetDto(null, null, null, 0.5, false, null, 0.0, null, null));
        return "timesheet";
    }

    @GetMapping("/timesheet-list")
    public String showTimesheetArchive(Model model) {
        model.addAttribute("timesheets", timesheetService.getAllTimesheets());
        return "timesheet-list";
    }

    @GetMapping("/invoice-list")
    public String showInvoices(Model model,
                               @RequestParam(required = false) Long clientId,
                               @RequestParam(required = false) Integer year,
                               @RequestParam(required = false) Integer month,
                               @RequestParam(required = false, defaultValue = "invoiceNumber") String sortBy,
                               @RequestParam(required = false, defaultValue = "asc") String sortDir) {

        List<InvoiceDto> invoices = invoiceService.searchAndSortInvoices(clientId, year, month, sortBy, sortDir);
        model.addAttribute("invoices", invoices);
        model.addAttribute("clients", clientService.getAllClients());
        return "invoice-pdf";
    }

    @GetMapping("/invoice-edit/{id}")
    public String showEditInvoiceForm(@PathVariable Long id, Model model) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        ClientDto client = clientService.getClientById(invoice.clientId());
        model.addAttribute("invoice", invoice);
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("seller", invoiceSeller);
        model.addAttribute("client", client);
        return "invoice-edit";
    }

    @GetMapping("/invoice-edit")
    public String showEditInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoice-edit";
    }
}
