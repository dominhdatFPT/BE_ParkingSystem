package com.swp.parking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String extractField(String message, String fieldName, String fieldDescription) {
        String prompt = """
                Bạn là trợ lý đăng ký thẻ xe cho hệ thống quản lý bãi đỗ xe.
                Hãy đọc tin nhắn của người dùng và chỉ trích xuất thông tin được yêu cầu.

                Trường cần lấy: %s
                Ý nghĩa: %s
                Tin nhắn người dùng: %s

                Quy tắc bắt buộc:
                - Không suy đoán, không tự tạo dữ liệu.
                - Chỉ lấy giá trị nếu người dùng nói rõ trong chính tin nhắn.
                - Không lấy các trường khác dù người dùng có cung cấp.
                - Giữ nguyên nội dung, chỉ được bỏ khoảng trắng thừa.
                - Trả về JSON duy nhất theo dạng {"value": "..."}.
                - Nếu không có giá trị rõ ràng, trả về {"value": null}.
                """.formatted(fieldName, fieldDescription, message);

        JsonNode result = callGemini(prompt);
        JsonNode valueNode = result.get("value");
        if (valueNode == null || valueNode.isNull() || !valueNode.isTextual()) {
            return null;
        }

        String value = valueNode.asText().trim();
        return value.isBlank() ? null : value;
    }

    public String classifyConfirmation(String message) {
        String prompt = """
                Phân loại câu trả lời của người dùng khi xác nhận thông tin đăng ký thẻ xe.
                Tin nhắn người dùng: %s

                Chỉ trả về JSON theo dạng {"action":"CONFIRM"},
                {"action":"CANCEL"}, hoặc {"action":"UNKNOWN"}.
                Chỉ chọn CONFIRM khi người dùng đồng ý rõ ràng; không được suy đoán.
                """.formatted(message);

        JsonNode result = callGemini(prompt);
        String action = result.path("action").asText("UNKNOWN").toUpperCase();
        return switch (action) {
            case "CONFIRM", "CANCEL" -> action;
            default -> "UNKNOWN";
        };
    }

    private JsonNode callGemini(String prompt) {
        validateConfiguration();

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json")
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = extractResponseText(response);
            return objectMapper.readTree(removeMarkdownFence(text));
        } catch (Exception ex) {
            throw new GeminiException("Không thể kết nối Gemini lúc này", ex);
        }
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiException("GEMINI_API_KEY chưa được cấu hình");
        }
        if (model == null || model.isBlank()) {
            throw new GeminiException("GEMINI_MODEL chưa được cấu hình");
        }
    }

    private String extractResponseText(JsonNode response) {
        if (response == null) {
            throw new GeminiException("Gemini trả về phản hồi rỗng");
        }

        JsonNode textNode = response.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");
        if (!textNode.isTextual() || textNode.asText().isBlank()) {
            throw new GeminiException("Gemini không trả về nội dung hợp lệ");
        }
        return textNode.asText();
    }

    private String removeMarkdownFence(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned;
    }

    public static class GeminiException extends RuntimeException {
        public GeminiException(String message) {
            super(message);
        }

        public GeminiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
