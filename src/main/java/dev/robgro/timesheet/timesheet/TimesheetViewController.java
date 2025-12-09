package dev.robgro.timesheet.timesheet;

import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/timesheets")
@RequiredArgsConstructor
public class TimesheetViewController {

    private final ClientService clientService;
    private final TimesheetService timesheetService;

    @GetMapping("/new")
    public String showTimesheetForm(Model model) {
        log.debug("Showing timesheet form");
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("timesheet", timesheetService.createEmptyTimesheetDto());
        return "timesheets/form";
    }

    @GetMapping("/list")
    public String showTimesheets(
            Model model,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false, defaultValue = "serviceDate") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Showing timesheets list with filters: clientId={}, paymentStatus={}, sortBy={}, sortDir={}, page={}, size={}",
                clientId, paymentStatus, sortBy, sortDir, page, size);

        Page<TimesheetDto> timesheets = timesheetService.getFilteredAndPaginatedTimesheets(
                clientId, paymentStatus, sortBy, sortDir, page, size);

        model.addAttribute("timesheets", timesheets.getContent());
        PaginationUtils.setPaginationAttributesWithSort(model, timesheets, page, size, sortBy, sortDir);
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("clientId", clientId);
        model.addAttribute("paymentStatus", paymentStatus);
        return "timesheets/list";
    }

    @GetMapping("/filter/{clientId}")
    @ResponseBody
    public List<TimesheetDto> getFilteredTimesheets(@PathVariable Long clientId) {
        log.debug("Getting filtered timesheets for client ID: {}", clientId);
        return timesheetService.getTimesheetByClientId(clientId);
    }

    @PostMapping("/new")
    public String handleTimesheetSubmit(
            @Valid @ModelAttribute TimesheetDto timesheet,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "timesheets/form";
        }

        timesheetService.createTimesheet(
                timesheet.clientId(),
                timesheet.serviceDate(),
                timesheet.duration(),
                null);

        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/timesheets/form";
    }

    @GetMapping("/view/{id}")
    public String viewTimesheet(@PathVariable Long id, Model model) {
        log.debug("Viewing timesheet ID: {}", id);
        model.addAttribute("timesheet", timesheetService.getTimesheetById(id));
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("readOnly", true);
        return "timesheets/form";
    }

    @GetMapping("/edit/{id}")
    public String editTimesheet(@PathVariable Long id, Model model) {
        log.debug("Editing timesheet ID: {}", id);
        model.addAttribute("timesheet", timesheetService.getTimesheetById(id));
        model.addAttribute("clients", clientService.getAllClients());
        return "timesheets/form";
    }

    @PostMapping("/edit/{id}")
    public String updateTimesheet(
            @PathVariable Long id,
            @Valid @ModelAttribute TimesheetDto timesheet,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "timesheets/form";
        }

        timesheetService.updateTimesheet(
                id,
                timesheet.clientId(),
                timesheet.serviceDate(),
                timesheet.duration(),
                null);

        redirectAttributes.addFlashAttribute("success", "Timesheet updated successfully");
        return "redirect:/timesheets/list";
    }
}
