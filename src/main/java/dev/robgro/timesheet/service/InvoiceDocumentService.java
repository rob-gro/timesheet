package dev.robgro.timesheet.service;

public interface InvoiceDocumentService {
    byte[] getInvoicePdfContent(Long invoiceId);

    void savePdfAndSendInvoice(Long invoiceId);
}
