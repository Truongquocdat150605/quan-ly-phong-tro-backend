package com.example.quanliPT.service.finance;

import com.example.quanliPT.repository.finance.BusinessExpenseRepository;
import com.example.quanliPT.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final InvoiceRepository invoiceRepository;
    private final BusinessExpenseRepository expenseRepository;

    public List<Map<String, Object>> getMonthlyReport(int year) {
        List<Map<String, Object>> report = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.plusMonths(1).minusDays(1);

            // Revenue from Paid Invoices
            BigDecimal revenue = invoiceRepository.findByBillingDateBetween(
                    start.atStartOfDay(),
                    end.atTime(23, 59, 59)
            ).stream()
                    .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Expenses
            BigDecimal expenses = expenseRepository.findByExpenseDateBetween(start, end)
                    .stream()
                    .map(exp -> exp.getAmount() != null ? exp.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal profit = revenue.subtract(expenses);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("name", "Tháng " + month);
            monthData.put("revenue", revenue);
            monthData.put("expenses", expenses);
            monthData.put("profit", profit);
            report.add(monthData);
        }

        return report;
    }
}


