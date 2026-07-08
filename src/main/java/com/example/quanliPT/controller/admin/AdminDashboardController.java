package com.example.quanliPT.controller.admin;

import com.example.quanliPT.dto.common.ApiResponse;
import com.example.quanliPT.dto.admin.DashboardDTO;
import com.example.quanliPT.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getStats() {
        try {
            DashboardDTO stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(ApiResponse.success("✅ Lấy dữ liệu thống kê thành công", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("❌ Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/report-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getReport(
            @RequestParam String start,
            @RequestParam String end) {
        try {
            LocalDateTime startTime = LocalDateTime.parse(start);
            LocalDateTime endTime = LocalDateTime.parse(end);
            return ResponseEntity.ok(ApiResponse.success("✅ Lấy báo cáo thành công",
                    dashboardService.getReportStats(startTime, endTime)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("❌ Lỗi định dạng ngày: " + e.getMessage()));
        }
    }
}



