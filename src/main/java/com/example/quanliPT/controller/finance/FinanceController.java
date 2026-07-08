package com.example.quanliPT.controller.finance;

import com.example.quanliPT.model.BusinessExpense;
import com.example.quanliPT.repository.finance.BusinessExpenseRepository;
import com.example.quanliPT.service.finance.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final BusinessExpenseRepository expenseRepository;

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyReport(@RequestParam(defaultValue = "2026") int year) {
        return ResponseEntity.ok(financeService.getMonthlyReport(year));
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BusinessExpense>> getAllExpenses() {
        return ResponseEntity.ok(expenseRepository.findAll());
    }

    @PostMapping("/expenses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessExpense> createExpense(@RequestBody BusinessExpense expense) {
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @PutMapping("/expenses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessExpense> updateExpense(
            @PathVariable Long id,
            @RequestBody BusinessExpense expenseDetails
    ) {
        BusinessExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        expense.setDescription(expenseDetails.getDescription());
        expense.setAmount(expenseDetails.getAmount());
        expense.setExpenseDate(expenseDetails.getExpenseDate());
        expense.setCategory(expenseDetails.getCategory());
        expense.setNotes(expenseDetails.getNotes());

        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @DeleteMapping("/expenses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        BusinessExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expenseRepository.delete(expense);
        return ResponseEntity.noContent().build();
    }
}



