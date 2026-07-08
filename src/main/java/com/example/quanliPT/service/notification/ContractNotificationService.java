package com.example.quanliPT.service.notification;

import com.example.quanliPT.dto.notification.ContractChangeNotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void notifyContractChanged(ContractChangeNotificationDTO dto) {
        // Tenant sẽ nhận trigger và tự gọi GET /api/contracts/my để lấy dữ liệu (đảm bảo phân quyền)
        messagingTemplate.convertAndSend("/topic/contracts/changed", dto);
    }
}



