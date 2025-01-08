package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import dev.robgro.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        model.addAttribute("timesheet", new TimesheetDto(null, null, null, 0.5, false, null, 0.0));
        return "timesheet";
    }

    @GetMapping("/timesheet-archive")
    public String showTimesheetArchive(Model model) {
        model.addAttribute("timesheets", timesheetService.getAllTimesheets());
        return "timesheet-archive";
    }

    @GetMapping("/invoice-list")
    public String showInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoice-pdf";
    }

    @GetMapping("/invoice-edit")
    public String showEditInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoice-edit";
    }
}
