package dev.robgro.timesheet.service;

import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.*;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.model.entity.InvoiceItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfGenerator {

    public void generateInvoicePdf(Invoice invoice, InvoiceSeller seller, OutputStream outputStream) {

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, new Color(0, 86, 179)); // #0056b3
            Font blueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, new Color(92, 106, 196)); // #5c6ac4
            Font blueBoldFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, new Color(92, 106, 196));

// "ORIGINAL"
            document.add(new Paragraph("ORIGINAL", boldFont));
            document.add(new Paragraph("\n"));

// data and invoice number
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            headerTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell dateLabel = new PdfPCell(new Phrase("Date", normalFont));
            PdfPCell dateValue = new PdfPCell(new Phrase(invoice.getIssueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), headerFont));

            PdfPCell invoiceLabel = new PdfPCell(new Phrase("Invoice #", normalFont));
            PdfPCell invoiceValue = new PdfPCell(new Phrase(invoice.getInvoiceNumber(), headerFont));
            dateLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dateValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceValue.setHorizontalAlignment(Element.ALIGN_RIGHT);

            dateLabel.setBorder(Rectangle.NO_BORDER);
            dateValue.setBorder(Rectangle.NO_BORDER);

// separate line
            PdfPCell separatorCell = new PdfPCell(new Phrase());
            separatorCell.setBorder(Rectangle.BOTTOM);
            separatorCell.setBorderWidth(0.5f);
            separatorCell.setBorderColor(new Color(38, 134, 214));
            separatorCell.setFixedHeight(10);
            separatorCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerTable.setWidthPercentage(15);

            invoiceLabel.setBorder(Rectangle.NO_BORDER);
            invoiceValue.setBorder(Rectangle.NO_BORDER);

            headerTable.addCell(dateLabel);
            headerTable.addCell(dateValue);
            headerTable.addCell(separatorCell);
            headerTable.addCell(invoiceLabel);
            headerTable.addCell(invoiceValue);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

// address section
            PdfPTable addressTable = new PdfPTable(2);
            addressTable.setWidthPercentage(100);

// sender / seller
            PdfPCell fromCell = new PdfPCell();
            fromCell.setBorder(Rectangle.NO_BORDER);
            fromCell.addElement(new Paragraph("Invoice from:", normalFont));
            fromCell.addElement(new Paragraph(seller.getName(), normalFont));
            fromCell.addElement(new Paragraph(seller.getStreet(), normalFont));
            fromCell.addElement(new Paragraph(seller.getPostcode() + ", " + seller.getCity(), normalFont));

// receiver / client
            PdfPCell toCell = new PdfPCell();
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph invoiceFor = new Paragraph("Invoice for:", blueFont);
            invoiceFor.setAlignment(Element.ALIGN_RIGHT);
            toCell.addElement(invoiceFor);

// client address to right
            Paragraph[] clientAddressLines = {
                    new Paragraph(invoice.getClient().getClientName(), blueBoldFont),
                    new Paragraph(invoice.getClient().getHouseNo() + " " + invoice.getClient().getStreetName(), blueBoldFont),
                    new Paragraph(invoice.getClient().getPostCode() + ", " + invoice.getClient().getCity(), blueBoldFont)
            };

            for (Paragraph line : clientAddressLines) {
                line.setAlignment(Element.ALIGN_RIGHT);
                toCell.addElement(line);
            }

            addressTable.addCell(fromCell);
            addressTable.addCell(toCell);

            document.add(new Paragraph("\n"));

// grey background for addresses section
            PdfPCell addressWrapper = new PdfPCell(addressTable);
            addressWrapper.setBackgroundColor(new Color(241, 245, 249)); // #f1f5f9
            addressWrapper.setPadding(20);
            addressWrapper.setBorder(Rectangle.NO_BORDER);

            PdfPTable wrapperTable = new PdfPTable(1);
            wrapperTable.setWidthPercentage(100);
            wrapperTable.addCell(addressWrapper);
            document.add(wrapperTable);
            document.add(new Paragraph("\n"));

