package com.example.quanliPT.dto.notification;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Long targetUserId;
    private boolean broadcast;
    private boolean isRead;

    /**
     * Kept for backward-compatibility with existing code/tests.
     */
    public static NotificationDTOBuilder builder() {
        return new NotificationDTOBuilder();
    }

    /**
     * Support legacy getter naming from NotificationDTO usage.
     */
    public boolean getRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }


    public static class NotificationDTOBuilder {
        private final NotificationDTO dto = new NotificationDTO();

        public NotificationDTOBuilder id(Long id) {
            dto.setId(id);
            return this;
        }

        public NotificationDTOBuilder title(String title) {
            dto.setTitle(title);
            return this;
        }

        public NotificationDTOBuilder content(String content) {
            dto.setContent(content);
            return this;
        }

        public NotificationDTOBuilder createdAt(LocalDateTime createdAt) {
            dto.setCreatedAt(createdAt);
            return this;
        }

        public NotificationDTOBuilder targetUserId(Long targetUserId) {
            dto.setTargetUserId(targetUserId);
            return this;
        }

        public NotificationDTOBuilder broadcast(boolean broadcast) {
            dto.setBroadcast(broadcast);
            return this;
        }

        public NotificationDTOBuilder isRead(boolean isRead) {
            dto.setRead(isRead);
            return this;
        }

        public NotificationDTO build() {
            return dto;
        }
    }
}


