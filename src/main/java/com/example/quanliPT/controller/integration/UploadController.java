package com.example.quanliPT.controller.integration;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired(required = false)
    private Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file ảnh (jpg, png, webp...)"));
            }

            String imageUrl = null;

            // 1. Thử upload lên Cloudinary nếu có cấu hình
            if (cloudinary != null && StringUtils.hasText(cloudName)) {
                try {
                    Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
                    if (uploadResult != null) {
                        if (uploadResult.get("secure_url") != null) {
                            imageUrl = uploadResult.get("secure_url").toString();
                        } else if (uploadResult.get("url") != null) {
                            imageUrl = uploadResult.get("url").toString().replace("http://", "https://");
                        }
                        if (imageUrl != null) {
                            System.out.println("✅ [Upload] File đã lưu lên Cloudinary (HTTPS): " + imageUrl);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("⚠️ [Upload Cloudinary warning] " + ex.getMessage() + ". Chuyển sang lưu local.");
                }
            }

            // 2. Fallback lưu vào thư mục local 'uploads/' trên server nếu chưa có URL Cloudinary
            if (imageUrl == null) {
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
                String filename = UUID.randomUUID().toString() + "_" + originalFilename;
                Path filePath = uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                imageUrl = filename;
                System.out.println("✅ [Upload] File đã lưu local: " + filePath);
            }

            Map<String, String> response = new HashMap<>();
            response.put("fileName", imageUrl);
            response.put("filePath", imageUrl);
            response.put("message", "Upload thành công");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [Upload error] " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi upload: " + e.getMessage()));
        }
    }
}