// table
            PdfPTable itemsTable = new PdfPTable(new float[]{0.5f, 5, 2, 2, 2});
            itemsTable.setWidthPercentage(98);

// table head
            String[] headers = {"#", "Description", "Duration (h)", "Rate (£)", "Amount (£)"};
            PdfPCell[] headerCells = {
                    createHeaderCell(headers[0], Element.ALIGN_CENTER),    // kolumna numeracji
                    createHeaderCell(headers[1], Element.ALIGN_LEFT),      // kolumna opisu
                    createHeaderCell(headers[2], Element.ALIGN_RIGHT),     // kolumna czasu
                    createHeaderCell(headers[3], Element.ALIGN_RIGHT),     // kolumna stawki
                    createHeaderCell(headers[4], Element.ALIGN_RIGHT)      // kolumna kwoty
            };
            for (PdfPCell cell : headerCells) {
                itemsTable.addCell(cell);
            }

// table rows
            int rowNumber = 1;
            for (InvoiceItem item : invoice.getItemsList()) {
                PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowNumber++), normalFont));
                numberCell.setBorder(Rectangle.NO_BORDER);
                numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                numberCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                numberCell.setPadding(10);
                numberCell.setBorderWidthBottom(1f);
                numberCell.setBorderColor(new Color(241, 245, 249));

                PdfPCell descCell = new PdfPCell(new Phrase("Cleaning services on " +
                        item.getServiceDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), normalFont));
                descCell.setBorder(Rectangle.NO_BORDER);
                descCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                descCell.setPadding(10);
                descCell.setBorderWidthBottom(1f);
                descCell.setBorderColor(new Color(241, 245, 249));

                PdfPCell durationCell = new PdfPCell(new Phrase(String.valueOf(item.getDuration()), normalFont));
                durationCell.setBorder(Rectangle.NO_BORDER);
                durationCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                durationCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                durationCell.setPadding(10);
                durationCell.setBorderWidthBottom(1f);
                durationCell.setBorderColor(new Color(241, 245, 249));

                PdfPCell rateCell = new PdfPCell(new Phrase(String.valueOf(invoice.getClient().getHourlyRate()), normalFont));
                rateCell.setBorder(Rectangle.NO_BORDER);
                rateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                rateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                rateCell.setPadding(10);
                rateCell.setBorderWidthBottom(1f);
                rateCell.setBorderColor(new Color(241, 245, 249));

                PdfPCell amountCell = new PdfPCell(new Phrase(item.getAmount().toString(), normalFont));
                amountCell.setBorder(Rectangle.NO_BORDER);
                amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                amountCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                amountCell.setPadding(10);
                amountCell.setBorderWidthBottom(1f);
                amountCell.setBorderColor(new Color(241, 245, 249));

                itemsTable.addCell(numberCell);
                itemsTable.addCell(descCell);
                itemsTable.addCell(durationCell);
                itemsTable.addCell(rateCell);
                itemsTable.addCell(amountCell);
            }

            document.add(itemsTable);
            document.add(new Paragraph("\n"));

