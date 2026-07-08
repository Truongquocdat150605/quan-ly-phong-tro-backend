package com.example.quanliPT.model;

import com.example.quanliPT.model.enums.InvoiceStatus;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false)
    private BigDecimal rentalAmount;
    
    // Electricity
    @Column(name = "electricity_start")
    private Double electricityStart;

    @Column(name = "electricity_end")
    private Double electricityEnd;

    @Column(name = "electricity_price")
    private BigDecimal electricityPrice;

    @Column(name = "electricity_amount")
    private BigDecimal electricityAmount;

    // Water
    @Column(name = "water_start")
    private Double waterStart;

    @Column(name = "water_end")
    private Double waterEnd;

    @Column(name = "water_price")
    private BigDecimal waterPrice;

    @Column(name = "water_amount")
    private BigDecimal waterAmount;
    
    private BigDecimal serviceAmount; // Wifi, Cleaning, etc.
    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "billing_date")
    private LocalDateTime billingDate;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;
    
    @Column(name = "is_overdue_notified", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isOverdueNotified = false;
    
    private String notes;
}
