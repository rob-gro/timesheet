package dev.robgro.timesheet.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test/scheduler")
@RequiredArgsConstructor
@Profile("!prod")  // Działa TYLKO jeśli NIE jest profil prod
public class InvoicingSchedulerTestController {

    private final InvoicingTaskService invoicingTaskService;

    @GetMapping("/trigger-invoicing")
    public InvoicingSummary triggerMonthlyInvoicing() {
        return invoicingTaskService.executeMonthlyInvoicing();
    }
}