// total amount
            PdfPTable totalTable = new PdfPTable(1);
            totalTable.setWidthPercentage(50);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell totalCell = new PdfPCell(new PdfPTable(2));
            totalCell.setBackgroundColor(new Color(92, 106, 196)); // #5c6ac4
            totalCell.setBorder(Rectangle.NO_BORDER);
            totalCell.setPadding(10);

            PdfPTable totalInnerTable = new PdfPTable(2);
            totalInnerTable.setWidthPercentage(100);
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total Amount:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            PdfPCell totalValueCell = new PdfPCell(new Phrase("£ " + invoice.getTotalAmount().toString(), FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            totalLabelCell.setBorder(Rectangle.NO_BORDER);
            totalValueCell.setBorder(Rectangle.NO_BORDER);
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalInnerTable.addCell(totalLabelCell);
            totalInnerTable.addCell(totalValueCell);

            totalCell.addElement(totalInnerTable);
            totalTable.addCell(totalCell);
            document.add(totalTable);
            document.add(new Paragraph("\n\n\n"));

// payment details
            Paragraph paymentTitle = new Paragraph("PAYMENT DETAILS", boldFont);
            document.add(paymentTitle);
            document.add(new Paragraph("\n"));
// account holder
            Phrase holderLabel = new Phrase("Account owner: ", normalFont);
            Phrase holderValue = new Phrase("Agnieszka Markiewicz", boldFont);
            Paragraph holderLine = new Paragraph();
            holderLine.add(holderLabel);
            holderLine.add(holderValue);
            document.add(holderLine);
// bank
            Phrase bankLabel = new Phrase("Bank: ", normalFont);
            Phrase bankValue = new Phrase("TSB", boldFont);
            Paragraph bankLine = new Paragraph();
            bankLine.add(bankLabel);
            bankLine.add(bankValue);
            document.add(bankLine);
// sort code
            Phrase sortCodeLabel = new Phrase("Sort Code: ", normalFont);
            Phrase sortCodeValue = new Phrase("87-68-20", boldFont);
            Paragraph sortCodeLine = new Paragraph();
            sortCodeLine.add(sortCodeLabel);
            sortCodeLine.add(sortCodeValue);
            document.add(sortCodeLine);
// account number
            Phrase accountLabel = new Phrase("Account Number: ", normalFont);
            Phrase accountValue = new Phrase("75040460", boldFont);
            Paragraph accountLine = new Paragraph();
            accountLine.add(accountLabel);
            accountLine.add(accountValue);
            document.add(accountLine);
            document.add(new Paragraph("\n\n"));

// notes
            PdfPTable footerTable = new PdfPTable(1);
            float y = document.bottom();
            footerTable.setTotalWidth(document.getPageSize().getWidth() / 2);

            float leftMargin = document.left();

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_LEFT,
                    new Phrase("Notes:", boldFont),
                    leftMargin,
                    y + 120, 0
            );

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_LEFT,
                    new Phrase("____", normalFont),
                    leftMargin,
                    y + 105, 0
            );

// footer
            footerTable.setWidthPercentage(50);
            footerTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(102, 102, 102));

// line over the footer
            PdfPCell separatorLine = new PdfPCell(new Phrase());
            separatorLine.setBorder(Rectangle.BOTTOM);
            separatorLine.setBorderWidth(0.5f);
            separatorLine.setBorderColor(new Color(38, 134, 214));
            separatorLine.setFixedHeight(10);
            footerTable.setWidthPercentage(15);
            footerTable.addCell(separatorLine);

// hrefs
            Chunk websiteLink = new Chunk("https://robgro.dev", footerFont);
            websiteLink.setAnchor("https://robgro.dev");

            Chunk emailLink = new Chunk("contact@robgro.dev", footerFont);
            emailLink.setAnchor("mailto:contact@robgro.dev");

// separator
            Chunk separator = new Chunk("  |  ", footerFont);

            Phrase footerPhrase = new Phrase();
            footerPhrase.add(websiteLink);
            footerPhrase.add(separator);
            footerPhrase.add(new Chunk("+44 747 8385 228", footerFont));
            footerPhrase.add(separator);
            footerPhrase.add(emailLink);

            PdfPCell footerCell = new PdfPCell(footerPhrase);
            footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerCell.setBorder(Rectangle.NO_BORDER);
            footerTable.addCell(footerCell);

// footer setting
            y = document.bottom() - 10;
            footerTable.writeSelectedRows(0, -1, (document.getPageSize().getWidth() - footerTable.getTotalWidth()) / 2, y, writer.getDirectContent());

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private PdfPCell createHeaderCell(String text, int alignment) {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, boldFont));
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(new Color(92, 106, 196));
        cell.setBorderWidthBottom(2);
        cell.setBorderWidthTop(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setPaddingBottom(10);
        return cell;
    }
}
