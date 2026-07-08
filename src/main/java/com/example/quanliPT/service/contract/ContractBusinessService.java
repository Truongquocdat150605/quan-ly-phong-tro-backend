package com.example.quanliPT.service.contract;

import com.example.quanliPT.service.finance.BillingService;
import com.example.quanliPT.service.notification.EmailService;

import com.example.quanliPT.model.*;
import com.example.quanliPT.model.enums.RoomStatus;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;

import com.example.quanliPT.repository.user.UserRepository;
import java.util.Optional;

import com.example.quanliPT.model.Contract;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.enums.Role;
import com.example.quanliPT.model.Room;

import com.example.quanliPT.model.User;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.room.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;


@Service
@RequiredArgsConstructor
@Slf4j
public class ContractBusinessService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BillingService billingService;
    private final EmailService emailService;

    @Transactional
    @CacheEvict(value = {"roomsAvailable", "roomsById"}, allEntries = true)
    public Contract createContractAndTenant(
            Long roomId,
            String tenantFullName,
            String tenantEmail,
            String tenantPhone,
            String tenantIdentity,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal rentPrice,
            BigDecimal deposit) {

        log.info("Starting createContractAndTenant for roomId={}, tenantPhone={}", roomId, tenantPhone);

        // 1. Lấy phòng
        log.debug("Looking up room with id={}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("Room not found with id={}", roomId);
                    return new RuntimeException("Không tìm thấy phòng");
                });
        log.debug("Found room: id={}, status={}", room.getId(), room.getStatus());

        // 2. Tìm hoặc tạo User
        String loginIdentifier = tenantPhone;
        log.debug("Looking up user by phone={}", loginIdentifier);
        java.util.List<User> usersByPhone = userRepository.findByPhone(loginIdentifier);

        User tenant;
        boolean isNewUser = false;
        if (!usersByPhone.isEmpty()) {
            tenant = usersByPhone.get(0); // Lấy user đầu tiên nếu có nhiều
            log.debug("Existing user found: id={}, username={}", tenant.getId(), tenant.getUsername());
            // KHÔNG GHI ĐÈ thông tin cũ (Email, CCCD, Tên) bằng thông tin mới từ form.
            // Tránh trường hợp nhập nhầm SĐT làm sai lệch dữ liệu của người khác.
            // Khách có thể tự cập nhật Profile của mình trong trang cá nhân sau.
        } else {
            isNewUser = true;
            log.debug("No existing user found, creating new tenant");
            tenant = User.builder()
                    .username(tenantPhone)
                    .email(tenantEmail != null ? tenantEmail : tenantPhone + "@example.com")
                    .fullName(tenantFullName)
                    .phone(tenantPhone)
                    .identityNumber(tenantIdentity)
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.TENANT)
                    .active(true)
                    .build();
            tenant = userRepository.save(tenant);
            log.info("Created new tenant id={}, username={}", tenant.getId(), tenant.getUsername());
        }

        // 3. Tạo hợp đồng
        log.debug("Creating contract for roomId={}, tenantId={}", room.getId(), tenant.getId());
        Contract contract = Contract.builder()
                .room(room)
                .tenant(tenant)
                .startDate(startDate)
                .endDate(endDate)
                .rentPrice(rentPrice)
                .deposit(deposit)
                .status(ContractStatus.ACTIVE)
                .active(true)
                .build();

        contract = contractRepository.save(contract);
        log.info("Contract created with id={}, status=ACTIVE", contract.getId());

        // 4. Đổi trạng thái phòng
        log.debug("Updating room status to OCCUPIED for roomId={}", room.getId());
        room.setStatus(RoomStatus.OCCUPIED);
        log.info("Room id={} status changed to {}", room.getId(), room.getStatus());
        roomRepository.save(room);

        // 5. Tự động tạo hóa đơn kỳ đầu
        log.debug("Generating initial invoice for contract id={}", contract.getId());
        try {
            billingService.generateInvoiceForContract(contract);
            log.info("Initial invoice generated for contract id={}", contract.getId());
        } catch (Exception e) {
            log.error("Failed to generate initial invoice for contract id={}: {}", contract.getId(), e.getMessage(), e);
        }

        // 6. Gửi email thông báo
        if (tenant.getEmail() != null && !tenant.getEmail().isBlank() && !tenant.getEmail().endsWith("@example.com")) {
            log.debug("Sending welcome/approval email to {}", tenant.getEmail());
            try {
                if (isNewUser) {
                    emailService.sendWelcomeEmail(
                            tenant.getEmail(),
                            tenant.getFullName(),
                            room.getRoomNumber(),
                            tenant.getPhone(),
                            "123456" // Mật khẩu mặc định lúc tạo mới
                    );
                } else {
                    emailService.sendExistingUserContractEmail(
                            tenant.getEmail(),
                            tenant.getFullName(),
                            room.getRoomNumber()
                    );
                }
            } catch (Exception e) {
                log.error("Failed to send welcome email to {}: {}", tenant.getEmail(), e.getMessage());
            }
        }

        log.info("Completed createContractAndTenant successfully for contractId={}", contract.getId());
        return contract;
    }
}






