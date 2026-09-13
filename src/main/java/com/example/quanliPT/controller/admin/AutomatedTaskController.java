package com.example.quanliPT.controller.admin;

import com.example.quanliPT.service.task.AutomatedTaskService;
import com.example.quanliPT.service.task.BillingScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/automated-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AutomatedTaskController {

    private final AutomatedTaskService automatedTaskService;
    private final BillingScheduler billingScheduler;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTaskStatus() {
        return ResponseEntity.ok(billingScheduler.getStatus());
    }

    @PostMapping("/run-overdue-check")
    public ResponseEntity<Map<String, String>> runOverdueCheck() {
        automatedTaskService.checkOverdueInvoices();
        return ResponseEntity.ok(Map.of("message", "Đã kích hoạt quét hóa đơn quá hạn thành công!"));
    }

    @PostMapping("/run-expired-contracts-check")
    public ResponseEntity<Map<String, String>> runExpiredContractsCheck() {
        automatedTaskService.checkExpiredContracts();
        return ResponseEntity.ok(Map.of("message", "Đã kích hoạt quét hợp đồng hết hạn thành công!"));
    }

    @PostMapping("/run-monthly-billing")
    public ResponseEntity<Map<String, String>> runMonthlyBilling(@RequestParam(defaultValue = "false") boolean force) {
        billingScheduler.runMonthlyBilling();
        return ResponseEntity.ok(Map.of("message", "Đã kích hoạt tiến trình lập hóa đơn tự động thành công!"));
    }
}
