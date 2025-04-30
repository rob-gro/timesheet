package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.timesheet.TimesheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/invoices/items")
@RequiredArgsConstructor
public class InvoiceViewController {

    private final InvoiceService invoiceService;
    private final TimesheetService timesheetService;
    private final ClientService clientService;
    private final InvoiceSeller invoiceSeller;

    @GetMapping
    public String showItemsForm(Model model) {
        log.debug("Showing invoice items form");
        model.addAttribute("timesheets", getUnbilledTimesheets());
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("createInvoiceRequest", new CreateInvoiceRequest(null, null, List.of(), null));
        return "invoices/items";
    }

    @PostMapping
    public String processItems(
            @Valid @ModelAttribute("createInvoiceRequest") CreateInvoiceRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            log.warn("Validation errors in invoice creation: {}", result.getAllErrors());
            model.addAttribute("timesheets", getUnbilledTimesheets());
            model.addAttribute("clients", clientService.getAllClients());
            return "invoices/items";
        }

        log.info("Creating new invoice for client ID: {}, with {} timesheet(s)",
                request.clientId(), request.timesheetIds().size());

        InvoiceDto invoice = invoiceService.createAndRedirectInvoice(request);
        return "redirect:/invoices/create/" + invoice.id();
    }

    @GetMapping("/edit/{id}")
    public String showEditInvoiceForm(@PathVariable Long id, Model model) {
        log.debug("Showing edit form for invoice ID: {}", id);
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        ClientDto client = clientService.getClientById(invoice.clientId());
        model.addAttribute("invoice", invoice);
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("seller", invoiceSeller);
        model.addAttribute("client", client);
        return "invoices/edit";
    }

    @GetMapping("/edit")
    public String showEditInvoices(Model model) {
        log.debug("Showing invoice edit list");
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoices/edit";
    }

    private List<TimesheetDto> getUnbilledTimesheets() {
        return timesheetService.getUnbilledTimesheets();
    }
}
