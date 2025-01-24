package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/timesheets")
@RequiredArgsConstructor
public class TimesheetViewController {

    private final ClientService clientService;
    private final TimesheetService timesheetService;

    @GetMapping("/new")
    public String showTimesheetForm(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("timesheet", new TimesheetDto(null, null, null, 0.5, false, null, 0.0, null));
        return "timesheet";
    }

    @GetMapping("/list")
    public String showTimesheets(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("timesheets", timesheetService.getAllTimesheets());
        return "timesheet-list";
    }

    @GetMapping("/filter/{clientId}")
    public List<TimesheetDto> getFilteredTimesheets(@PathVariable Long clientId) {
        return timesheetService.getTimesheetByClientId(clientId);
    }

    @PostMapping("/new")
    public String handleTimesheetSubmit(
            @ModelAttribute TimesheetDto timesheet,
            RedirectAttributes redirectAttributes) {
        timesheetService.createTimesheet(
                timesheet.clientId(),
                timesheet.serviceDate(),
                timesheet.duration());

        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/timesheets";
    }

    @GetMapping("/view/{id}")
    public String viewTimesheet(@PathVariable Long id, Model model) {
        model.addAttribute("timesheet", timesheetService.getTimesheetById(id));
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("readOnly", true);
        return "timesheet";
    }

    @GetMapping("/edit/{id}")
    public String editTimesheet(@PathVariable Long id, Model model) {
        model.addAttribute("timesheet", timesheetService.getTimesheetById(id));
        model.addAttribute("clients", clientService.getAllClients());
        return "timesheet";
    }
}
