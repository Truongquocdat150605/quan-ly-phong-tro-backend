package com.example.quanliPT.model;

import com.example.quanliPT.model.enums.RoomStatus;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomNumber;

    @Column(nullable = false)
    private String type; // VIP, Ordinary, etc.

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private double area;
    private String description;
    private String image;
    @Column(nullable = true) // có thể null vì phòng cũ chưa có dữ liệu
    private Integer floor;

    @ManyToMany
    @JoinTable(name = "room_services_mapping", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    @Builder.Default
    private Set<RentalService> services = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private RoomStatus status = RoomStatus.AVAILABLE;
}
