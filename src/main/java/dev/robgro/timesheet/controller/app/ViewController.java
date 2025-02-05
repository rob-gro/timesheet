package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import dev.robgro.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final TimesheetService timesheetService;
    private final ClientService clientService;
    private final InvoiceService invoiceService;

    @GetMapping("/")
    public String showIndex() {
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

    @GetMapping("/invoice-edit")
    public String showEditInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoice-edit";
    }
}
