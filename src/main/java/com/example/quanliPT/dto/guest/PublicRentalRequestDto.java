package com.example.quanliPT.dto.guest;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Data
public class PublicRentalRequestDto {
    @NotNull
    private Long roomId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    private String email;

    private String identityNumber;
    private LocalDate desiredMoveInDate;
    private String note;
    private Long userId;
}

