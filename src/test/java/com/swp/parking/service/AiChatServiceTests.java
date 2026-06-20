package com.swp.parking.service;

import com.swp.parking.dto.request.AiChatRequest;
import com.swp.parking.dto.response.AiChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatServiceTests {

    private GeminiService geminiService;
    private AiRegistrationService aiRegistrationService;
    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        geminiService = mock(GeminiService.class);
        aiRegistrationService = mock(AiRegistrationService.class);
        aiChatService = new AiChatService(geminiService, aiRegistrationService);
        when(aiRegistrationService.canonicalVehicleType(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRegistrationService.canonicalPackage(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(aiRegistrationService.createPendingRegistration(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(123L);
    }

    @Test
    void collectsOneFieldPerMessageThenAsksForConfirmation() {
        when(geminiService.extractField(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiChatResponse first = send(null, "Nguyễn Văn An");
        assertThat(first.getStatus()).isEqualTo("COLLECTING");
        assertThat(first.getCollectedData().getOwnerName()).isEqualTo("Nguyễn Văn An");
        assertThat(first.getCollectedData().getPhone()).isNull();

        String sessionId = first.getSessionId();
        send(sessionId, "0901234567");
        send(sessionId, "Xe máy");
        send(sessionId, "59A1-123.45");
        AiChatResponse finalCollection = send(sessionId, "Gói tháng");

        assertThat(finalCollection.getStatus()).isEqualTo("CONFIRMING");
        assertThat(finalCollection.getCollectedData().getPackageType()).isEqualTo("Gói tháng");

        AiChatResponse completed = send(sessionId, "Đồng ý");
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getRegistrationId()).isEqualTo(123L);
    }

    @Test
    void returnsErrorStatusWithoutLosingSessionWhenGeminiFails() {
        when(geminiService.extractField(anyString(), anyString(), anyString()))
                .thenThrow(new GeminiService.GeminiException("Không thể kết nối Gemini lúc này"));

        AiChatResponse response = send(null, "Xin chào");

        assertThat(response.getStatus()).isEqualTo("ERROR");
        assertThat(response.getSessionId()).isNotBlank();
        assertThat(response.getCollectedData().getOwnerName()).isNull();
    }

    private AiChatResponse send(String sessionId, String message) {
        return aiChatService.chat(1L, AiChatRequest.builder()
                .sessionId(sessionId)
                .message(message)
                .build());
    }
}
