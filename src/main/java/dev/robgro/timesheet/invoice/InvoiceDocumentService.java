package dev.robgro.timesheet.invoice;

public interface InvoiceDocumentService {
    byte[] getInvoicePdfContent(Long invoiceId);

    void savePdfAndSendInvoice(Long invoiceId, PrintMode printMode);
}
