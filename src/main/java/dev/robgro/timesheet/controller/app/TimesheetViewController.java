package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.ClientService;
import dev.robgro.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("timesheet", new TimesheetDto(null, null, null, 0.5, false, null, 0.0, null));
        return "timesheet";
    }

//    @GetMapping("/list")
//    public String showTimesheets(
//            Model model,
//            @RequestParam(required = false) Long clientId,
//            @RequestParam(required = false, defaultValue = "serviceDate") String sortBy,
//            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
//
//        List<TimesheetDto> timesheets = timesheetService.searchAndSortTimesheets(clientId, sortBy, sortDir);
//        model.addAttribute("timesheets", timesheets);
//        model.addAttribute("clients", clientService.getAllClients());
//        return "timesheet-list";
//    }

//    @GetMapping("/list")
//    public String showTimesheets(Model model,
//                                 @RequestParam(required = false) Long clientId,
//                                 @RequestParam(required = false, defaultValue = "serviceDate") String sortBy,
//                                 @RequestParam(required = false, defaultValue = "asc") String sortDir,
//                                 @RequestParam(defaultValue = "0") int page,
//                                 @RequestParam(defaultValue = "10") int size) {
//
//        Page<TimesheetDto> timesheets;
//        if ("invoiceNumber".equals(sortBy)) {
//            timesheets = timesheetService.getAllTimesheetsSortedByInvoiceNumber(clientId, PageRequest.of(page, size));
//        } else {
//            timesheets = timesheetService.getAllTimesheetsPageable(clientId,
//                    PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy));
//        }
//
//        model.addAttribute("timesheets", timesheets.getContent());
//        model.addAttribute("currentPage", page);
//        model.addAttribute("totalPages", timesheets.getTotalPages());
//        model.addAttribute("totalItems", timesheets.getTotalElements());
//        model.addAttribute("size", size);
//        model.addAttribute("clients", clientService.getAllClients());
//
//        return "timesheet-list";
//    }

    @GetMapping("/list")
    public String showTimesheets(
            Model model,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false, defaultValue = "serviceDate") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.Direction.fromString(sortDir),
                sortBy.equals("invoiceNumber") ? "serviceDate" : sortBy);

        Page<TimesheetDto> timesheets;
        if (clientId != null) {
            timesheets = timesheetService.getTimesheetsByClientIdPageable(clientId, pageRequest);
        } else {
            timesheets = timesheetService.getAllTimesheetsPageable(pageRequest);
        }

        model.addAttribute("timesheets", timesheets.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", timesheets.getTotalPages());
        model.addAttribute("totalItems", timesheets.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("clients", clientService.getAllClients());

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
