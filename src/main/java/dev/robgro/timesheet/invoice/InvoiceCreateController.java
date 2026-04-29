package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.invoice.delivery.InvoiceDeliveryJobService;
import dev.robgro.timesheet.seller.SellerDto;
import dev.robgro.timesheet.seller.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/invoices/create")
@RequiredArgsConstructor
public class InvoiceCreateController {
    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final SellerService sellerService;
    private final InvoiceDeliveryJobService deliveryJobService;

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
    @ResponseBody
    public ResponseEntity<Void> saveAndSendInvoice(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "COPY") PrintMode printMode) {
        log.info("Requesting async delivery for invoice ID: {}, printMode: {}", id, printMode);
        deliveryJobService.requestDelivery(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm")
    @ResponseBody
    public void confirmAndCreateInvoice(@RequestBody CreateInvoiceRequest request) {
        log.info("Creating invoice from preview for client ID: {}, with {} timesheet(s)",
                request.clientId(), request.timesheetIds().size());

        InvoiceDto invoice = invoiceService.createAndRedirectInvoice(request);
        log.info("Invoice created with ID: {} — delivery job enqueued async", invoice.id());
    }
}
