package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequest {

    @NotBlank(message = "Vui lòng chọn chủ đề hỗ trợ")
    @Size(max = 50, message = "Chủ đề hỗ trợ không được vượt quá 50 ký tự")
    private String subject;

    @NotBlank(message = "Vui lòng nhập nội dung sự cố")
    @Size(max = 5000, message = "Nội dung sự cố không được vượt quá 5000 ký tự")
    private String message;
}
