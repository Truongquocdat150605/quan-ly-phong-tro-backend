package com.example.quanliPT.dto.chat;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private List<ChatMessage> history;
    private String message;

    @Data
    public static class ChatMessage {
        private String role;
        private String text;
    }
}

