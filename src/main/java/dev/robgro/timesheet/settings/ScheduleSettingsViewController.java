package dev.robgro.timesheet.settings;

import dev.robgro.timesheet.scheduler.InvoicingSummary;
import dev.robgro.timesheet.scheduler.InvoicingTaskService;
import dev.robgro.timesheet.scheduler.RunStatusWriteService;
import dev.robgro.timesheet.scheduler.SaveScheduleConfigRequest;
import dev.robgro.timesheet.scheduler.SellerScheduleConfigDto;
import dev.robgro.timesheet.scheduler.SellerScheduleConfigService;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/settings/invoicing-schedule")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ScheduleSettingsViewController {

    private final SellerScheduleConfigService configService;
    private final UserService userService;
    private final Clock clock;
    private final InvoicingTaskService invoicingTaskService;
    private final RunStatusWriteService runStatusWriteService;

    private static final List<String> COMMON_TIMEZONES = List.of(
            "UTC",
            "Europe/London", "Europe/Warsaw", "Europe/Berlin", "Europe/Paris",
            "Europe/Amsterdam", "Europe/Rome", "Europe/Madrid", "Europe/Stockholm",
            "Europe/Helsinki", "Europe/Bucharest", "Europe/Athens", "Europe/Istanbul",
            "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
            "America/Toronto", "America/Sao_Paulo", "America/Argentina/Buenos_Aires",
            "Asia/Dubai", "Asia/Kolkata", "Asia/Singapore", "Asia/Tokyo", "Asia/Shanghai",
            "Australia/Sydney", "Pacific/Auckland"
    );

    @GetMapping
    public String showSchedule(Model model, Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("timezones", COMMON_TIMEZONES);

        if (currentUser == null || currentUser.getDefaultSeller() == null) {
            model.addAttribute("error", "No seller assigned. Please set a default seller first.");
            return "settings/invoicing-schedule";
        }

        Long sellerId = currentUser.getDefaultSeller().getId();
        SellerScheduleConfigDto config = configService.getConfigForSeller(sellerId);
        model.addAttribute("config", config);

        boolean overdue = config.nextRunAt() != null
                && config.nextRunAt().isBefore(ZonedDateTime.now(clock));
        model.addAttribute("nextRunOverdue", overdue);

        return "settings/invoicing-schedule";
    }

    @PostMapping("/save")
    public String saveSchedule(
            @RequestParam int scheduledDay,
            @RequestParam int scheduledHour,
            @RequestParam String timezone,
            @RequestParam(required = false) boolean enabled,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(authentication.getName());
            if (currentUser == null || currentUser.getDefaultSeller() == null) {
                redirectAttributes.addFlashAttribute("error", "No seller assigned.");
                return "redirect:/settings/invoicing-schedule";
            }

            Long sellerId = currentUser.getDefaultSeller().getId();
            SaveScheduleConfigRequest request = new SaveScheduleConfigRequest(
                    scheduledDay, scheduledHour, timezone, enabled);
            configService.saveConfig(sellerId, request);

            redirectAttributes.addFlashAttribute("success", "Invoicing schedule saved successfully!");
            log.info("Saved invoicing schedule for seller {}: day={}, hour={}, tz={}, enabled={}",
                    sellerId, scheduledDay, scheduledHour, timezone, enabled);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid input: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to save invoicing schedule", e);
            redirectAttributes.addFlashAttribute("error", "Save failed: " + e.getMessage());
        }
        return "redirect:/settings/invoicing-schedule";
    }

    @PostMapping("/trigger")
    public String triggerNow(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(authentication.getName());
            if (currentUser == null || currentUser.getDefaultSeller() == null) {
                redirectAttributes.addFlashAttribute("error", "No seller assigned.");
                return "redirect:/settings/invoicing-schedule";
            }
            Long sellerId = currentUser.getDefaultSeller().getId();
            YearMonth period = YearMonth.now().minusMonths(1);
            String runId = UUID.randomUUID().toString();
            runStatusWriteService.createRun(runId, "manual");
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(sellerId, period, runId);
            redirectAttributes.addFlashAttribute("success",
                    "Trigger complete: " + summary.successfulInvoices() + " invoices created, "
                    + summary.failedInvoices() + " failed (period: " + period + ")");
            log.info("Manual trigger by {} for seller {}: period={}, success={}, failed={}",
                    authentication.getName(), sellerId, period,
                    summary.successfulInvoices(), summary.failedInvoices());
        } catch (Exception e) {
            log.error("Manual trigger failed for user {}", authentication.getName(), e);
            redirectAttributes.addFlashAttribute("error", "Trigger failed: " + e.getMessage());
        }
        return "redirect:/settings/invoicing-schedule";
    }
}