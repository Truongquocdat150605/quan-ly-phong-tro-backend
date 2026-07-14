package com.example.quanliPT.service.auth;

import com.example.quanliPT.service.notification.EmailService;

import com.example.quanliPT.dto.auth.AuthResponse;
import com.example.quanliPT.dto.auth.ChangePasswordRequest;
import com.example.quanliPT.dto.auth.ForgotPasswordRequest;
import com.example.quanliPT.dto.auth.LoginRequest;
import com.example.quanliPT.dto.auth.RegisterRequest;
import com.example.quanliPT.dto.auth.ResetPasswordRequest;
import com.example.quanliPT.dto.auth.VerifyTokenRequest;
import com.example.quanliPT.model.PasswordResetToken;
import com.example.quanliPT.model.enums.Role;
import com.example.quanliPT.model.User;
import com.example.quanliPT.repository.auth.PasswordResetTokenRepository;
import com.example.quanliPT.repository.user.UserRepository;
import com.example.quanliPT.security.JwtUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int RESET_TOKEN_EXPIRY_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with username={}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username {} already exists", request.getUsername());
            throw new RuntimeException("Username already exists");
        }

        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(Role.TENANT)
                .active(true)
                .build();

        userRepository.save(user);
        log.debug("User saved with id={}, username={}", user.getId(), user.getUsername());

        var springUser = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        var token = jwtUtils.generateToken(springUser);
        log.info("User registered successfully, token generated for username={}", user.getUsername());

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username={}", request.getUsername());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        log.debug("Authentication successful for username={}", request.getUsername());

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found after authentication: {}", request.getUsername());
                    return new RuntimeException("User not found");
                });

        var springUser = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        var token = jwtUtils.generateToken(springUser);
        log.info("Login successful for username={}, token generated", user.getUsername());

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        passwordResetTokenRepository.deleteByEmail(email);

        String token = generateUniqueSixDigitToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(email)
                .expiryDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendResetEmail(email, token, user.getFullName());

        log.info("Password reset token created for email={}", email);
        return Map.of("message", "Mã đặt lại mật khẩu đã được gửi tới email của bạn");
    }

    public Map<String, Object> verifyResetToken(VerifyTokenRequest request) {
        validateResetToken(request.getToken(), request.getEmail());
        return Map.of(
                "valid", true,
                "message", "Mã xác thực hợp lệ"
        );
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = validateResetToken(request.getToken(), request.getEmail());
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for email={}", email);
        return Map.of("message", "Đặt lại mật khẩu thành công");
    }

    @Transactional
    public Map<String, String> changePassword(String username, ChangePasswordRequest request) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Bạn cần đăng nhập để đổi mật khẩu");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new RuntimeException("Mật khẩu hiện tại không được để trống");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for username={}", username);
        return Map.of("message", "Đổi mật khẩu thành công");
    }

    private PasswordResetToken validateResetToken(String token, String email) {
        String normalizedToken = normalizeToken(token);
        String normalizedEmail = normalizeEmail(email);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(normalizedToken)
                .orElseThrow(() -> new RuntimeException("Mã xác thực không hợp lệ"));

        if (!resetToken.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new RuntimeException("Email không khớp với mã xác thực");
        }

        if (resetToken.isUsed()) {
            throw new RuntimeException("Mã xác thực đã được sử dụng");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã xác thực đã hết hạn");
        }

        return resetToken;
    }

    private String generateUniqueSixDigitToken() {
        String token;
        do {
            token = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        } while (passwordResetTokenRepository.findByToken(token).isPresent());
        return token;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Mã xác thực không được để trống");
        }
        return token.trim();
    }
}




