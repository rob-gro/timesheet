package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.seller.SellerDto;
import dev.robgro.timesheet.seller.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/invoices/create")
@RequiredArgsConstructor
public class InvoiceCreateController {
    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final SellerService sellerService;

    @GetMapping("/{id}")
    public String showCreateForm(@PathVariable Long id, Model model) {
        log.debug("Received request for invoice ID: {}", id);

        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        log.debug("Found invoice: {}", invoice);

        ClientDto client = clientService.getClientById(invoice.clientId());
        SellerDto seller = sellerService.getSellerById(invoice.sellerId());

        model.addAttribute("invoice", invoice);
        model.addAttribute("seller", seller);
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

    @PostMapping("/confirm")
    @ResponseBody
    @Transactional
    public void confirmAndCreateInvoice(@RequestBody CreateInvoiceRequest request) {
        log.info("Creating invoice from preview for client ID: {}, with {} timesheet(s)",
                request.clientId(), request.timesheetIds().size());

        // Create invoice in database
        InvoiceDto invoice = invoiceService.createAndRedirectInvoice(request);
        log.info("Invoice created with ID: {}", invoice.id());

        // Save PDF and send email
        invoiceService.savePdfAndSendInvoice(invoice.id());
        log.info("Successfully saved and sent invoice with ID: {}", invoice.id());
    }
}
