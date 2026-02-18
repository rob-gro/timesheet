package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * View controller for invoice numbering settings pages.
 * Provides Thymeleaf-based UI for managing numbering schemes.
 */
@Slf4j
@Controller
@RequestMapping("/settings/invoice-numbering")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@RequiredArgsConstructor
public class InvoiceNumberingViewController {

    private final InvoiceNumberingSchemeService schemeService;
    private final UserService userService;

    @GetMapping
    public String showSettings(Model model, Authentication authentication) {
        // Get all schemes for current seller (for history view)
        model.addAttribute("schemes", schemeService.getAllSchemes());

        // Get active schemes
        model.addAttribute("activeSchemes", schemeService.getActiveSchemes());

        // Add empty form object for creating new scheme
        model.addAttribute("newScheme", new CreateSchemeRequest("", ResetPeriod.MONTHLY, null));

        // Add reset period options for dropdown
        model.addAttribute("resetPeriods", ResetPeriod.values());

        // Add current user info
        if (authentication != null) {
            User currentUser = userService.findByUsername(authentication.getName());
            model.addAttribute("currentUser", currentUser);
        }

        return "settings/invoice-numbering";
    }

    @PostMapping("/create")
    public String createScheme(
            @Valid @ModelAttribute("newScheme") CreateSchemeRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (result.hasErrors()) {
            // Redirect back with errors
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newScheme", result);
            redirectAttributes.addFlashAttribute("newScheme", request);
            redirectAttributes.addFlashAttribute("error", "Please correct the errors in the form");
            return "redirect:/settings/invoice-numbering";
        }

        try {
            // createdBy is automatically set by Spring Data JPA Auditing
            schemeService.createScheme(request);

            redirectAttributes.addFlashAttribute("success",
                "Invoice numbering scheme created successfully!");

            log.info("User {} created new invoice numbering scheme: template={}",
                authentication.getName(), request.template());

        } catch (Exception e) {
            log.error("Error creating invoice numbering scheme", e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to create scheme: " + e.getMessage());
        }

        return "redirect:/settings/invoice-numbering";
    }

    @PostMapping("/archive/{id}")
    public String archiveScheme(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        try {
            schemeService.archiveScheme(id);

            redirectAttributes.addFlashAttribute("success",
                "Invoice numbering scheme archived successfully");

            log.info("User {} archived invoice numbering scheme: id={}",
                authentication.getName(), id);

        } catch (Exception e) {
            log.error("Error archiving invoice numbering scheme", e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to archive scheme: " + e.getMessage());
        }

        return "redirect:/settings/invoice-numbering";
    }
}
