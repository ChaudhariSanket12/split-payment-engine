package com.example.splitpay.reconciliation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

    /**
     * Runs daily at 2:00 AM.
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledReconciliation() {
        log.info("Scheduled reconciliation triggered at 2 AM");
        reconciliationService.runReconciliation();
    }

    /**
     * Also runs every 6 hours for near-real-time discrepancy detection.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    public void periodicReconciliation() {
        log.info("Periodic reconciliation triggered (every 6h)");
        reconciliationService.runReconciliation();
    }
}
