package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @Size(max = 100, message = "Session ID không hợp lệ")
    private String sessionId;

    @NotBlank(message = "Message không được để trống")
    @Size(max = 1000, message = "Message không được vượt quá 1000 ký tự")
    private String message;
}
