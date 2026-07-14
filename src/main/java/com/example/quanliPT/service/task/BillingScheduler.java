package com.example.quanliPT.service.task;

import com.example.quanliPT.service.finance.BillingService;
import com.example.quanliPT.service.finance.BillingService.BillingRunResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingScheduler {

    private final BillingService billingService;

    @Value("${app.billing.monthly-cron:0 0 0 1 * ?}")
    private String cronExpression;

    @Value("${app.billing.scheduler-force-create:false}")
    private boolean schedulerForceCreate;

    @Getter
    private LocalDateTime lastRunAt;
    @Getter
    private BillingRunResult lastResult;
    @Getter
    private String lastError;

    @Scheduled(cron = "${app.billing.monthly-cron:0 0 0 1 * ?}")
    public void runMonthlyBilling() {
        lastRunAt = LocalDateTime.now();
        lastError = null;
        try {
            lastResult = billingService.generateMonthlyInvoices(schedulerForceCreate);
            log.info("Scheduled billing run finished at {} with result {}", lastRunAt, lastResult);
        } catch (Exception e) {
            lastError = e.getMessage();
            log.error("Scheduled billing run failed at {}: {}", lastRunAt, e.getMessage(), e);
        }
    }

    public Map<String, Object> getStatus() {
        return Map.of(
                "cron", cronExpression,
                "forceCreate", schedulerForceCreate,
                "lastRunAt", lastRunAt != null ? lastRunAt : "",
                "created", lastResult != null ? lastResult.created() : 0,
                "skipped", lastResult != null ? lastResult.skipped() : 0,
                "failed", lastResult != null ? lastResult.failed() : 0,
                "lastError", lastError != null ? lastError : ""
        );
    }
}
