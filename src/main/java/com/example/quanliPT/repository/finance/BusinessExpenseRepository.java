package com.example.quanliPT.repository.finance;

import com.example.quanliPT.model.BusinessExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BusinessExpenseRepository extends JpaRepository<BusinessExpense, Long> {
    List<BusinessExpense> findByExpenseDateBetween(LocalDate start, LocalDate end);
}

