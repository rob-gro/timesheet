package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * View controller for admin invoice counter health page.
 * Renders HTML shell; data loaded via JS fetch to /internal/invoice-counters.
 */
@Controller
@RequestMapping("/admin/invoice-counters")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class InvoiceCounterObservabilityViewController {

    @GetMapping
    public String showCounterHealth(
            @RequestParam(required = false) Long sellerId,
            Model model) {
        // Prefill from query param (?sellerId=123) or fall back to current user's seller
        Long defaultSellerId = sellerId != null ? sellerId : SecurityUtils.getCurrentSellerId();
        model.addAttribute("defaultSellerId", defaultSellerId);
        return "admin/invoice-counters";
    }
}