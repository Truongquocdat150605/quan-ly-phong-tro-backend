package com.example.quanliPT.repository.auth;

import com.example.quanliPT.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteByEmail(String email);

    List<PasswordResetToken> findAllByEmail(String email);
}

