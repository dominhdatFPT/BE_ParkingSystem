package com.swp.parking.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Cấu hình Firebase Admin SDK – khởi tạo FirebaseApp từ service account trên classpath.
 */
@Configuration
public class FirebaseConfig {

    /**
     * Khởi tạo FirebaseApp một lần duy nhất khi ứng dụng start.
     * Đọc file firebase-service-account.json từ src/main/resources.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Tránh khởi tạo trùng nếu bean được gọi nhiều lần
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
