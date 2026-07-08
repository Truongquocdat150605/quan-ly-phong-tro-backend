package com.example.quanliPT.repository.notification;

import com.example.quanliPT.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy thông báo dành cho 1 user cụ thể + thông báo broadcast (gửi tất cả)
    @Query("SELECT n FROM Notification n WHERE n.targetUserId = :userId OR (n.broadcast = true AND n.targetUserId IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsForUser(@Param("userId") Long userId);

    // Đếm thông báo chưa đọc
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.targetUserId = :userId AND n.isRead = false")
    long countUnreadForUser(@Param("userId") Long userId);

    // Lấy tất cả (cho admin)
    List<Notification> findAllByOrderByCreatedAtDesc();
}

