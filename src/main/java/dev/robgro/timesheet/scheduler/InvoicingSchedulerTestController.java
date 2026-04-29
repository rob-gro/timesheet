package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.seller.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test/scheduler")
@RequiredArgsConstructor
@Profile("!prod")  // Działa TYLKO jeśli NIE jest profil prod
public class InvoicingSchedulerTestController {

    private final InvoicingTaskService invoicingTaskService;
    private final SellerRepository sellerRepository;
    private final RunStatusService runStatusService;
    private final RunStatusWriteService runStatusWriteService;

    @GetMapping("/trigger-invoicing")
    public InvoicingSummary triggerMonthlyInvoicing(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        Long resolvedSellerId = sellerId != null ? sellerId
                : sellerRepository.findByIsSystemDefaultTrue()
                        .or(() -> sellerRepository.findByActiveTrue().stream().findFirst())
                        .map(s -> s.getId())
                        .orElseThrow(() -> new IllegalStateException("No active seller found"));

        YearMonth period = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now().minusMonths(1);

        String runId = UUID.randomUUID().toString();
        runStatusWriteService.createRun(runId, "manual");

        return invoicingTaskService.executeMonthlyInvoicing(resolvedSellerId, period, runId);
    }

    @GetMapping("/run-status/{runId}")
    public RunStatusDto getRunStatus(@PathVariable String runId) {
        return runStatusService.getRunStatus(runId);
    }
}
