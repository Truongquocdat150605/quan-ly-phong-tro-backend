package com.example.quanliPT.controller.user;

import com.example.quanliPT.dto.auth.ChangePasswordRequest;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.Room;
import com.example.quanliPT.model.User;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TENANT')")
public class TenantController {

    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentTenant(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<List<Room>> getCurrentTenantRooms(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Room> rooms = contractRepository.findByTenantIdAndActiveTrueAndStatus(user.getId(), ContractStatus.ACTIVE)
                .stream()
                .map(c -> c.getRoom())
                .distinct()
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateCurrentTenantProfile(
            Authentication authentication,
            @RequestBody User request
    ) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setIdentityNumber(request.getIdentityNumber());
        user.setAddress(request.getAddress());

        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request
    ) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}



