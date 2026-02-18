package dev.robgro.timesheet.invoice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts database constraint name from DataIntegrityViolationException.
 * MariaDB error format: "Duplicate entry '...' for key '<constraint_name>'"
 */
final class ConstraintNameExtractor {

    private static final Pattern KEY_PATTERN = Pattern.compile("for key '([^']+)'");

    private ConstraintNameExtractor() {}

    static String tryGetConstraintName(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null) {
                Matcher m = KEY_PATTERN.matcher(msg);
                if (m.find()) return m.group(1);
            }
            t = t.getCause();
        }
        return null;
    }
}