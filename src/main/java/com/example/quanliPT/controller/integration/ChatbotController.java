package com.example.quanliPT.controller.integration;

import com.example.quanliPT.dto.chat.ChatRequest;
import com.example.quanliPT.dto.chat.ChatResponse;
import com.example.quanliPT.service.integration.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final GeminiService geminiService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Nội dung tin nhắn không được để trống."));
        }
        ChatResponse response = geminiService.processChat(request);
        return ResponseEntity.ok(response);
    }
}



