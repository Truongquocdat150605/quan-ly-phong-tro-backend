package com.example.quanliPT.dto.auth;

import lombok.Data;

@Data
public class VerifyTokenRequest {
    private String token;
    private String email;
}

