package dev.robgro.timesheet.invoice;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import dev.robgro.timesheet.seller.Seller;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InvoicePageEventHelper extends PdfPageEventHelper {

    static final float FOOTER_HEIGHT = 50f;

    // Template width for page number line - wide enough for "Page 99 / 99"
    private static final float PAGE_NUM_TEMPLATE_WIDTH = 160f;

    private static final BaseFont TEXT_FONT;
    private static final BaseFont EMOJI_FONT;

    static {
        BaseFont textFont;
        try {
            textFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        TEXT_FONT = textFont;

        BaseFont emojiFont = null;
        try (InputStream stream = InvoicePageEventHelper.class.getResourceAsStream("/fonts/NotoEmoji-Regular.ttf")) {
            if (stream != null) {
                byte[] bytes = stream.readAllBytes();
                emojiFont = BaseFont.createFont("NotoEmoji-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                log.info("NotoEmoji font loaded for PDF footer");
            } else {
                log.warn("NotoEmoji-Regular.ttf not found in /fonts/ — emoji disabled in PDF footer");
            }
        } catch (Exception e) {
            log.warn("Failed to load NotoEmoji font: {} — using text prefixes", e.getMessage());
        }
        EMOJI_FONT = emojiFont;
    }

    private final Seller seller;
    // Per-page templates for page number line (filled in onCloseDocument only if totalPages > 1)
    private final List<PdfTemplate> pageNumTemplates = new ArrayList<>();

    public InvoicePageEventHelper(Seller seller) {
        this.seller = seller;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        // nothing to initialize here anymore
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        float y = writer.getPageSize().getBottom(FOOTER_HEIGHT);
        float left = document.left();
        float right = document.right();
        float centerX = left + (right - left) / 2f;

        // separator line
        cb.setLineWidth(0.5f);
        cb.setColorStroke(new Color(38, 134, 214));
        cb.moveTo(left, y + 14);
        cb.lineTo(right, y + 14);
        cb.stroke();

        // page number: reserve a centered template, filled later in onCloseDocument
        PdfTemplate pageNumTemplate = cb.createTemplate(PAGE_NUM_TEMPLATE_WIDTH, 12);
        pageNumTemplates.add(pageNumTemplate);
        cb.addTemplate(pageNumTemplate, centerX - PAGE_NUM_TEMPLATE_WIDTH / 2f, y + 3);

        // footer contact text - centered, rendered segment by segment (mixed fonts)
        List<TextSegment> segments = buildFooterSegments();
        if (!segments.isEmpty()) {
            float totalWidth = 0f;
            for (TextSegment seg : segments) {
                totalWidth += seg.font().getWidthPoint(seg.text(), 8);
            }
            float currentX = centerX - totalWidth / 2f;
            float textY = y - 9;
            for (TextSegment seg : segments) {
                float segWidth = seg.font().getWidthPoint(seg.text(), 8);
                boolean isLink = seg.linkUrl() != null;
                cb.beginText();
                cb.setFontAndSize(seg.font(), 8);
                cb.setColorFill(isLink ? new Color(0, 102, 204) : new Color(102, 102, 102));
                cb.setTextMatrix(currentX, textY);
                cb.showText(seg.text());
                cb.endText();
                if (isLink) {
                    // underline
                    cb.setLineWidth(0.5f);
                    cb.setColorStroke(new Color(0, 102, 204));
                    cb.moveTo(currentX, textY - 1.5f);
                    cb.lineTo(currentX + segWidth, textY - 1.5f);
                    cb.stroke();
                    // clickable annotation
                    Rectangle linkRect = new Rectangle(currentX, textY - 2, currentX + segWidth, textY + 8);
                    PdfAnnotation link = PdfAnnotation.createLink(writer, linkRect,
                            PdfAnnotation.HIGHLIGHT_NONE, new PdfAction(seg.linkUrl()));
                    writer.addAnnotation(link);
                }
                currentX += segWidth;
            }
        }
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        int totalPages = pageNumTemplates.size(); // getPageNumber() returns totalPages+1, use our own count
        if (totalPages <= 1) {
            // single page — leave templates empty, nothing rendered
            return;
        }
        for (int i = 0; i < pageNumTemplates.size(); i++) {
            String pageText = "Page " + (i + 1) + " / " + totalPages;
            float textWidth = TEXT_FONT.getWidthPoint(pageText, 8);
            PdfTemplate tmpl = pageNumTemplates.get(i);
            tmpl.beginText();
            tmpl.setFontAndSize(TEXT_FONT, 8);
            tmpl.setColorFill(new Color(102, 102, 102));
            // center within the template
            tmpl.setTextMatrix((PAGE_NUM_TEMPLATE_WIDTH - textWidth) / 2f, 3);
            tmpl.showText(pageText);
            tmpl.endText();
        }
    }

    private List<TextSegment> buildFooterSegments() {
        List<TextSegment> segments = new ArrayList<>();
        boolean first = true;

        if (seller.getWebsite() != null && !seller.getWebsite().isBlank()) {
            addEmojiOrText(segments, "\uD83C\uDF10 ", "Web: ");  // 🌐
            String website = seller.getWebsite().trim();
            String websiteUrl = website.startsWith("http") ? website : "https://" + website;
            segments.add(new TextSegment(website, TEXT_FONT, websiteUrl));
            first = false;
        }
        if (seller.getPhone() != null && !seller.getPhone().isBlank()) {
            if (!first) segments.add(new TextSegment("  |  ", TEXT_FONT));
            addEmojiOrText(segments, "\uD83D\uDCDE ", "Tel: ");  // 📞
            segments.add(new TextSegment(seller.getPhone(), TEXT_FONT));
            first = false;
        }
        if (seller.getEmail() != null && !seller.getEmail().isBlank()) {
            if (!first) segments.add(new TextSegment("  |  ", TEXT_FONT));
            addEmojiOrText(segments, "\u2709 ", "");             // ✉
            String email = seller.getEmail().trim();
            segments.add(new TextSegment(email, TEXT_FONT, "mailto:" + email));
        }
        return segments;
    }

    private void addEmojiOrText(List<TextSegment> segments, String emoji, String fallback) {
        if (EMOJI_FONT != null) {
            segments.add(new TextSegment(emoji, EMOJI_FONT));
        } else if (!fallback.isEmpty()) {
            segments.add(new TextSegment(fallback, TEXT_FONT));
        }
    }

    private record TextSegment(String text, BaseFont font, String linkUrl) {
        TextSegment(String text, BaseFont font) {
            this(text, font, null);
        }
    }
}
