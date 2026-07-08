package com.example.quanliPT.repository.user;

import com.example.quanliPT.model.enums.Role;
import com.example.quanliPT.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    List<User> findByPhone(String phone);
    List<User> findByRoleAndActiveTrue(Role role);
    
    boolean existsByUsername(String username);
}

