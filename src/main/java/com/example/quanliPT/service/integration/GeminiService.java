package com.example.quanliPT.service.integration;

import com.example.quanliPT.dto.chat.ChatRequest;
import com.example.quanliPT.dto.chat.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String SYSTEM_PROMPT = "Bạn là trợ lý ảo thông minh của website Smart Phòng Trọ (hệ thống quản lý nhà trọ). " +
            "Nhiệm vụ: trả lời câu hỏi của khách hàng bằng tiếng Việt, ngắn gọn, thân thiện, hữu ích. " +
            "Giới thiệu các chức năng chính: xem phòng trống, đặt phòng, tạo hợp đồng, hóa đơn, thanh toán online (QR PayOS/Stripe), yêu cầu bảo trì, thông báo. " +
            "Nếu câu hỏi ngoài phạm vi, hãy trả lời: 'Vui lòng liên hệ Admin qua hotline 0123.456.789 hoặc gửi tin nhắn trong mục Liên hệ.'";

    public ChatResponse processChat(ChatRequest request) {
        try {
            String apiUrl = GEMINI_API_URL + geminiApiKey;

            ObjectNode requestBody = objectMapper.createObjectNode();

            // 1. System Instruction
            ObjectNode systemInstruction = requestBody.putObject("systemInstruction");
            systemInstruction.put("role", "system");
            ArrayNode sysParts = systemInstruction.putArray("parts");
            sysParts.addObject().put("text", SYSTEM_PROMPT);

            // 2. Contents (History + Current Message)
            ArrayNode contents = requestBody.putArray("contents");

            if (request.getHistory() != null) {
                // Chỉ lấy 6 tin nhắn gần nhất để tiết kiệm token
                List<ChatRequest.ChatMessage> history = request.getHistory();
                int startIndex = Math.max(0, history.size() - 6);
                for (int i = startIndex; i < history.size(); i++) {
                    ChatRequest.ChatMessage msg = history.get(i);
                    ObjectNode contentNode = contents.addObject();
                    contentNode.put("role", "bot".equals(msg.getRole()) || "model".equals(msg.getRole()) ? "model" : "user");
                    contentNode.putArray("parts").addObject().put("text", msg.getText());
                }
            }

            // Dòng tin nhắn mới nhất
            ObjectNode latestMsg = contents.addObject();
            latestMsg.put("role", "user");
            latestMsg.putArray("parts").addObject().put("text", request.getMessage());

            // 3. Generation Config
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("maxOutputTokens", 500);
            generationConfig.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.info("Sending request to Gemini API...");
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String replyText = rootNode.path("candidates")
                    .get(0).path("content").path("parts")
                    .get(0).path("text").asText();

            return new ChatResponse(replyText);

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return new ChatResponse("⚠️ Rất tiếc, mình đang gặp sự cố kết nối AI. Vui lòng thử lại sau hoặc liên hệ Admin qua hotline 0123.456.789.");
        }
    }
}



