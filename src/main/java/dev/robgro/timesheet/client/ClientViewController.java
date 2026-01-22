package dev.robgro.timesheet.client;

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
@RequestMapping("/clients")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@RequiredArgsConstructor
public class ClientViewController {
    private final ClientService clientService;

    @GetMapping
    public String showClientList(Model model, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("clients", clientService.getAllClients(isAdmin));
        return "clients/list";
    }

    @GetMapping("/new")
    public String showNewClientForm(Model model) {
        model.addAttribute("client", clientService.createEmptyClientDto());
        return "clients/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditClientForm(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.getClientById(id));
        return "clients/form";
    }

    @PostMapping("/save")
    public String saveClient(@Valid @ModelAttribute ClientDto client,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "clients/form";
        }
        clientService.saveClient(client);
        redirectAttributes.addFlashAttribute("success", "Client saved successfully");
        return "redirect:/clients";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Deactivating client ID: {}", id);
        OperationResult result = clientService.deactivateClient(id);
        redirectAttributes.addFlashAttribute(result.success() ? "success" : "error", result.message());
        return "redirect:/clients";
    }

    @PostMapping("/{id}/activate")
    public String activateClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        OperationResult result = clientService.setActiveStatus(id, true);
        redirectAttributes.addFlashAttribute(result.success() ? "success" : "error", result.message());
        return "redirect:/clients";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        OperationResult result = clientService.setActiveStatus(id, false);
        redirectAttributes.addFlashAttribute(result.success() ? "success" : "error", result.message());
        return "redirect:/clients";
    }
}
