package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/invoice-create")
@RequiredArgsConstructor
public class InvoiceCreateController {
    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final InvoiceSeller invoiceSeller;

    @GetMapping("/{id}")
    public String showCreateForm(@PathVariable Long id, Model model) {
        System.out.println("LOG_4 -> Received request for invoice ID: " + id);  // log 4
        System.out.println("LOG_8 -> Seller name: " + invoiceSeller.getName()); // log 8

        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        System.out.println("LOG_5 -> Found invoice: " + invoice);  // log 5

        ClientDto client = clientService.getClientById(invoice.clientId());
        System.out.println("LOG_6 -> Found client: " + client);  // log 6

        System.out.println("LOG_7 -> Seller properties: " + invoiceSeller);
        model.addAttribute("invoice", invoice);
        model.addAttribute("seller", invoiceSeller);
        model.addAttribute("client", client);
        return "invoice-create";
    }

    @PostMapping("/{id}")
    public String createInvoice(@PathVariable Long id) {
        return "invoice-create";
    }

    @PostMapping("/{id}/save-and-send")
    public String saveAndSendInvoice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.savePdfAndSendInvoice(id);
            System.out.println("LOG_11 -> invoiceService.savePdfAndSendInvoice(id) is working correctly");
            redirectAttributes.addFlashAttribute("success", "Invoice has been saved and sent");
            System.out.println("LOG_12 -> redirectAttributes.addFlashAttribute is working correctly");
            return "redirect:/invoice-create/" + id;
        } catch (Exception e) {
            System.out.println("LOG_13 -> There is a problem with the invoice saved and send PDF");

            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
