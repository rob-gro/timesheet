package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.model.dto.CreateInvoiceRequest;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.BillingService;
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
    private final BillingService billingService;
    private final InvoiceSeller invoiceSeller;

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
            InvoiceDto invoice = billingService.createInvoice(
                    request.clientId(),
                    request.issueDate(),
                    request.timesheetIds()
            );

            System.out.println("LOG_1 -> Created invoice with ID: " + invoice.id());  // log 1
            String redirectUrl = "redirect:/invoice-create/" + invoice.id();
            System.out.println("LOG_2 -> Redirecting to: " + redirectUrl);  // log 2
            return redirectUrl;

        } catch (Exception e) {
            System.out.println("LOG_3 -> Error creating invoice: " + e.getMessage());  // log 3
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
