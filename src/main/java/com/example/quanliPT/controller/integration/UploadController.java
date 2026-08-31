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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class UploadController {

    @Autowired
    private Cloudinary cloudinary;

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

            // Upload lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String imageUrl = uploadResult.get("url").toString();

            System.out.println("✅ [Upload] File đã lưu lên Cloudinary: " + imageUrl);

            // Trả về thông tin file
            Map<String, String> response = new HashMap<>();
            response.put("fileName", imageUrl); // Trả về url luôn
            response.put("filePath", imageUrl);
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

