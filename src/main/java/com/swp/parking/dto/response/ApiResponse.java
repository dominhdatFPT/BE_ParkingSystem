package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Wrapper chuẩn cho mọi phản hồi API (thành công hoặc lỗi nghiệp vụ).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    /**
     * Tạo phản hồi thành công kèm dữ liệu.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    /**
     * Tạo phản hồi lỗi với thông báo mô tả.
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
