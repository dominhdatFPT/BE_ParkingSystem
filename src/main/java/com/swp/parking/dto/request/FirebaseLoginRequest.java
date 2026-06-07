package com.swp.parking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận Firebase ID Token từ FE sau khi đăng nhập Google thành công.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseLoginRequest {

    /** Firebase ID Token do Firebase Auth trả về cho FE */
    private String idToken;
}
