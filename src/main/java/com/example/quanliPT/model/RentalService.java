package com.example.quanliPT.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.example.quanliPT.model.enums.ServiceCategory;
import com.example.quanliPT.model.enums.ServiceFrequency;
import com.example.quanliPT.model.enums.ServiceUnit;

@Entity
@Table(name = "rental_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private ServiceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit")
    private ServiceUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency")
    private ServiceFrequency frequency;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
