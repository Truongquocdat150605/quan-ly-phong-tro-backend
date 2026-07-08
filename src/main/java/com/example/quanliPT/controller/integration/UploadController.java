package com.example.quanliPT.controller.integration;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class UploadController {

    // ✅ Đường dẫn tương đối so với thư mục chạy ứng dụng
    private final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Kiểm tra file rỗng
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File không được để trống");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate loại file (chỉ chấp nhận ảnh)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Chỉ chấp nhận file ảnh (jpg, png, webp...)");
                return ResponseEntity.badRequest().body(error);
            }

            // Tạo tên file duy nhất bằng UUID
            String originalFileName = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
            );
            String extension = "";
            if (originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + extension;

            // Tạo thư mục uploads nếu chưa có
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("📁 [Upload] Đã tạo thư mục: " + uploadPath);
            }

            // Lưu file
            Path filePath = uploadPath.resolve(newFileName);
            file.transferTo(filePath.toFile());

            System.out.println("✅ [Upload] File đã lưu: " + filePath);

            // Trả về thông tin file
            Map<String, String> response = new HashMap<>();
            response.put("fileName", newFileName);
            response.put("filePath", "/uploads/" + newFileName);
            response.put("message", "Upload thành công");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [Upload error] " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi upload: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}

