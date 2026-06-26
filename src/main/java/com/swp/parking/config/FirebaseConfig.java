package com.swp.parking.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Configures Firebase Admin SDK without requiring service account secrets in Git.
 */
@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
    private String firebaseJson;

    @Value("${FIREBASE_SERVICE_ACCOUNT_PATH:}")
    private String firebaseFilePath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(loadCredentials())
                .build();
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (StringUtils.hasText(firebaseJson)) {
            try (InputStream serviceAccount = new ByteArrayInputStream(
                    firebaseJson.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        if (StringUtils.hasText(firebaseFilePath)) {
            try (InputStream serviceAccount = new FileInputStream(firebaseFilePath)) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        String googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(googleApplicationCredentials)) {
            try (InputStream serviceAccount = new FileInputStream(googleApplicationCredentials)) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        ClassPathResource localFallback = new ClassPathResource("firebase-service-account.json");
        if (localFallback.exists()) {
            try (InputStream serviceAccount = localFallback.getInputStream()) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        return GoogleCredentials.getApplicationDefault();
    }
}
