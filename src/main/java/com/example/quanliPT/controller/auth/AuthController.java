package com.example.quanliPT.controller.auth;

import com.example.quanliPT.dto.auth.AuthResponse;
import com.example.quanliPT.dto.auth.ChangePasswordRequest;
import com.example.quanliPT.dto.auth.ForgotPasswordRequest;
import com.example.quanliPT.dto.auth.LoginRequest;
import com.example.quanliPT.dto.auth.RegisterRequest;
import com.example.quanliPT.dto.auth.ResetPasswordRequest;
import com.example.quanliPT.dto.auth.VerifyTokenRequest;
import com.example.quanliPT.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            return ResponseEntity.ok(authService.forgotPassword(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-reset-token")
    public ResponseEntity<?> verifyResetToken(@RequestBody VerifyTokenRequest request) {
        try {
            return ResponseEntity.ok(authService.verifyResetToken(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            return ResponseEntity.ok(authService.resetPassword(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request
    ) {
        try {
            String username = authentication == null ? null : authentication.getName();
            return ResponseEntity.ok(authService.changePassword(username, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}



