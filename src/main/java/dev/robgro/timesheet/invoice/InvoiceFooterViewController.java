package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.seller.SellerDto;
import dev.robgro.timesheet.seller.SellerService;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/settings/invoice-footer")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@RequiredArgsConstructor
public class InvoiceFooterViewController {

    private final SellerService sellerService;
    private final UserService userService;

    @GetMapping
    public String showSettings(Model model, Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        Long sellerId = currentUser.getDefaultSeller().getId();
        SellerDto seller = sellerService.getSellerById(sellerId);

        model.addAttribute("seller", seller);
        model.addAttribute("currentUser", currentUser);

        return "settings/invoice-footer";
    }

    @PostMapping
    public String saveSettings(
            @RequestParam(required = false) String website,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        try {
            User currentUser = userService.findByUsername(authentication.getName());
            Long sellerId = currentUser.getDefaultSeller().getId();

            sellerService.updateFooterSettings(sellerId, website, email, phone);

            redirectAttributes.addFlashAttribute("success", "Footer settings saved successfully!");

            log.info("User {} updated invoice footer settings for seller id {}",
                    authentication.getName(), sellerId);

        } catch (Exception e) {
            log.error("Error saving invoice footer settings", e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to save settings: " + e.getMessage());
        }

        return "redirect:/settings/invoice-footer";
    }
}
