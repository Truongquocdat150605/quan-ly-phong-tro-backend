package com.example.quanliPT.controller.contract;

import com.example.quanliPT.model.Contract;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.enums.RoomStatus;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.room.RoomRepository;
import com.example.quanliPT.repository.user.UserRepository;
import com.example.quanliPT.service.contract.ContractBusinessService;
import com.example.quanliPT.service.notification.ContractNotificationService;
import com.example.quanliPT.dto.notification.ContractChangeNotificationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractController {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ContractBusinessService contractBusinessService;
    private final ContractNotificationService contractNotificationService;


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Contract> getAllContracts() {
        log.info("Entering getAllContracts");
        List<Contract> result = contractRepository.findAll();
        log.info("Returning {} contracts", result.size());
        return result;
    }

    @GetMapping("/my-contracts/{tenantId}")
    @PreAuthorize("@contractSecurity.canAccessTenantContracts(#tenantId, authentication)")
    public List<Contract> getMyContracts(@PathVariable Long tenantId) {
        log.info("Entering getMyContracts for tenantId={}", tenantId);
        List<Contract> result = contractRepository.findByTenantId(tenantId);
        log.info("Returning {} contracts for tenantId={}", result.size(), tenantId);
        return result;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public List<Contract> getCurrentTenantContracts(Authentication authentication) {
        log.info("Entering getCurrentTenantContracts for user={}", authentication.getName());
        var user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<Contract> result = contractRepository.findByTenantId(user.getId());
        log.info("Returning {} contracts for current user", result.size());
        return result;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        log.info("Entering createContract with roomId={}, tenantId={}",
                contract.getRoom() != null ? contract.getRoom().getId() : null,
                contract.getTenant() != null ? contract.getTenant().getId() : null);
        Contract saved = contractRepository.save(contract);
        if (saved.getRoom() != null) {
            saved.getRoom().setStatus(RoomStatus.OCCUPIED);
            log.info("ContractController: createContract: Cập nhật trạng thái phòng ID {} thành {}", saved.getRoom().getId(), saved.getRoom().getStatus());
            roomRepository.save(saved.getRoom());
        }
        log.info("Contract created id={}", saved.getId());
        ContractChangeNotificationDTO dto = ContractChangeNotificationDTO.builder()
                .type(ContractChangeNotificationDTO.Type.CREATE)
                .contractId(saved.getId())
                .tenantId(saved.getTenant() != null ? saved.getTenant().getId() : null)
                .message("Hợp đồng đã được tạo")
                .updatedAt(saved.getLastModifiedDate() != null ? saved.getLastModifiedDate() : null)
                .build();
        contractNotificationService.notifyContractChanged(dto);
        return ResponseEntity.ok(saved);

    }

    @PostMapping("/create-with-tenant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Contract> createContractWithTenant(
            @RequestParam Long roomId,
            @RequestParam String tenantFullName,
            @RequestParam String tenantEmail,
            @RequestParam String tenantPhone,
            @RequestParam(required = false) String tenantIdentity,
            @RequestParam String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam BigDecimal rentPrice,
            @RequestParam BigDecimal deposit) {

        log.info("Entering createContractWithTenant roomId={}, tenantPhone={}, startDate={}", roomId, tenantPhone, startDate);
        Contract contract = contractBusinessService.createContractAndTenant(
                roomId,
                tenantFullName,
                tenantEmail,
                tenantPhone,
                tenantIdentity,
                LocalDate.parse(startDate),
                endDate != null ? LocalDate.parse(endDate) : null,
                rentPrice,
                deposit
        );
        log.info("Contract created with id={} via createContractWithTenant", contract.getId());
        ContractChangeNotificationDTO dto = ContractChangeNotificationDTO.builder()
                .type(ContractChangeNotificationDTO.Type.CREATE)
                .contractId(contract.getId())
                .tenantId(contract.getTenant() != null ? contract.getTenant().getId() : null)
                .message("Hợp đồng đã được tạo")
                .updatedAt(contract.getLastModifiedDate() != null ? contract.getLastModifiedDate() : null)
                .build();
        contractNotificationService.notifyContractChanged(dto);
        return ResponseEntity.ok(contract);

    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Contract> updateContract(@PathVariable Long id, @RequestBody Contract contractDetails) {
        log.info("Entering updateContract for contract id={}", id);
        Contract contract = contractRepository.findById(id).orElseThrow(() -> {
            log.error("Contract not found with id={}", id);
            return new RuntimeException("Contract not found");
        });
        contract.setStartDate(contractDetails.getStartDate());
        contract.setEndDate(contractDetails.getEndDate());
        contract.setRentPrice(contractDetails.getRentPrice());
        contract.setDeposit(contractDetails.getDeposit());
        contract.setStatus(contractDetails.getStatus());
        contract.setActive(contractDetails.isActive());
        Contract saved = contractRepository.save(contract);
        log.debug("Contract id={} saved with status={}, active={}", saved.getId(), saved.getStatus(), saved.isActive());

        if (saved.getRoom() != null) {
            boolean shouldFreeRoom = !saved.isActive()
                    || saved.getStatus() == ContractStatus.EXPIRED
                    || saved.getStatus() == ContractStatus.TERMINATED;
            saved.getRoom().setStatus(shouldFreeRoom ? RoomStatus.AVAILABLE : RoomStatus.OCCUPIED);
            log.info("ContractController: updateContract: Cập nhật trạng thái phòng ID {} thành {}", saved.getRoom().getId(), saved.getRoom().getStatus());
            roomRepository.save(saved.getRoom());
        }

        if (saved.getTenant() != null) {
            if (saved.getStatus() == ContractStatus.TERMINATED || !saved.isActive()) {
                saved.getTenant().setActive(false);
                log.debug("Tenant id={} deactivated", saved.getTenant().getId());
            } else if (saved.getStatus() == ContractStatus.ACTIVE && saved.isActive()) {
                saved.getTenant().setActive(true);
                log.debug("Tenant id={} activated", saved.getTenant().getId());
            }
            userRepository.save(saved.getTenant());
        }

        log.info("Contract id={} updated successfully", saved.getId());
        ContractChangeNotificationDTO dto = ContractChangeNotificationDTO.builder()
                .type(ContractChangeNotificationDTO.Type.UPDATE)
                .contractId(saved.getId())
                .tenantId(saved.getTenant() != null ? saved.getTenant().getId() : null)
                .message("Hợp đồng đã được cập nhật")
                .updatedAt(saved.getLastModifiedDate() != null ? saved.getLastModifiedDate() : null)
                .build();
        contractNotificationService.notifyContractChanged(dto);
        return ResponseEntity.ok(saved);

    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        log.info("Entering deleteContract for contract id={}", id);
        Contract contract = contractRepository.findById(id).orElseThrow(() -> {
            log.error("Contract not found with id={}", id);
            return new RuntimeException("Contract not found");
        });
        contractRepository.deleteById(id);
        log.info("Contract id={} deleted", id);
        ContractChangeNotificationDTO dto = ContractChangeNotificationDTO.builder()
                .type(ContractChangeNotificationDTO.Type.DELETE)
                .contractId(id)
                .tenantId(contract.getTenant() != null ? contract.getTenant().getId() : null)
                .message("Hợp đồng đã bị xóa")
                .updatedAt(contract.getLastModifiedDate() != null ? contract.getLastModifiedDate() : null)
                .build();
        contractNotificationService.notifyContractChanged(dto);
        return ResponseEntity.noContent().build();

    }
}




