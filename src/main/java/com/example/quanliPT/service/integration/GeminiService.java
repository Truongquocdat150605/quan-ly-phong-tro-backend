package com.example.quanliPT.service.integration;

import com.example.quanliPT.dto.chat.ChatRequest;
import com.example.quanliPT.dto.chat.ChatResponse;
import com.example.quanliPT.model.Room;
import com.example.quanliPT.model.enums.RoomStatus;
import com.example.quanliPT.repository.room.RoomRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RoomRepository roomRepository;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý AI của hệ thống Smart Phòng Trọ (chỉ quản lý một tòa nhà cụ thể).
            Nhiệm vụ: Tư vấn các phòng đang trống, hướng dẫn thanh toán.
            QUY TẮC NGHIÊM NGẶT:
            1. KHÔNG trả lời các câu hỏi tào lao ngoài luồng.
            2. Trả lời ngắn gọn, thân thiện.
            3. KHÔNG BAO GIỜ hỏi khách muốn tìm phòng ở "khu vực nào", "quận nào" vì bạn chỉ quản lý một tòa nhà.
            4. CHỈ giới thiệu các phòng được cung cấp trong phần CONTEXT. Nếu CONTEXT trống, hãy mời khách vào menu "Phòng trọ" để xem chi tiết.
            """;

    public ChatResponse processChat(ChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        List<Room> matchedRooms = findMatchedRooms(message);

        if (!isGeminiKeyConfigured()) {
            log.warn("Gemini API key is missing. Using local chatbot fallback.");
            return buildLocalFallbackResponse(message, matchedRooms);
        }

        try {
            ObjectNode requestBody = buildGeminiRequest(request, matchedRooms);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.info("Sending request to Gemini API model={}", geminiModel);
            String apiUrl = String.format(GEMINI_API_URL, geminiModel, geminiApiKey.trim());
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String replyText = rootNode.path("candidates")
                    .path(0).path("content").path("parts")
                    .path(0).path("text").asText(null);
            if (replyText == null || replyText.isBlank()) {
                log.warn("Gemini response has no text: {}", response.getBody());
                return buildLocalFallbackResponse(message, matchedRooms);
            }

            return new ChatResponse(replyText, matchedRooms);
        } catch (Exception e) {
            log.error("Error calling Gemini API. Using local chatbot fallback.", e);
            return buildLocalFallbackResponse(message, matchedRooms);
        }
    }

    private ObjectNode buildGeminiRequest(ChatRequest request, List<Room> matchedRooms) {
        ObjectNode requestBody = objectMapper.createObjectNode();

        ObjectNode systemInstruction = requestBody.putObject("systemInstruction");
        systemInstruction.putArray("parts").addObject().put("text", SYSTEM_PROMPT + buildRoomContext(matchedRooms));

        ArrayNode contents = requestBody.putArray("contents");
        if (request.getHistory() != null) {
            List<ChatRequest.ChatMessage> history = request.getHistory();
            int startIndex = Math.max(0, history.size() - 6);
            for (int i = startIndex; i < history.size(); i++) {
                ChatRequest.ChatMessage msg = history.get(i);
                ObjectNode contentNode = contents.addObject();
                contentNode.put("role", "bot".equals(msg.getRole()) || "model".equals(msg.getRole()) ? "model" : "user");
                contentNode.putArray("parts").addObject().put("text", msg.getText());
            }
        }

        ObjectNode latestMsg = contents.addObject();
        latestMsg.put("role", "user");
        latestMsg.putArray("parts").addObject().put("text", request.getMessage());

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", 500);
        generationConfig.put("temperature", 0.3); // Giảm temperature để trả lời nghiêm túc hơn
        return requestBody;
    }

    private List<Room> findMatchedRooms(String message) {
        String userMsg = message.toLowerCase();
        
        // Cập nhật thêm các từ khóa liệt kê, danh sách
        boolean isAskingForRoom = userMsg.contains("phòng") || userMsg.contains("phong") || userMsg.contains("giá") || userMsg.contains("gia")
                || userMsg.contains("thuê") || userMsg.contains("thue") || userMsg.contains("trọ") || userMsg.contains("tro") || userMsg.contains("tìm") || userMsg.contains("tim")
                || userMsg.contains("liệt kê") || userMsg.contains("liet ke") || userMsg.contains("xem") || userMsg.contains("danh sách") || userMsg.contains("danh sach") || userMsg.contains("trống");
                
        if (!isAskingForRoom) {
            return List.of(); // Trả về rỗng để khỏi tốn Token chèn vào prompt
        }

        List<String> keywords = Arrays.stream(userMsg.split("\\s+"))
                .filter(keyword -> keyword.length() > 2)
                .collect(Collectors.toList());
        List<Room> availableRooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);

        if (keywords.isEmpty()) {
            return availableRooms.stream().limit(3).collect(Collectors.toList());
        }

        return availableRooms.stream()
                .filter(room -> keywords.stream().anyMatch(keyword ->
                        safe(room.getType()).contains(keyword)
                                || safe(room.getRoomNumber()).contains(keyword)
                                || String.valueOf(room.getPrice()).contains(keyword)
                                || safe(room.getDescription()).contains(keyword)))
                .limit(3) // Chỉ lấy tối đa 3 phòng để tiết kiệm token
                .collect(Collectors.toList());
    }

    private String buildRoomContext(List<Room> matchedRooms) {
        if (matchedRooms == null || matchedRooms.isEmpty()) {
            return "";
        }

        return "\n\nCONTEXT - DANH SACH PHONG TRONG PHU HOP:\n"
                + matchedRooms.stream()
                .map(room -> "- [ROOM:" + room.getId() + "] Phong " + room.getRoomNumber()
                        + ", loai: " + room.getType()
                        + ", gia: " + room.getPrice() + " VND/thang"
                        + ", dien tich: " + room.getArea() + "m2")
                .collect(Collectors.joining("\n"));
    }

    private boolean isGeminiKeyConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private ChatResponse buildLocalFallbackResponse(String message, List<Room> matchedRooms) {
        String msg = safe(message);

        if (msg.contains("phong") || msg.contains("phòng") || msg.contains("tro")
                || msg.contains("trọ") || msg.contains("gia") || msg.contains("giá")) {
            if (matchedRooms == null || matchedRooms.isEmpty()) {
                return new ChatResponse("Hien tai minh chua tim thay phong trong phu hop. Ban co the vao muc Phong tro de loc theo gia, dien tich va loai phong.", List.of());
            }

            String roomText = matchedRooms.stream()
                    .limit(3)
                    .map(room -> "Phong " + room.getRoomNumber() + " gia " + room.getPrice()
                            + " VND/thang [ROOM:" + room.getId() + "]")
                    .collect(Collectors.joining("\n"));
            return new ChatResponse("Minh dang dung che do tu van noi bo vi Gemini API key chua hop le. Mot so phong trong phu hop:\n" + roomText, matchedRooms);
        }

        if (msg.contains("thanh toan") || msg.contains("thanh toán") || msg.contains("hoa don") || msg.contains("hóa đơn")) {
            return new ChatResponse("Ban co the vao muc Hoa don cua toi de xem chi tiet va thanh toan. He thong ho tro Stripe, PayOS va tien mat. Neu chon tien mat, admin se xac nhan sau khi thu tien that.", matchedRooms);
        }

        if (msg.contains("bao tri") || msg.contains("bảo trì") || msg.contains("hong") || msg.contains("hỏng")) {
            return new ChatResponse("Neu phong co su co dien, nuoc hoac thiet bi, ban vao muc Yeu cau bao tri de gui noi dung cho admin xu ly.", matchedRooms);
        }

        return new ChatResponse("Minh dang dung che do tra loi noi bo vi Gemini API key chua hop le. Ban co the hoi ve phong trong, dat phong, hop dong, hoa don, thanh toan hoac bao tri.", matchedRooms);
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
