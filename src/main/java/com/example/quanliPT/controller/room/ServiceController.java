package com.example.quanliPT.controller.room;

import com.example.quanliPT.model.RentalService;
import com.example.quanliPT.dto.service.RentalServiceDTO;
import com.example.quanliPT.repository.room.RentalServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final RentalServiceRepository serviceRepository;

    // ─── GET ALL ─────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<RentalServiceDTO>> getAllServices() {
        List<RentalServiceDTO> services = serviceRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(services);
    }

    // ─── GET BY ID ───────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceById(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .<ResponseEntity<?>>map(service -> ResponseEntity.ok(convertToDTO(service)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── POST: Tạo mới dịch vụ ───────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createService(@RequestBody RentalServiceDTO serviceDTO) {
        try {
            if (serviceDTO.getName() == null || serviceDTO.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tên dịch vụ không được để trống"));
            }
            if (serviceDTO.getPrice() == null || serviceDTO.getPrice().doubleValue() < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Giá dịch vụ không hợp lệ"));
            }

            RentalService service = RentalService.builder()
                    .name(serviceDTO.getName().trim())
                    .price(serviceDTO.getPrice())
                    .category(serviceDTO.getCategory())
                    .unit(serviceDTO.getUnit())
                    .frequency(serviceDTO.getFrequency())
                    .description(serviceDTO.getDescription())
                    .active(true)
                    .build();

            RentalService saved = serviceRepository.save(service);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi tạo dịch vụ: " + e.getMessage()));
        }
    }

    // ─── PUT: Cập nhật dịch vụ ───────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody RentalServiceDTO updated) {
        try {
            RentalService service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại"));

            if (updated.getName() == null || updated.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tên dịch vụ không được để trống"));
            }
            if (updated.getPrice() == null || updated.getPrice().doubleValue() < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Giá dịch vụ không hợp lệ"));
            }

            service.setName(updated.getName().trim());
            service.setPrice(updated.getPrice());
            service.setCategory(updated.getCategory());
            service.setUnit(updated.getUnit());
            service.setFrequency(updated.getFrequency());
            service.setDescription(updated.getDescription());
            service.setActive(updated.isActive());

            RentalService saved = serviceRepository.save(service);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi cập nhật: " + e.getMessage()));
        }
    }

    // ─── DELETE: Xóa dịch vụ (kiểm tra ràng buộc) ────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        try {
            serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại"));

            serviceRepository.deleteById(id);
            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {
            // Bị ràng buộc khóa ngoại (dịch vụ đang được gán cho phòng qua room_services_mapping)
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Không thể xóa: Dịch vụ này đang được sử dụng bởi một hoặc nhiều phòng. Hãy gỡ dịch vụ khỏi các phòng trước, hoặc chuyển trạng thái sang 'Ngưng hoạt động'."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi xóa dịch vụ: " + e.getMessage()));
        }
    }

    // ─── PATCH: Kích hoạt / Vô hiệu hóa dịch vụ ─────────────────────────────
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        try {
            RentalService service = serviceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại"));
            service.setActive(!service.isActive());
            RentalService saved = serviceRepository.save(service);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── HELPER: Convert Entity to DTO ────────────────────────────────────────
    private RentalServiceDTO convertToDTO(RentalService service) {
        return RentalServiceDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .price(service.getPrice())
                .category(service.getCategory())
                .unit(service.getUnit())
                .frequency(service.getFrequency())
                .description(service.getDescription())
                .active(service.isActive())
                .build();
    }
}



