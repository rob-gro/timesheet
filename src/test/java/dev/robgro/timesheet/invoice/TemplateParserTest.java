package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateParserTest {

    private TemplateParser templateParser;

    @BeforeEach
    void setUp() {
        templateParser = new TemplateParser();
    }

    @Test
    void shouldParseStandardMonthlyTemplate() {
        // Given
        String template = "{SEQ:3}-{MM}-{YYYY}";
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(1)
            .year(2026)
            .month(2)
            .build();

        // When
        String result = templateParser.apply(template, context);

        // Then
        assertThat(result).isEqualTo("001-02-2026");
    }

    @Test
    void shouldParseYearlyTemplateWithoutMonth() {
        // Given
        String template = "{SEQ:3}-{YYYY}";
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(5)
            .year(2026)
            .month(0) // YEARLY - no month
            .build();

        // When
        String result = templateParser.apply(template, context);

        // Then
        assertThat(result).isEqualTo("005-2026");
    }

    @Test
    void shouldHandleZeroPaddingInSequence() {
        // Given
        String template = "{SEQ:5}-{YYYY}";
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(42)
            .year(2026)
            .month(0)
            .build();

        // When
        String result = templateParser.apply(template, context);

        // Then
        assertThat(result).isEqualTo("00042-2026");
    }

    @Test
    void shouldParsePolishFormat() {
        // Given
        String template = "FV/{SEQ:3}/{YYYY}";
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(123)
            .year(2026)
            .month(0)
            .build();

        // When
        String result = templateParser.apply(template, context);

        // Then
        assertThat(result).isEqualTo("FV/123/2026");
    }

    @Test
    void shouldParseShortYear() {
        // Given
        String template = "{SEQ:3}-{YY}";
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(1)
            .year(2026)
            .month(0)
            .build();

        // When
        String result = templateParser.apply(template, context);

        // Then
        assertThat(result).isEqualTo("001-26");
    }

    @Test
    void shouldAlwaysReplaceMonthToken_withActualIssueMonth() {
        // TemplateContext.month is always the actual issue month (1-12), never 0.
        // month=0 (YEARLY counter sentinel) never reaches TemplateParser —
        // InvoiceNumberGeneratorImpl uses issueDate.getMonthValue() for display.
        // This test documents the contract: {MM} is always replaced, not conditionally stripped.
        String template = "{SEQ:3}-{MM}-{YYYY}";
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(1)
            .year(2026)
            .month(3) // actual invoice month from issueDate
            .build();

        String result = templateParser.apply(template, context);

        assertThat(result).isEqualTo("001-03-2026");
    }

    @Test
    void shouldValidateTemplateWithSeqToken() {
        // Given
        String validTemplate = "{SEQ:3}-{YYYY}";

        // When & Then
        templateParser.validateTemplate(validTemplate); // Should not throw
    }

    @Test
    void shouldRejectTemplateWithoutSeqToken() {
        // Given
        String invalidTemplate = "{YYYY}-{MM}";

        // When & Then
        assertThatThrownBy(() -> templateParser.validateTemplate(invalidTemplate))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must contain {SEQ:N}");
    }

    @Test
    void shouldRejectTemplateTooLong() {
        // Given
        String longTemplate = "{SEQ:3}" + "X".repeat(100);

        // When & Then
        assertThatThrownBy(() -> templateParser.validateTemplate(longTemplate))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("too long");
    }

    @Test
    void shouldRejectInvalidPadding() {
        // Given
        String invalidTemplate = "{SEQ:0}-{YYYY}";

        // When & Then
        assertThatThrownBy(() -> templateParser.validateTemplate(invalidTemplate))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("padding must be between 1 and 10");
    }

    @Test
    void shouldGeneratePreview() {
        // Given
        String template = "{SEQ:3}-{MM}-{YYYY}";

        // When
        String preview = templateParser.preview(template);

        // Then
        assertThat(preview).isEqualTo("001-02-2026");
    }

    @Test
    void shouldRejectNullTemplate() {
        // When & Then
        assertThatThrownBy(() -> templateParser.apply(null, TemplateContext.builder().build()))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectNullContext() {
        // When & Then
        assertThatThrownBy(() -> templateParser.apply("{SEQ:3}", null))
            .isInstanceOf(ValidationException.class);
    }
}
