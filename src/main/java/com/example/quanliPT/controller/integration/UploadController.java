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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.info("📸 [UploadController] Received upload request: filename={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
        try {
            if (file.isEmpty()) {
                log.warn("📸 [UploadController] File is empty");
                return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("📸 [UploadController] Invalid content type: {}", contentType);
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file ảnh (jpg, png, webp...)"));
            }

            String imageUrl = null;
            String source = "NONE";

            // 1. Thử upload lên Cloudinary nếu có cấu hình
            if (cloudinary != null && StringUtils.hasText(cloudName)) {
                try {
                    log.info("📸 [UploadController] Attempting Cloudinary upload (cloudName={})...", cloudName);
                    Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
                    log.info("📸 [UploadController] Raw Cloudinary result: {}", uploadResult);
                    if (uploadResult != null) {
                        if (uploadResult.get("secure_url") != null) {
                            imageUrl = uploadResult.get("secure_url").toString();
                        } else if (uploadResult.get("url") != null) {
                            imageUrl = uploadResult.get("url").toString().replace("http://", "https://");
                        }
                        if (imageUrl != null) {
                            source = "CLOUDINARY";
                            log.info("✅ [UploadController] Saved to Cloudinary (HTTPS): {}", imageUrl);
                        }
                    }
                } catch (Exception ex) {
                    log.error("⚠️ [UploadController Cloudinary Exception] {}: {}", ex.getClass().getName(), ex.getMessage(), ex);
                }
            } else {
                log.info("📸 [UploadController] Cloudinary not configured (cloudinaryBean={}, cloudName={})", (cloudinary != null), cloudName);
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
                source = "LOCAL_DISK";
                log.info("✅ [UploadController] Saved to local disk: {}", filePath);
            }

            Map<String, String> response = new HashMap<>();
            response.put("fileName", imageUrl);
            response.put("filePath", imageUrl);
            response.put("source", source);
            response.put("message", "Upload thành công");

            log.info("📸 [UploadController] Returning response: {}", response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [UploadController Fatal Error] {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi upload: " + e.getMessage()));
        }
    }
}
