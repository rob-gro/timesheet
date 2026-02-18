package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses invoice number templates and applies token replacements.
 *
 * Supported tokens:
 * - {SEQ:N} - Sequence number with N-digit padding (e.g., {SEQ:3} → 001)
 * - {YYYY} - Full year (e.g., 2026)
 * - {YY} - Short year (e.g., 26)
 * - {MM} - Month with zero padding (e.g., 01, 02..12)
 * - {M} - Month without padding (e.g., 1, 2..12)
 * - {DEPT} - Department code (e.g., DUT, DUI)
 * - {DEPT_NAME} - Full department name
 *
 * IMPORTANT: This class does NOT depend on JPA entities.
 * Works only with TemplateContext (Value Object) for easier testing.
 */
@Component
public class TemplateParser {

    private static final Pattern SEQ_PATTERN = Pattern.compile("\\{SEQ:(\\d+)}");

    /**
     * Apply template to generate display number.
     *
     * @param template Template string with tokens (e.g., "{SEQ:3}-{MM}-{YYYY}")
     * @param context Context with values for replacement
     * @return Formatted invoice number (e.g., "001-01-2026")
     * @throws ValidationException if template is invalid or context is incomplete
     */
    public String apply(String template, TemplateContext context) {
        if (template == null || template.isBlank()) {
            throw new ValidationException("Template cannot be null or blank");
        }
        if (context == null) {
            throw new ValidationException("Template context is required");
        }

        String result = template;

        // Process {SEQ:N} with padding
        result = processSequenceToken(result, context.getSequenceNumber());

        // Replace year tokens
        result = result.replace("{YYYY}", String.valueOf(context.getYear()));
        result = result.replace("{YY}", String.valueOf(context.getYear() % 100));

        // Replace month tokens — {MM}/{M} are display tokens, always replaced with actual month
        // month=0 is only a counter key sentinel (YEARLY/NEVER); display always uses issueDate month
        result = result.replace("{MM}", String.format("%02d", context.getMonth()));
        result = result.replace("{M}", String.valueOf(context.getMonth()));

        // Replace department tokens if present
        if (context.getDepartment() != null) {
            result = result.replace("{DEPT}", context.getDepartment().getCode());
            result = result.replace("{DEPT_NAME}", context.getDepartment().getName());
        } else {
            // Remove department tokens if no department
            result = result.replace("-{DEPT}", "");
            result = result.replace("{DEPT}-", "");
            result = result.replace("{DEPT}", "");
            result = result.replace("{DEPT_NAME}", "");
        }

        return result;
    }

    /**
     * Process {SEQ:N} token with zero-padding.
     *
     * @param template Template containing {SEQ:N} token
     * @param sequenceNumber Sequence number to format
     * @return Template with {SEQ:N} replaced by padded sequence
     */
    private String processSequenceToken(String template, Integer sequenceNumber) {
        if (sequenceNumber == null) {
            throw new ValidationException("Sequence number is required");
        }

        Matcher matcher = SEQ_PATTERN.matcher(template);
        if (!matcher.find()) {
            throw new ValidationException("Template must contain {SEQ:N} token");
        }

        int padding = Integer.parseInt(matcher.group(1));
        String paddedSequence = String.format("%0" + padding + "d", sequenceNumber);

        return matcher.replaceAll(paddedSequence);
    }

    /**
     * Validate template format without applying it.
     * Checks for required tokens and valid syntax.
     *
     * @param template Template to validate
     * @throws ValidationException if template is invalid
     */
    public void validateTemplate(String template) {
        if (template == null || template.isBlank()) {
            throw new ValidationException("Template cannot be null or blank");
        }

        if (template.length() > 64) {
            throw new ValidationException("Template too long (max 64 characters)");
        }

        // Must contain {SEQ:N}
        Matcher matcher = SEQ_PATTERN.matcher(template);
        if (!matcher.find()) {
            throw new ValidationException("Template must contain {SEQ:N} token (e.g., {SEQ:3})");
        }

        // Validate padding value
        int padding = Integer.parseInt(matcher.group(1));
        if (padding < 1 || padding > 10) {
            throw new ValidationException("Sequence padding must be between 1 and 10");
        }

        // Check for invalid tokens
        String testTemplate = template;
        testTemplate = matcher.replaceAll("SEQ");
        testTemplate = testTemplate.replaceAll("\\{(YYYY|YY|MM|M|DEPT|DEPT_NAME)}", "");

        if (testTemplate.contains("{") || testTemplate.contains("}")) {
            throw new ValidationException("Template contains invalid tokens. Valid: {SEQ:N}, {YYYY}, {YY}, {MM}, {M}, {DEPT}, {DEPT_NAME}");
        }
    }

    /**
     * Generate preview of template with example values.
     * Used for UI live preview.
     *
     * @param template Template to preview
     * @return Example invoice number (e.g., "001-02-2026")
     */
    public String preview(String template) {
        validateTemplate(template);

        TemplateContext exampleContext = TemplateContext.builder()
            .sequenceNumber(1)
            .year(2026)
            .month(2) // February for example
            .department(null) // No department in example
            .build();

        return apply(template, exampleContext);
    }
}
