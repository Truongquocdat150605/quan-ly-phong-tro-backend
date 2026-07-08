package com.example.quanliPT.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private long totalUsers;
    private long totalRooms;
    private long totalContracts;
    private BigDecimal totalRevenue;
    private long availableRooms;
    private long occupiedRooms;
    private long unpaidInvoices;
    private long pendingContracts;

    // Monthly revenue statistics for charts
    private List<Map<String, Object>> monthlyRevenue;

    // Recent activity or top items if needed
    private List<Map<String, Object>> topTenants;
}

