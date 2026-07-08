package com.example.quanliPT.service.admin;

import com.example.quanliPT.model.*;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.model.enums.RoomStatus;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;

import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;
import com.example.quanliPT.model.*;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.model.enums.RoomStatus;
 
import com.example.quanliPT.dto.admin.DashboardDTO;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;

    public DashboardDTO getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalRooms = roomRepository.count();
        long totalContracts = contractRepository.count();
        
        long availableRooms = roomRepository.countByStatus(RoomStatus.AVAILABLE);
        long occupiedRooms = roomRepository.countByStatus(RoomStatus.OCCUPIED);
        
        long unpaidInvoices = invoiceRepository.countByStatus(InvoiceStatus.UNPAID);
        long pendingContracts = contractRepository.countByStatus(ContractStatus.PENDING);

        // Sum of all PAID invoices
        BigDecimal totalRevenue = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .map(i -> i.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardDTO.builder()
                .totalUsers(totalUsers)
                .totalRooms(totalRooms)
                .totalContracts(totalContracts)
                .totalRevenue(totalRevenue)
                .availableRooms(availableRooms)
                .occupiedRooms(occupiedRooms)
                .unpaidInvoices(unpaidInvoices)
                .pendingContracts(pendingContracts)
                .monthlyRevenue(new ArrayList<>())
                .build();
    }

    public Map<String, Object> getReportStats(LocalDateTime start, LocalDateTime end) {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getBillingDate().isAfter(start) && i.getBillingDate().isBefore(end))
                .collect(Collectors.toList());

        long totalInvoices = invoices.size();
        long paidInvoices = invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        BigDecimal revenue = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("totalOrders", totalInvoices);
        report.put("completedOrders", paidInvoices);
        report.put("totalRevenue", revenue);
        report.put("pendingOrders", totalInvoices - paidInvoices);
        
        // Mocking top products by revenue (as rooms)
        report.put("topProductsReport", new ArrayList<>());

        return report;
    }
}





