package com.example.quanliPT.controller.admin;

import com.example.quanliPT.model.*;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;

import com.example.quanliPT.repository.user.UserRepository;
import com.example.quanliPT.model.enums.RoomStatus;

import com.example.quanliPT.model.ContactMessage;
import com.example.quanliPT.model.Contract;
import com.example.quanliPT.model.RentalRequest;
import com.example.quanliPT.model.enums.RentalRequestStatus;

import com.example.quanliPT.service.contract.ContractBusinessService;
import com.example.quanliPT.repository.guest.ContactMessageRepository;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.guest.RentalRequestRepository;
import com.example.quanliPT.repository.room.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminRequestController {

    private final RentalRequestRepository rentalRequestRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final ContractBusinessService contractBusinessService;

    @GetMapping("/rental")
    public List<RentalRequest> getAllRentalRequests() {
        log.info("Entering getAllRentalRequests");
        List<RentalRequest> result = rentalRequestRepository.findAll();
        log.info("Returning {} rental requests", result.size());
        return result;
    }

    @PutMapping("/rental/{id}/status")
    public ResponseEntity<RentalRequest> updateRentalRequestStatus(
            @PathVariable Long id,
            @RequestParam RentalRequestStatus status
    ) {
        log.info("Entering updateRentalRequestStatus with id={}, status={}", id, status);
        RentalRequest request = rentalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental request not found"));
        request.setStatus(status);
        RentalRequest saved = rentalRequestRepository.save(request);
        log.info("Rental request id={} status updated to {}", id, status);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/contacts")
    public List<ContactMessage> getAllContactMessages() {
        log.info("Entering getAllContactMessages");
        List<ContactMessage> result = contactMessageRepository.findAll();
        log.info("Returning {} contact messages", result.size());
        return result;
    }

    @PostMapping("/rental/{id}/approve-and-create-contract")
    public ResponseEntity<Contract> approveAndCreateContract(
            @PathVariable Long id,
            @RequestParam LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam BigDecimal rentPrice,
            @RequestParam BigDecimal deposit
    ) {
        log.info("Entering approveAndCreateContract with id={}, startDate={}, endDate={}", id, startDate, endDate);
        if (startDate == null) {
            throw new IllegalArgumentException("startDate không được để trống");
        }
        RentalRequest request = rentalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental request not found"));


        // Kiểm tra trạng thái: Chỉ duyệt khi còn PENDING
        if (request.getStatus() != RentalRequestStatus.PENDING) {
            log.warn("Rental request id={} already processed with status={}", id, request.getStatus());
            return ResponseEntity.badRequest().body(null);
        }

        var room = request.getRoom();
        if (room == null) {
            log.error("Rental request id={} has no room associated", id);
            throw new RuntimeException("Rental request has no room");
        }

        Contract savedContract = contractBusinessService.createContractAndTenant(
                room.getId(),
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getIdentityNumber(),
                startDate,
                endDate,
                rentPrice,
                deposit
        );

        // Trạng thái phòng đã được cập nhật trong contractBusinessService.createContractAndTenant()
        // Không cần thiết lập lại ở đây để tránh thao tác thừa

        request.setStatus(RentalRequestStatus.APPROVED);
        rentalRequestRepository.save(request);

        log.info("Contract created successfully with id={} for rental request id={}", savedContract.getId(), id);
        return ResponseEntity.ok(savedContract);
    }
}






