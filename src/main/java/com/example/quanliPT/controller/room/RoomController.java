package com.example.quanliPT.controller.room;

import com.example.quanliPT.service.room.RoomService;

import com.example.quanliPT.model.*;
import com.example.quanliPT.model.enums.RoomStatus;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;

import com.example.quanliPT.model.Room;

import com.example.quanliPT.repository.room.RoomRepository;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.model.Contract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final RoomService roomService;
    private final RentalServiceRepository rentalServiceRepository;


    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        log.info("Entering getAllRooms");
        List<Room> rooms = roomRepository.findAll();
        log.info("Returning {} rooms", rooms.size());
        return rooms;
    }

    @Cacheable(value = "roomsAvailable")
    @GetMapping("/available")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Room> getAvailableRooms() {
        log.info("Entering getAvailableRooms");
        List<Room> rooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);
        log.info("Returning {} available rooms", rooms.size());
        return rooms;
    }

    @Cacheable(value = "roomsHot", key = "'top5'")
    @GetMapping("/hot")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Room> getHotRooms() {
        log.info("Entering getHotRooms");
        List<Room> rooms = roomService.getHotRooms();
        log.info("Returning {} hot rooms", rooms.size());
        return rooms;
    }

    @Cacheable(value = "roomsNewest", key = "'top5'")
    @GetMapping("/newest")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Room> getNewestRooms() {
        log.info("Entering getNewestRooms");
        List<Room> rooms = roomService.getNewestRooms();
        log.info("Returning {} newest rooms", rooms.size());
        return rooms;
    }


    @Cacheable(value = "roomsById", key = "#id")
    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        log.info("Entering getRoomById with id={}", id);
        return roomRepository.findById(id)
                .map(room -> {
                    log.debug("Found room id={}", room.getId());
                    return ResponseEntity.ok(room);
                })
                .orElseGet(() -> {
                    log.warn("Room not found with id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"roomsAvailable", "roomsById", "roomsHot", "roomsNewest"}, allEntries = true)
    public ResponseEntity<?> createRoom(
            @RequestParam("roomNumber") String roomNumber,
            @RequestParam("type") String type,
            @RequestParam("price") String priceStr,
            @RequestParam("area") String areaStr,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "serviceIds", required = false) List<Long> serviceIds,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        log.info("Entering createRoom with roomNumber={}, type={}", roomNumber, type);
        try {
            if (roomNumber == null || roomNumber.trim().isEmpty()) {
                log.warn("roomNumber is empty");
                return ResponseEntity.badRequest().body(Map.of("error", "Số phòng không được để trống"));
            }

            java.math.BigDecimal price = java.math.BigDecimal.ZERO;
            try {
                if (priceStr != null && !priceStr.isEmpty()) {
                    price = new java.math.BigDecimal(priceStr);
                }
            } catch (Exception e) {
                log.warn("Invalid price value: {}", priceStr);
            }

            if (price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Giá thuê phải lớn hơn 0"));
            }


            double area = 0;
            try {
                if (areaStr != null && !areaStr.isEmpty()) {
                    area = Double.parseDouble(areaStr);
                }
            } catch (Exception e) {
                log.warn("Invalid area value: {}", areaStr);
            }

            String imageName = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    imageName = saveImage(imageFile);
                } catch (Exception e) {
                    log.error("Failed to save image: {}", e.getMessage(), e);
                }
            }

            Room room = Room.builder()
                    .roomNumber(roomNumber)
                    .type(type)
                    .price(price)
                    .area(area)
                    .description(description)
                    .image(imageName)
                    .status(RoomStatus.AVAILABLE)
                    .services(loadSelectedServices(serviceIds))
                    .build();

            Room savedRoom = roomRepository.save(room);
            log.info("Room created successfully id={}, roomNumber={}", savedRoom.getId(), savedRoom.getRoomNumber());
            return ResponseEntity.ok(savedRoom);
        } catch (Exception e) {
            log.error("Error creating room: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"roomsAvailable", "roomsById", "roomsHot", "roomsNewest"}, allEntries = true)
    public ResponseEntity<?> updateRoom(
            @PathVariable Long id,
            @RequestParam("roomNumber") String roomNumber,
            @RequestParam("type") String type,
            @RequestParam("price") String priceStr,
            @RequestParam("area") String areaStr,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "serviceIds", required = false) List<Long> serviceIds,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        log.info("Entering updateRoom with id={}, roomNumber={}", id, roomNumber);
        try {
            Room room = roomRepository.findById(id).orElseThrow(() -> {
                log.error("Room not found with id={}", id);
                return new RuntimeException("Phòng không tồn tại");
            });

            if (priceStr != null && !priceStr.isEmpty()) {
                room.setPrice(new java.math.BigDecimal(priceStr));
            }
            if (areaStr != null && !areaStr.isEmpty()) {
                room.setArea(Double.parseDouble(areaStr));
            }

            room.setRoomNumber(roomNumber);
            room.setType(type);
            room.setDescription(description);
            room.setServices(loadSelectedServices(serviceIds));

            if (status != null && !status.isEmpty()) {
                try {
                    room.setStatus(RoomStatus.valueOf(status.toUpperCase()));
                    log.debug("Room status set to {}", status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}, keeping existing status", status);
                }
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imageName = saveImage(imageFile);
                    room.setImage(imageName);
                } catch (Exception e) {
                    log.error("Failed to save image for room id={}: {}", id, e.getMessage(), e);
                }
            }

            Room savedRoom = roomRepository.save(room);
            log.info("Room updated successfully id={}", savedRoom.getId());
            return ResponseEntity.ok(savedRoom);
        } catch (Exception e) {
            log.error("Error updating room id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi cập nhật phòng: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"roomsAvailable", "roomsById", "roomsHot", "roomsNewest"}, allEntries = true)
    public ResponseEntity<?> updateRoomImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile imageFile
    ) {
        log.info("Entering updateRoomImage for room id={}", id);
        try {
            Room room = roomRepository.findById(id).orElseThrow(() -> {
                log.error("Room not found with id={}", id);
                return new RuntimeException("Room not found");
            });
            String imageName = saveImage(imageFile);
            room.setImage(imageName);
            roomRepository.save(room);

            Map<String, String> response = new HashMap<>();
            response.put("fileName", imageName);
            response.put("filePath", "/uploads/" + imageName);
            response.put("message", "Cập nhật ảnh phòng thành công");
            log.info("Image updated for room id={}: {}", id, imageName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating image for room id={}: {}", id, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi cập nhật ảnh: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"roomsAvailable", "roomsById", "roomsHot", "roomsNewest"}, allEntries = true)
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        log.info("Entering deleteRoom with id={}", id);
        try {
            Room room = roomRepository.findById(id).orElseThrow(() -> {
                log.error("Room not found with id={}", id);
                return new RuntimeException("Phòng không tồn tại");
            });

            List<Contract> activeContracts = contractRepository.findByRoomIdAndActiveTrue(id);
            if (activeContracts != null && !activeContracts.isEmpty()) {
                log.warn("Cannot delete room id={} because it has {} active contracts", id, activeContracts.size());
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa: Phòng này đang có hợp đồng hoạt động."));
            }

            roomRepository.deleteById(id);
            log.info("Room id={} deleted", id);
            return ResponseEntity.noContent().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Cannot delete room id={} due to data integrity violation: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa phòng này vì đã có dữ liệu hợp đồng hoặc hóa đơn liên quan."));
        } catch (Exception e) {
            log.error("Error deleting room id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    private String saveImage(MultipartFile imageFile) throws Exception {
        log.debug("Saving image file: originalName={}", imageFile.getOriginalFilename());
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("Invalid content type: {}", contentType);
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh");
        }
        String originalName = StringUtils.cleanPath(
                imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename() : "img"
        );
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : "";
        String fileName = UUID.randomUUID().toString() + ext;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.debug("Upload directory created: {}", uploadPath);
        }

        imageFile.transferTo(uploadPath.resolve(fileName).toFile());
        log.info("Image saved: {}", fileName);
        return fileName;
    }

    private Set<RentalService> loadSelectedServices(List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(rentalServiceRepository.findAllById(serviceIds));
    }
}






