package com.example.quanliPT.model;

import com.example.quanliPT.model.enums.RentalRequestStatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(name = "identity_number")
    private String identityNumber;

    private LocalDate desiredMoveInDate;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RentalRequestStatus status = RentalRequestStatus.PENDING;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "user_id")
    private Long userId;
}
