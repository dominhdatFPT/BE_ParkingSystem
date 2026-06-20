package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentReplyRequest {

    @NotBlank(message = "Vui lòng nhập tiêu đề phản hồi")
    @Size(max = 200, message = "Tiêu đề phản hồi không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Vui lòng nhập nội dung phản hồi")
    @Size(max = 5000, message = "Nội dung phản hồi không được vượt quá 5000 ký tự")
    private String message;

    @NotBlank(message = "Vui lòng chọn trạng thái")
    @Pattern(regexp = "IN_PROGRESS|REPLIED|RESOLVED|CLOSED", message = "Trạng thái xử lý không hợp lệ")
    private String status;
}
