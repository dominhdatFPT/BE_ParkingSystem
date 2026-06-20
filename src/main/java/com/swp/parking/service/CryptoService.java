package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mã hóa/giải mã chuỗi bằng AES-256-CBC.
 * Dùng để bảo vệ momoPartnerToken trước khi lưu vào DB.
 *
 * Định dạng lưu trữ: Base64( IV[16 bytes] + CipherText )
 * IV được sinh ngẫu nhiên mỗi lần encrypt, nên cùng plainText sẽ cho cipherText khác nhau.
 */
@Slf4j
@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    @Value("${crypto.aes-secret-key}")
    private String rawKey;

    /**
     * Mã hóa plainText thành chuỗi Base64(IV + CipherText).
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = buildKeySpec();
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Ghép IV vào đầu để lưu cùng ciphertext
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES encrypt failed: {}", e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi mã hóa dữ liệu");
        }
    }

    /**
     * Giải mã chuỗi Base64(IV + CipherText) trở lại plainText gốc.
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Dữ liệu mã hóa không hợp lệ");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            SecretKeySpec keySpec = buildKeySpec();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("AES decrypt failed: {}", e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi giải mã dữ liệu");
        }
    }

    /**
     * Tạo AES-256 key từ rawKey bằng cách hash SHA-256 → đảm bảo luôn đủ 32 bytes.
     */
    private SecretKeySpec buildKeySpec() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
