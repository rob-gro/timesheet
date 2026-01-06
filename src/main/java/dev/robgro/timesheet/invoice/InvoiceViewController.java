package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.timesheet.TimesheetService;
import dev.robgro.timesheet.seller.SellerDto;
import dev.robgro.timesheet.seller.SellerService;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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
    private final SellerService sellerService;
    private final UserService userService;

    @GetMapping
    public String showItemsForm(Model model, Authentication authentication) {
        log.debug("Showing invoice items form");
        model.addAttribute("timesheets", getUnbilledTimesheets());
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("sellers", sellerService.getAllSellers());
        model.addAttribute("createInvoiceRequest", new CreateInvoiceRequest(null, null, null, List.of(), null));

        // Add current user for default seller pre-selection
        if (authentication != null) {
            User currentUser = userService.findByUsername(authentication.getName());
            model.addAttribute("currentUser", currentUser);
        }
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
            model.addAttribute("sellers", sellerService.getAllSellers());
            return "invoices/items";
        }

        log.info("Building invoice preview for client ID: {}, with {} timesheet(s)",
                request.clientId(), request.timesheetIds().size());

        // Build preview without creating invoice in database
        InvoiceDto previewInvoice = invoiceService.buildInvoicePreview(request);
        ClientDto client = clientService.getClientById(request.clientId());
        SellerDto seller = sellerService.getSellerById(request.sellerId());

        model.addAttribute("invoice", previewInvoice);
        model.addAttribute("client", client);
        model.addAttribute("seller", seller);
        model.addAttribute("createRequest", request);
        model.addAttribute("isPreview", true);

        return "invoices/create";
    }

    @GetMapping("/edit/{id}")
    public String showEditInvoiceForm(@PathVariable Long id, Model model) {
        log.debug("Showing edit form for invoice ID: {}", id);
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        ClientDto client = clientService.getClientById(invoice.clientId());
        SellerDto seller = sellerService.getSellerById(invoice.sellerId());
        model.addAttribute("invoice", invoice);
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("seller", seller);
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
