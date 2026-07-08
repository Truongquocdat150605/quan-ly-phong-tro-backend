package com.example.quanliPT.model;

import com.example.quanliPT.model.enums.ContractStatus;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "contracts")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "rent_price", nullable = false)
    private BigDecimal rentPrice; // Giá thuê thỏa thuận lúc ký hợp đồng
    
    @Column(nullable = false)
    private BigDecimal deposit; // Tiền đặt cọc

    @Enumerated(EnumType.STRING)
    private ContractStatus status = ContractStatus.ACTIVE;
    
    @Builder.Default
    private boolean active = true;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @Column(name = "is_expired_notified", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isExpiredNotified = false;
}

