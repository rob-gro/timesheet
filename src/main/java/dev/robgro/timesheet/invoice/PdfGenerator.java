package dev.robgro.timesheet.invoice;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import dev.robgro.timesheet.exception.ServiceOperationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerator.class);

    // Height of pre-table content on page 1: ORIGINAL/COPY label + invoice header + addresses.
    // Measured with current fonts/margins (A4, top/bottom=36pt, footerHeight=50pt).
    // If header layout changes significantly, recalibrate this constant.
    private static final float HEADER_BLOCK_HEIGHT_PT = 260f;

    // Height of totals section: Total Amount box (with paddingBottom=40) + Payment Details.
    // Measured with current fonts/margins (A4, top/bottom=36pt, footerHeight=50pt).
    private static final float TOTALS_BLOCK_HEIGHT_PT = 160f;

    private static final float SAFETY_GAP_PT = 20f;
    private static final int   MIN_ROWS_PER_PAGE = 2;
    private static final int   MAX_DESCRIPTION_CHARS = 80;
    // Reserved height for the "Continued on next page" footer row embedded inside the items table.
    // Subtracted from avail1/availN so the footer always stays on the same page as the last item row.
    private static final float CONTINUED_ROW_HEIGHT_PT = 30f;

    public void generateInvoicePdf(Invoice invoice, OutputStream outputStream, PrintMode printMode) {

        try {
            float left = 36f, right = 36f, top = 36f, bottom = 36f;
            Document document = new Document(PageSize.A4, left, right, top, bottom + InvoicePageEventHelper.FOOTER_HEIGHT);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new InvoicePageEventHelper(invoice.getSeller()));
            document.open();

            Font normalFont    = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont      = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
            Font headerFont    = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, new Color(0, 86, 179));
            Font blueFont      = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, new Color(92, 106, 196));
            Font blueBoldFont  = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, new Color(92, 106, 196));

            float pageH     = PageSize.A4.getHeight() - top - bottom - InvoicePageEventHelper.FOOTER_HEIGHT;
            float bodyWidth = PageSize.A4.getWidth() - left - right;
            float tableWidth = bodyWidth * 0.98f;

            // Phase 1: prepare descriptions (truncation BEFORE measurement)
            List<String> descriptions = prepareDescriptions(invoice);

            // Phase 2: measure
            float[] allRowHeights  = measureAllRowHeights(invoice, descriptions, normalFont, tableWidth);
            float   hdrH           = allRowHeights[0];
            float[] dataRowHeights = new float[descriptions.size()];
            System.arraycopy(allRowHeights, 1, dataRowHeights, 0, descriptions.size());

            // Reserve space for "Continued on next page" footer row on non-last item pages.
            // The last page "wastes" this reserve but simplifies planning (all pages treated equally).
            float avail1 = pageH - HEADER_BLOCK_HEIGHT_PT - hdrH - CONTINUED_ROW_HEIGHT_PT;
            float availN = pageH - hdrH - CONTINUED_ROW_HEIGHT_PT;

            // Phase 2: plan
            List<int[]> slices = PaginationPlanner.computeSlices(dataRowHeights, avail1, availN, MIN_ROWS_PER_PAGE);

            // totals-fit check
            boolean totalsOnSamePage = false;
            if (!slices.isEmpty()) {
                int[]  last      = slices.get(slices.size() - 1);
                float  lastAvail = (slices.size() == 1) ? avail1 : availN;
                float  used      = PaginationPlanner.sumHeights(dataRowHeights, last[0], last[1]);
                totalsOnSamePage = (lastAvail - used) >= TOTALS_BLOCK_HEIGHT_PT + SAFETY_GAP_PT;
            }

            // Phase 3: render page 1 header
            renderPage1Header(document, invoice, printMode, normalFont, boldFont, blueFont, blueBoldFont, headerFont);

            // Phase 3: render items pages
            for (int pageIdx = 0; pageIdx < slices.size(); pageIdx++) {
                if (pageIdx > 0) document.newPage();
                int[] slice = slices.get(pageIdx);
                boolean addContinued = (pageIdx < slices.size() - 1);
                document.add(buildItemsTableForPage(invoice, descriptions, normalFont, slice[0], slice[1], addContinued));
            }

            // Phase 3: render totals
            if (!totalsOnSamePage && !slices.isEmpty()) {
                document.newPage();
                addInvoiceSummaryHeader(document, boldFont);
            } else if (!slices.isEmpty()) {
                document.add(Chunk.NEWLINE);
            }
            document.add(buildTotalsSection(invoice));

            document.close();
        } catch (Exception e) {
            throw new ServiceOperationException("Failed to generate PDF", e);
        }
    }

    // --- Phase 1 ---

    private List<String> prepareDescriptions(Invoice invoice) {
        String serviceDesc = invoice.getSeller() != null && invoice.getSeller().getServiceDescription() != null
                ? invoice.getSeller().getServiceDescription()
                : "Services";
        List<String> descriptions = new ArrayList<>();
        for (InvoiceItem item : invoice.getItemsList()) {
            String desc = serviceDesc + " " + item.getServiceDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (desc.length() > MAX_DESCRIPTION_CHARS) {
                log.warn("Description too long for item {}, truncating to {} chars", item.getServiceDate(), MAX_DESCRIPTION_CHARS);
                desc = desc.substring(0, MAX_DESCRIPTION_CHARS - 1) + "\u2026";
            }
            descriptions.add(desc);
        }
        return descriptions;
    }

    // --- Phase 2 ---

    private float[] measureAllRowHeights(Invoice invoice, List<String> descriptions, Font normalFont, float tableWidth) {
        int toItem = descriptions.isEmpty() ? -1 : descriptions.size() - 1;
        // Measure without the "Continued" footer row; its height is accounted for via CONTINUED_ROW_HEIGHT_PT constant.
        PdfPTable table = buildRawTable(invoice, descriptions, normalFont, 0, toItem, false);
        table.setTotalWidth(tableWidth);
        table.setLockedWidth(true);
        table.calculateHeights(true);

        float[] heights = new float[descriptions.size() + 1];
        for (int i = 0; i <= descriptions.size(); i++) {
            heights[i] = table.getRowHeight(i);  // i=0 is header row
        }
        return heights;
    }

    // --- Phase 3 render helpers ---

    private void renderPage1Header(Document document, Invoice invoice, PrintMode printMode,
                                   Font normalFont, Font boldFont,
                                   Font blueFont, Font blueBoldFont, Font headerFont)
            throws DocumentException {

        if (printMode != PrintMode.NONE) {
            document.add(new Paragraph(printMode.name(), boldFont));
            document.add(new Paragraph("\n"));
        }

        // date and invoice number
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell dateLabel    = new PdfPCell(new Phrase("Date", normalFont));
        PdfPCell dateValue    = new PdfPCell(new Phrase(invoice.getIssueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), headerFont));
        PdfPCell invoiceLabel = new PdfPCell(new Phrase("Invoice #", normalFont));
        PdfPCell invoiceValue = new PdfPCell(new Phrase(invoice.getInvoiceNumber(), headerFont));

        dateLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        invoiceLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        invoiceValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateLabel.setBorder(Rectangle.NO_BORDER);
        dateValue.setBorder(Rectangle.NO_BORDER);

        PdfPCell separatorCell = new PdfPCell(new Phrase());
        separatorCell.setBorder(Rectangle.BOTTOM);
        separatorCell.setBorderWidth(0.5f);
        separatorCell.setBorderColor(new Color(38, 134, 214));
        separatorCell.setFixedHeight(10);
        separatorCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.setWidthPercentage(25);

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

        PdfPCell fromCell = new PdfPCell();
        fromCell.setBorder(Rectangle.NO_BORDER);
        fromCell.addElement(new Paragraph("Invoice from:", normalFont));
        fromCell.addElement(new Paragraph(invoice.getSeller().getName(), boldFont));
        fromCell.addElement(new Paragraph(invoice.getSeller().getStreet(), boldFont));
        fromCell.addElement(new Paragraph(invoice.getSeller().getPostcode() + ", " + invoice.getSeller().getCity(), boldFont));

        PdfPCell toCell = new PdfPCell();
        toCell.setBorder(Rectangle.NO_BORDER);
        toCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph invoiceFor = new Paragraph("Invoice for:", blueFont);
        invoiceFor.setAlignment(Element.ALIGN_RIGHT);
        toCell.addElement(invoiceFor);

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

        PdfPCell addressWrapper = new PdfPCell(addressTable);
        addressWrapper.setBackgroundColor(new Color(241, 245, 249));
        addressWrapper.setPadding(20);
        addressWrapper.setBorder(Rectangle.NO_BORDER);

        PdfPTable wrapperTable = new PdfPTable(1);
        wrapperTable.setWidthPercentage(100);
        wrapperTable.addCell(addressWrapper);
        document.add(wrapperTable);
        document.add(new Paragraph("\n"));
    }

    private PdfPTable buildItemsTableForPage(Invoice invoice, List<String> descriptions,
                                             Font normalFont, int fromItem, int toItem,
                                             boolean addContinuedFooter) {
        PdfPTable table = buildRawTable(invoice, descriptions, normalFont, fromItem, toItem, addContinuedFooter);
        table.setWidthPercentage(98);
        return table;
    }

    /**
     * Builds a raw PdfPTable with the header row + data rows [fromItem..toItem].
     * fromItem=0, toItem=-1 → header-only table (empty list).
     * If addContinuedFooter=true, appends a "Continued on next page" footer row (colspan=5).
     * Caller sets width/alignment before use.
     */
    private PdfPTable buildRawTable(Invoice invoice, List<String> descriptions,
                                    Font normalFont, int fromItem, int toItem,
                                    boolean addContinuedFooter) {
        PdfPTable table = new PdfPTable(new float[]{0.8f, 5, 2, 2, 2});

        // header row
        String[] headers = {"#", "Description", "Duration (h)", "Rate (£)", "Amount (£)"};
        table.addCell(createHeaderCell(headers[0], Element.ALIGN_CENTER));
        table.addCell(createHeaderCell(headers[1], Element.ALIGN_LEFT));
        table.addCell(createHeaderCell(headers[2], Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell(headers[3], Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell(headers[4], Element.ALIGN_RIGHT));

        // data rows
        List<InvoiceItem> items = invoice.getItemsList();
        for (int i = fromItem; i <= toItem; i++) {
            InvoiceItem item = items.get(i);
            String description = descriptions.get(i);
            int rowNumber = i + 1;  // 1-based global row number

            table.addCell(createDataCell(String.valueOf(rowNumber), normalFont, Element.ALIGN_CENTER));
            table.addCell(createDataCell(description, normalFont, Element.ALIGN_LEFT));
            table.addCell(createDataCell(String.valueOf(item.getDuration()), normalFont, Element.ALIGN_RIGHT));
            table.addCell(createDataCell(String.format(Locale.UK, "%.2f", item.getHourlyRate()), normalFont, Element.ALIGN_RIGHT));
            table.addCell(createDataCell(item.getAmount().toString(), normalFont, Element.ALIGN_RIGHT));
        }

        if (addContinuedFooter) {
            Font f = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(150, 150, 150));
            PdfPCell continuedCell = new PdfPCell(new Phrase("Continued on next page", f));
            continuedCell.setColspan(5);
            continuedCell.setBorder(Rectangle.NO_BORDER);
            continuedCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            continuedCell.setPaddingTop(8);
            continuedCell.setPaddingBottom(8);
            continuedCell.setPaddingRight(4);
            table.addCell(continuedCell);
        }

        return table;
    }

    private PdfPTable buildTotalsSection(Invoice invoice) {
        PdfPTable totalsSection = new PdfPTable(1);
        totalsSection.setWidthPercentage(100);

        // total amount
        Font whiteFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, Color.WHITE);

        PdfPTable totalTable = new PdfPTable(1);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPTable totalInnerTable = new PdfPTable(2);
        totalInnerTable.setWidthPercentage(100);
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total Amount:", whiteFont));
        PdfPCell totalValueCell = new PdfPCell(new Phrase("£ " + invoice.getTotalAmount().toString(), whiteFont));
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalInnerTable.addCell(totalLabelCell);
        totalInnerTable.addCell(totalValueCell);

        PdfPCell totalCell = new PdfPCell(totalInnerTable);
        totalCell.setBackgroundColor(new Color(92, 106, 196));
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setPadding(10);
        totalTable.addCell(totalCell);

        PdfPCell totalTableWrapper = new PdfPCell(totalTable);
        totalTableWrapper.setBorder(Rectangle.NO_BORDER);
        totalTableWrapper.setPaddingBottom(40);
        totalsSection.addCell(totalTableWrapper);

        // payment details
        Font paymentNormalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font paymentBoldFont   = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD);

        PdfPCell paymentTitleCell = new PdfPCell(new Phrase("PAYMENT DETAILS", paymentBoldFont));
        paymentTitleCell.setBorder(Rectangle.NO_BORDER);
        paymentTitleCell.setPaddingBottom(4);
        totalsSection.addCell(paymentTitleCell);

        PdfPCell holderCell = new PdfPCell();
        holderCell.setBorder(Rectangle.NO_BORDER);
        Paragraph holderLine = new Paragraph();
        holderLine.add(new Chunk("Account owner: ", paymentNormalFont));
        holderLine.add(new Chunk(invoice.getSeller().getName(), paymentBoldFont));
        holderCell.addElement(holderLine);
        Paragraph bankLine = new Paragraph();
        bankLine.add(new Chunk("Bank: ", paymentNormalFont));
        bankLine.add(new Chunk(invoice.getSeller().getBankName(), paymentBoldFont));
        holderCell.addElement(bankLine);
        Paragraph sortCodeLine = new Paragraph();
        sortCodeLine.add(new Chunk("Sort Code: ", paymentNormalFont));
        sortCodeLine.add(new Chunk(invoice.getSeller().getSortCode(), paymentBoldFont));
        holderCell.addElement(sortCodeLine);
        Paragraph accountLine = new Paragraph();
        accountLine.add(new Chunk("Account Number: ", paymentNormalFont));
        accountLine.add(new Chunk(invoice.getSeller().getAccountNumber(), paymentBoldFont));
        holderCell.addElement(accountLine);
        totalsSection.addCell(holderCell);

        return totalsSection;
    }

    private void addInvoiceSummaryHeader(Document document, Font boldFont) throws DocumentException {
        Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Font.BOLD, new Color(0x77, 0x77, 0x77));
        Paragraph summary = new Paragraph("INVOICE SUMMARY", summaryFont);
        summary.setSpacingAfter(6f);
        document.add(summary);

        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell sepCell = new PdfPCell(new Phrase(""));
        sepCell.setBorder(Rectangle.BOTTOM);
        sepCell.setBorderWidth(0.5f);
        sepCell.setBorderColorBottom(new Color(0xAA, 0xAA, 0xAA));
        sepCell.setFixedHeight(8f);
        sepCell.setPadding(0);
        sep.addCell(sepCell);
        sep.setSpacingAfter(10f);
        document.add(sep);
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

    private PdfPCell createDataCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10);
        cell.setBorderWidthBottom(1f);
        cell.setBorderColor(new Color(241, 245, 249));
        return cell;
    }
}