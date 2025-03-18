package dev.robgro.timesheet.controller.app;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.OperationResult;
import dev.robgro.timesheet.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientViewController {
    private final ClientService clientService;

    @GetMapping
    public String showClientList(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "clients";
    }

    @GetMapping("/new")
    public String showNewClientForm(Model model) {
        model.addAttribute("client", new ClientDto(
                null,        // id
                "",             // clientName
                0.0,            // hourlyRate
                0L,             // houseNo (long)
                "",             // streetName
                "",             // city
                "",             // postCode
                "",             // email
                true            // is active
        ));
        return "client-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditClientForm(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.getClientById(id));
        return "client-form";
    }

    @PostMapping("/save")
    public String saveClient(@ModelAttribute ClientDto client) {
        clientService.saveClient(client);
        return "redirect:/clients";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        OperationResult result = clientService.deactivateClient(id);
        redirectAttributes.addFlashAttribute(result.success() ? "success" : "error", result.message());
        return "redirect:/clients";
    }
}
