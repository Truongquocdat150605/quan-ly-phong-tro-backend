package com.example.quanliPT.service.task;

import com.example.quanliPT.service.finance.BillingService;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private final BillingService billingService;

    // Run at 00:00 on the 1st day of every month
    @Scheduled(cron = "0 0 0 1 * ?")
    public void runMonthlyBilling() {
        billingService.generateMonthlyInvoices();
    }
}


