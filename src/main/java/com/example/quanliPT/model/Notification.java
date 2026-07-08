package com.example.quanliPT.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // null = gửi cho tất cả tenant; có id = gửi riêng cho user đó
    @Column(name = "target_user_id")
    private Long targetUserId;

    @Builder.Default
    @Column(name = "broadcast")
    private boolean broadcast = false;

    @Builder.Default
    @Column(name = "is_read")
    private boolean isRead = false;
}
