package com.example.quanliPT.service.task;

import com.example.quanliPT.model.Contract;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.Invoice;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.model.Notification;
import com.example.quanliPT.model.enums.Role;
import com.example.quanliPT.model.User;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.finance.InvoiceRepository;
import com.example.quanliPT.repository.notification.NotificationRepository;
import com.example.quanliPT.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomatedTaskService {

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Chạy vào lúc 00:00 mỗi ngày để kiểm tra các hóa đơn quá hạn thanh toán.
     * Gửi thông báo cho Admin (1 lần) và tự động khóa tài khoản khách nếu quá hạn > 15 ngày.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkOverdueInvoices() {
        log.info("Bắt đầu tiến trình tự động quét Hóa Đơn quá hạn...");
        List<Invoice> unpaidInvoices = invoiceRepository.findByStatus(InvoiceStatus.UNPAID);
        LocalDateTime now = LocalDateTime.now();
        int overdueCount = 0;
        int lockedCount = 0;

        for (Invoice invoice : unpaidInvoices) {
            LocalDateTime deadline = invoice.getDueDate() != null 
                ? invoice.getDueDate() 
                : (invoice.getBillingDate() != null ? invoice.getBillingDate().plusDays(5) : null);

            if (deadline != null && now.isAfter(deadline)) {
                overdueCount++;
                
                // 1. Idempotency logic: Chỉ gửi thông báo nếu chưa gửi
                if (!invoice.isOverdueNotified()) {
                    String title = "Cảnh báo Hóa đơn quá hạn";
                    String content = String.format("Hóa đơn #%d của Phòng %s đã lố hạn thanh toán từ ngày %s. Vui lòng kiểm tra lại.",
                            invoice.getId(),
                            invoice.getContract().getRoom().getRoomNumber(),
                            deadline.toLocalDate().toString());
                    
                    notifyAdmins(title, content);
                    
                    invoice.setOverdueNotified(true);
                    invoiceRepository.save(invoice);
                }
                
                // 2. Auto-Lock logic: Khóa tài khoản nếu nợ quá 15 ngày
                if (now.isAfter(deadline.plusDays(15))) {
                    User tenant = invoice.getContract().getTenant();
                    if (tenant != null && tenant.isActive()) {
                        tenant.setActive(false);
                        userRepository.save(tenant);
                        lockedCount++;
                        
                        log.warn("Đã tự động khóa tài khoản tenantId={} do nợ hóa đơn #{} quá 15 ngày", tenant.getId(), invoice.getId());
                    }
                }
            }
        }
        log.info("Hoàn tất quét Hóa Đơn. Ghi nhận {} hóa đơn quá hạn. Đã khóa {} tài khoản.", overdueCount, lockedCount);
    }

    /**
     * Chạy vào lúc 00:05 mỗi ngày để kiểm tra các hợp đồng hết hạn.
     * Gửi thông báo cho Admin (1 lần duy nhất nhờ cờ isExpiredNotified).
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void checkExpiredContracts() {
        log.info("Bắt đầu tiến trình tự động quét Hợp Đồng hết hạn...");
        List<Contract> activeContracts = contractRepository.findByActiveTrueAndStatus(ContractStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        int expiredCount = 0;

        for (Contract contract : activeContracts) {
            if (contract.getEndDate() != null && contract.getEndDate().isBefore(today)) {
                expiredCount++;
                
                // Idempotency logic
                if (!contract.isExpiredNotified()) {
                    String title = "Cảnh báo Hợp đồng hết hạn";
                    String content = String.format("Hợp đồng #%d của Phòng %s (Khách thuê: %s) đã hết hạn vào ngày %s. Cần xem xét gia hạn hoặc thanh lý.",
                            contract.getId(),
                            contract.getRoom().getRoomNumber(),
                            contract.getTenant().getFullName(),
                            contract.getEndDate().toString());
                    
                    notifyAdmins(title, content);
                    
                    contract.setExpiredNotified(true);
                    contractRepository.save(contract);
                }
            }
        }
        log.info("Hoàn tất quét Hợp Đồng. Ghi nhận {} hợp đồng đã hết hạn (chỉ gửi thông báo mới nếu có).", expiredCount);
    }

    private void notifyAdmins(String title, String content) {
        List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);
        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .title(title)
                    .content(content)
                    .targetUserId(admin.getId())
                    .broadcast(false)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        }
    }
}


