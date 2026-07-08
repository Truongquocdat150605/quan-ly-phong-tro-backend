package com.example.quanliPT.controller.integration;

import com.example.quanliPT.model.Notification;
import com.example.quanliPT.model.enums.Role;
import com.example.quanliPT.model.User;
import com.example.quanliPT.dto.notification.NotificationDTO;
import com.example.quanliPT.repository.notification.NotificationRepository;
import com.example.quanliPT.repository.user.UserRepository;
import com.example.quanliPT.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    // ─── ADMIN: Lấy tất cả thông báo đã gửi ─────────────────────────────────
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getAllForAdmin() {
        List<NotificationDTO> notifications = notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    // ─── ADMIN: Gửi thông báo ────────────────────────────────────────────────
    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> body) {
        try {
            String title = (String) body.get("title");
            String content = (String) body.get("content");
            Object targetId = body.get("targetUserId");

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tiêu đề không được để trống"));
            }
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nội dung không được để trống"));
            }

            boolean sendToAll = targetId == null || targetId.toString().isEmpty();
            if (sendToAll) {
                List<User> tenants = userRepository.findByRoleAndActiveTrue(Role.TENANT);
                List<Notification> notifications = tenants.stream()
                        .map(tenant -> Notification.builder()
                                .title(title.trim())
                                .content(content.trim())
                                .targetUserId(tenant.getId())
                                .broadcast(true)
                                .createdAt(LocalDateTime.now())
                                .isRead(false)
                                .build())
                        .collect(Collectors.toList());

                notificationRepository.saveAll(notifications);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Đã gửi thông báo đến tất cả người thuê",
                        "count", notifications.size()
                ));
            }

            Long targetUserId = Long.valueOf(targetId.toString());
            userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Người nhận không tồn tại"));

            Notification notification = Notification.builder()
                    .title(title.trim())
                    .content(content.trim())
                    .targetUserId(targetUserId)
                    .broadcast(false)
                    .createdAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            Notification saved = notificationRepository.save(notification);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi gửi thông báo: " + e.getMessage()));
        }
    }

    // ─── ADMIN: Xóa thông báo ────────────────────────────────────────────────
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));
            notificationRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── TENANT: Lấy thông báo của tôi ──────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT')")
    public ResponseEntity<?> getMyNotifications(HttpServletRequest request) {
        try {
            Long userId = extractUserIdFromRequest(request);
            List<NotificationDTO> notifications = notificationRepository.findNotificationsForUser(userId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── TENANT: Đếm thông báo chưa đọc ─────────────────────────────────────
    @GetMapping("/my/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = extractUserIdFromRequest(request);
            long count = notificationRepository.countUnreadForUser(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }
    }

    // ─── TENANT: Đánh dấu đã đọc ─────────────────────────────────────────────
    @PatchMapping("/my/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT')")
    public ResponseEntity<?> markAsRead(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        try {
            Long userId = extractUserIdFromRequest(request);
            Notification n = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
            if (!userId.equals(n.getTargetUserId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không có quyền đọc thông báo này"));
            }
            n.setRead(true);
            Notification saved = notificationRepository.save(n);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Helper: Convert Entity to DTO ────────────────────────────────────────
    @PatchMapping("/my/mark-all-read")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT')")
    public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
        try {
            Long userId = extractUserIdFromRequest(request);
            List<Notification> notifications = notificationRepository.findNotificationsForUser(userId);
            notifications.forEach(notification -> notification.setRead(true));
            notificationRepository.saveAll(notifications);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", notifications.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/my/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT')")
    public ResponseEntity<?> deleteMyNotification(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        try {
            Long userId = extractUserIdFromRequest(request);
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

            if (!userId.equals(notification.getTargetUserId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không có quyền xóa thông báo này"));
            }

            notificationRepository.delete(notification);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .createdAt(notification.getCreatedAt())
                .targetUserId(notification.getTargetUserId())
                .broadcast(notification.isBroadcast())
                .isRead(notification.isRead())
                .build();
    }

    // ─── Helper: Lấy userId từ JWT token trong request ───────────────────────
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // For some tests (MockMvc) no Authorization header is provided.
            // Fall back to the current security principal name when possible.
            try {
                var auth = request.getUserPrincipal();
                if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                    String username = auth.getName();
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
                    return user.getId();
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Không tìm thấy token xác thực");
        }

        String token = header.substring(7);
        String username = jwtUtils.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return user.getId();
    }
}



