package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/invoices/create")
@RequiredArgsConstructor
public class InvoiceCreateController {
    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final InvoiceSeller invoiceSeller;

    @GetMapping("/{id}")
    public String showCreateForm(@PathVariable Long id, Model model) {
        log.debug("Received request for invoice ID: {}", id);

        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        log.debug("Found invoice: {}", invoice);

        ClientDto client = clientService.getClientById(invoice.clientId());

        model.addAttribute("invoice", invoice);
        model.addAttribute("seller", invoiceSeller);
        model.addAttribute("client", client);
        return "invoices/create";
    }

    @PostMapping("/{id}")
    public String viewInvoice(@PathVariable Long id) {
        return "invoices/create";
    }

    @PostMapping("/{id}/save-and-send")
    public String saveAndSendInvoice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Saving and sending invoice ID: {}", id);

        invoiceService.savePdfAndSendInvoice(id);

        log.info("Successfully saved and sent invoice with ID: {}", id);
        redirectAttributes.addFlashAttribute("success", "Invoice has been saved and sent");
        return "redirect:/invoices/create/" + id;
    }
}
