package dev.robgro.timesheet.seller;

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

@Slf4j
@Controller
@RequestMapping("/sellers")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@RequiredArgsConstructor
public class SellerViewController {
    private final SellerService sellerService;
    private final UserService userService;

    @GetMapping
    public String showSellerList(Model model, Authentication authentication) {
        // ADMIN sees all (active + inactive), USER sees only active
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("sellers", sellerService.getAllSellers(isAdmin));

        // Add current user's default seller info
        if (authentication != null) {
            User currentUser = userService.findByUsername(authentication.getName());
            model.addAttribute("currentUser", currentUser);
        }
        return "sellers/list";
    }

    @GetMapping("/new")
    public String showNewSellerForm(Model model) {
        model.addAttribute("seller", sellerService.createEmptySellerDto());
        return "sellers/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditSellerForm(@PathVariable Long id, Model model) {
        model.addAttribute("seller", sellerService.getSellerById(id));
        return "sellers/form";
    }

    @PostMapping("/save")
    public String saveSeller(@Valid @ModelAttribute SellerDto seller,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "sellers/form";
        }
        sellerService.saveSeller(seller);
        redirectAttributes.addFlashAttribute("success", "Seller saved successfully");
        return "redirect:/sellers";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteSeller(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Deactivating seller ID: {}", id);
        OperationResult result = sellerService.deactivateSeller(id);
        redirectAttributes.addFlashAttribute(result.success() ? "success" : "error", result.message());
        return "redirect:/sellers";
    }

    @PostMapping("/set-default/{id}")
    public String setDefaultSeller(@PathVariable Long id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        log.info("Setting default seller ID: {} for user: {}", id, authentication.getName());

        try {
            User currentUser = userService.findByUsername(authentication.getName());
            userService.setDefaultSeller(currentUser.getId(), id);
            redirectAttributes.addFlashAttribute("success", "Default seller updated successfully");
        } catch (Exception e) {
            log.error("Failed to set default seller", e);
            redirectAttributes.addFlashAttribute("error", "Failed to set default seller: " + e.getMessage());
        }
        return "redirect:/sellers";
    }

    @PostMapping("/set-system-default/{id}")
    public String setSystemDefaultSeller(@PathVariable Long id,
                                         RedirectAttributes redirectAttributes) {
        log.info("Setting system default seller ID: {}", id);

        try {
            SellerDto seller = sellerService.getSellerById(id);
            SellerDto updatedSeller = new SellerDto(
                    seller.id(),
                    seller.name(),
                    seller.street(),
                    seller.postcode(),
                    seller.city(),
                    seller.serviceDescription(),
                    seller.bankName(),
                    seller.accountNumber(),
                    seller.sortCode(),
                    seller.email(),
                    seller.phone(),
                    seller.companyRegistrationNumber(),
                    seller.legalForm(),
                    seller.vatNumber(),
                    seller.taxId(),
                    seller.active(),
                    true // systemDefault = true
            );
            sellerService.saveSeller(updatedSeller);
            redirectAttributes.addFlashAttribute("success", "System default seller updated successfully");
        } catch (Exception e) {
            log.error("Failed to set system default seller", e);
            redirectAttributes.addFlashAttribute("error", "Failed to set system default seller: " + e.getMessage());
        }
        return "redirect:/sellers";
    }

    @PostMapping("/{id}/activate")
    public String activateSeller(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        log.info("Activating seller ID: {}", id);
        OperationResult result = sellerService.setActiveStatus(id, true);
        redirectAttributes.addFlashAttribute(
                result.success() ? "success" : "error",
                result.message()
        );
        return "redirect:/sellers";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateSeller(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        log.info("Deactivating seller ID: {}", id);
        OperationResult result = sellerService.setActiveStatus(id, false);
        redirectAttributes.addFlashAttribute(
                result.success() ? "success" : "error",
                result.message()
        );
        return "redirect:/sellers";
    }
}
