package com.swp.parking.service;

import com.swp.parking.dto.request.AiChatRequest;
import com.swp.parking.dto.response.AiChatResponse;
import com.swp.parking.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String COLLECTING = "COLLECTING";
    private static final String CONFIRMING = "CONFIRMING";
    private static final String COMPLETED = "COMPLETED";
    private static final String ERROR = "ERROR";
    private static final int MAX_VALUE_LENGTH = 200;

    private final GeminiService geminiService;
    private final AiRegistrationService aiRegistrationService;
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public AiChatResponse chat(Long userId, AiChatRequest request) {
        String sessionId = normalizeSessionId(request.getSessionId());
        ChatSession session = sessions.computeIfAbsent(sessionId, ignored -> new ChatSession(userId));

        synchronized (session) {
            try {
                if (!session.userId.equals(userId)) {
                    throw new AppException(org.springframework.http.HttpStatus.FORBIDDEN,
                            "Phiên chat không thuộc người dùng hiện tại");
                }
                if (CONFIRMING.equals(session.status)) {
                    return handleConfirmation(userId, sessionId, session, request.getMessage());
                }
                if (COMPLETED.equals(session.status)) {
                    return response(sessionId, session,
                            "Phiên đăng ký này đã hoàn tất. Vui lòng bắt đầu một phiên mới nếu bạn muốn đăng ký xe khác.",
                            COMPLETED);
                }

                FieldStep step = nextMissingField(session.data);
                if (step != null) {
                    String extractedValue = geminiService.extractField(
                            request.getMessage(), step.fieldName(), step.description());
                    if (isAcceptable(extractedValue)) {
                        String normalizedValue;
                        try {
                            normalizedValue = normalizeCollectedValue(
                                    step.fieldName(), extractedValue.trim(), session.data);
                        } catch (AppException ex) {
                            return response(sessionId, session,
                                    ex.getMessage() + ". " + step.question(), COLLECTING);
                        }
                        step.setter().accept(session.data, normalizedValue);
                    }
                }

                FieldStep nextStep = nextMissingField(session.data);
                if (nextStep != null) {
                    session.status = COLLECTING;
                    return response(sessionId, session, nextStep.question(), COLLECTING);
                }

                session.status = CONFIRMING;
                return response(sessionId, session, confirmationMessage(session.data), CONFIRMING);
            } catch (GeminiService.GeminiException ex) {
                return response(sessionId, session,
                        ex.getMessage() + ". Vui lòng thử lại sau.", ERROR);
            } catch (AppException ex) {
                return response(sessionId, session, ex.getMessage(), ERROR);
            }
        }
    }

    private AiChatResponse handleConfirmation(
            Long userId, String sessionId, ChatSession session, String message) {
        String action = classifyConfirmationLocally(message);
        if ("UNKNOWN".equals(action)) {
            action = geminiService.classifyConfirmation(message);
        }
        if ("CONFIRM".equals(action)) {
            session.registrationId = aiRegistrationService.createPendingRegistration(userId, session.data);
            session.status = COMPLETED;
            return response(sessionId, session,
                    "Đăng ký thành công. Hồ sơ #" + session.registrationId
                            + " đã được lưu và đang chờ nhân viên phê duyệt.",
                    COMPLETED);
        }
        if ("CANCEL".equals(action)) {
            sessions.remove(sessionId);
            return response(sessionId, session,
                    "Đã hủy thông tin đăng ký. Bạn có thể gửi tin nhắn mới để bắt đầu lại.",
                    COMPLETED);
        }
        return response(sessionId, session,
                "Bạn có đồng ý xác nhận các thông tin trên không? Vui lòng trả lời “đồng ý” hoặc “hủy”.",
                CONFIRMING);
    }

    private String classifyConfirmationLocally(String message) {
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        boolean confirmationWords = normalized.contains("dong y")
                || normalized.contains("xac nhan")
                || normalized.equals("yes")
                || normalized.equals("ok");
        if (normalized.contains("huy") || (normalized.contains("khong") && confirmationWords)) {
            return "CANCEL";
        }
        return confirmationWords ? "CONFIRM" : "UNKNOWN";
    }

    private String normalizeCollectedValue(
            String fieldName, String value, AiChatResponse.CollectedData data) {
        return switch (fieldName) {
            case "phone" -> normalizePhone(value);
            case "vehicleType" -> aiRegistrationService.canonicalVehicleType(value);
            case "licensePlate" -> value.toUpperCase();
            case "packageType" -> aiRegistrationService.canonicalPackage(data.getVehicleType(), value);
            default -> value;
        };
    }

    private String normalizePhone(String value) {
        String normalized = value.replaceAll("[\\s.-]", "");
        if (!normalized.matches("(?:\\+84|0)\\d{9}")) {
            throw new AppException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Số điện thoại chưa hợp lệ");
        }
        return normalized;
    }

    private String normalizeSessionId(String requestedSessionId) {
        if (requestedSessionId == null || requestedSessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestedSessionId.trim();
    }

    private boolean isAcceptable(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_VALUE_LENGTH;
    }

    private FieldStep nextMissingField(AiChatResponse.CollectedData data) {
        FieldStep[] steps = {
                new FieldStep("ownerName", "Họ và tên của chủ xe", AiChatResponse.CollectedData::getOwnerName,
                        AiChatResponse.CollectedData::setOwnerName,
                        "Xin chào! Bạn vui lòng cho biết họ tên đầy đủ của chủ xe?"),
                new FieldStep("phone", "Số điện thoại liên hệ", AiChatResponse.CollectedData::getPhone,
                        AiChatResponse.CollectedData::setPhone,
                        "Số điện thoại liên hệ của bạn là gì?"),
                new FieldStep("vehicleType", "Loại xe, ví dụ ô tô hoặc xe máy", AiChatResponse.CollectedData::getVehicleType,
                        AiChatResponse.CollectedData::setVehicleType,
                        "Bạn muốn đăng ký loại xe nào?"),
                new FieldStep("licensePlate", "Biển số xe", AiChatResponse.CollectedData::getLicensePlate,
                        AiChatResponse.CollectedData::setLicensePlate,
                        "Biển số xe cần đăng ký là gì?"),
                new FieldStep("packageType", "Tên gói đăng ký thẻ xe", AiChatResponse.CollectedData::getPackageType,
                        AiChatResponse.CollectedData::setPackageType,
                        "Bạn muốn đăng ký gói nào?")
        };

        for (FieldStep step : steps) {
            String value = step.getter().apply(data);
            if (value == null || value.isBlank()) {
                return step;
            }
        }
        return null;
    }

    private String confirmationMessage(AiChatResponse.CollectedData data) {
        return """
                Mình đã nhận đủ thông tin:
                - Chủ xe: %s
                - Số điện thoại: %s
                - Loại xe: %s
                - Biển số: %s
                - Gói đăng ký: %s
                Bạn có đồng ý xác nhận các thông tin trên không?
                """.formatted(data.getOwnerName(), data.getPhone(), data.getVehicleType(),
                data.getLicensePlate(), data.getPackageType()).trim();
    }

    private AiChatResponse response(
            String sessionId, ChatSession session, String reply, String responseStatus) {
        return AiChatResponse.builder()
                .sessionId(sessionId)
                .reply(reply)
                .status(responseStatus)
                .registrationId(session.registrationId)
                .collectedData(copyData(session.data))
                .build();
    }

    private AiChatResponse.CollectedData copyData(AiChatResponse.CollectedData data) {
        return AiChatResponse.CollectedData.builder()
                .ownerName(data.getOwnerName())
                .phone(data.getPhone())
                .vehicleType(data.getVehicleType())
                .licensePlate(data.getLicensePlate())
                .packageType(data.getPackageType())
                .build();
    }

    private record FieldStep(
            String fieldName,
            String description,
            Function<AiChatResponse.CollectedData, String> getter,
            BiConsumer<AiChatResponse.CollectedData, String> setter,
            String question) {
    }

    private static class ChatSession {
        private final AiChatResponse.CollectedData data = new AiChatResponse.CollectedData();
        private final Long userId;
        private String status = COLLECTING;
        private Long registrationId;

        private ChatSession(Long userId) {
            this.userId = userId;
        }
    }
}
