package dev.robgro.timesheet.annotation;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Marks a method as an I/O boundary — no active transaction is allowed when entering.
 *
 * <p>Carries {@code @Transactional(NOT_SUPPORTED)} as meta-annotation, suspending any active TX.
 * {@link IoBoundaryAspect} additionally throws {@code IllegalStateException} when TX is active —
 * fail-fast instead of silent suspension.
 *
 * <p>Apply to methods performing I/O (PDF generation, FTP upload, SMTP send)
 * that must NOT hold a DB connection during the operation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public @interface IoBoundary {
}