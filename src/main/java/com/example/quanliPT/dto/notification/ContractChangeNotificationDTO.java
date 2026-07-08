package com.example.quanliPT.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractChangeNotificationDTO {
    public enum Type {
        CREATE,
        UPDATE,
        DELETE
    }

    private Type type;
    private Long contractId;
    private Long tenantId;
    private String message;
    private Instant updatedAt;
}


