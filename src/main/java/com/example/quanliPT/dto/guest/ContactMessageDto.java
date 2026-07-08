package com.example.quanliPT.dto.guest;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ContactMessageDto {
    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    @NotBlank
    private String message;
}

