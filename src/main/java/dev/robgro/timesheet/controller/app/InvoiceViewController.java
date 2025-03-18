package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.CreateInvoiceRequest;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import dev.robgro.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/invoice-items")
@RequiredArgsConstructor
public class InvoiceViewController {

    private final InvoiceService invoiceService;
    private final TimesheetService timesheetService;
    private final ClientService clientService;

    @GetMapping
    public String showItemsForm(Model model) {
        model.addAttribute("timesheets", getUnbilledTimesheets());
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("createInvoiceRequest", new CreateInvoiceRequest(null, null, List.of(), null));
        return "invoice-items";
    }

    @PostMapping
    public String processItems(@ModelAttribute("createInvoiceRequest") CreateInvoiceRequest request,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("timesheets", getUnbilledTimesheets());
            model.addAttribute("clients", clientService.getAllClients());
            return "invoice-items";
        }

        try {
            InvoiceDto invoice = invoiceService.createAndRedirectInvoice(request);
            return "redirect:/invoice-create/" + invoice.id();
        } catch (Exception e) {
            model.addAttribute("error", "There is an issue: " + e.getMessage());
            model.addAttribute("timesheets", getUnbilledTimesheets());
            model.addAttribute("clients", clientService.getAllClients());
            return "invoice-items";
        }
    }

    private List<TimesheetDto> getUnbilledTimesheets() {
        return timesheetService.getUnbilledTimesheets();
    }
}
