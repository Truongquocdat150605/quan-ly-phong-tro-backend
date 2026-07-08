package com.example.quanliPT.config;

import com.example.quanliPT.model.enums.Role;
import com.example.quanliPT.model.User;
import com.example.quanliPT.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = userRepository.findByUsername("admin")
                .orElseGet(() -> User.builder()
                        .username("admin")
                        .fullName("Administrator")
                        .email("admin@phongtro.vn")
                        .phone("0123456789")
                        .build());

        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFullName("System Administrator");
        admin.setEmail("admin@phongtro.vn");
        admin.setPhone("0123456789");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
    }
}

