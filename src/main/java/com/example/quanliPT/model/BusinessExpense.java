package com.example.quanliPT.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "business_expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessExpense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate expenseDate;

    private String category; // e.g., "Repair", "Tax", "Utility Bulk", "Salary"
    
    private String notes;
}
