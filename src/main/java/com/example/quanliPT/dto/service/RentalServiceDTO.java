package com.example.quanliPT.dto.service;

import lombok.*;
import java.math.BigDecimal;
import com.example.quanliPT.model.enums.ServiceCategory;
import com.example.quanliPT.model.enums.ServiceFrequency;
import com.example.quanliPT.model.enums.ServiceUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalServiceDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private ServiceCategory category;
    private ServiceUnit unit;
    private ServiceFrequency frequency;
    private String description;
    private boolean active;
}

