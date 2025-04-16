package dev.robgro.timesheet.service;

import dev.robgro.timesheet.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceNumberGeneratorImpl implements InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    @Override
    public String generateInvoiceNumber(LocalDate issueDate) {
        int year = issueDate.getYear();
        int month = issueDate.getMonthValue();
        String yearMonth = String.format("%02d-%d", month, year);

        List<Integer> existingNumbers = invoiceRepository.findByInvoiceNumberEndingWith(yearMonth)
                .stream()
                .map(invoice -> Integer.parseInt(invoice.getInvoiceNumber().substring(0, 3)))
                .sorted()
                .toList();

        int nextNumber = 1;
        for (Integer existingNumber : existingNumbers) {
            if (existingNumber != nextNumber) {
                break;
            }
            nextNumber++;
        }
        return String.format("%03d-%s", nextNumber, yearMonth);
    }
}
