package com.swp.parking.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        GoogleCredentials credentials = loadCredentials();
        if (credentials == null) {
            log.warn("Firebase credentials not found — Google login will be unavailable");
            return null;
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials loadCredentials() {
        try {
            if (StringUtils.hasText(firebaseJson)) {
                try (InputStream is = new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8))) {
                    return GoogleCredentials.fromStream(is);
                }
            }

            if (StringUtils.hasText(firebaseFilePath)) {
                try (InputStream is = new FileInputStream(firebaseFilePath)) {
                    return GoogleCredentials.fromStream(is);
                }
            }

            String googleCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (StringUtils.hasText(googleCreds)) {
                try (InputStream is = new FileInputStream(googleCreds)) {
                    return GoogleCredentials.fromStream(is);
                }
            }

            ClassPathResource localFallback = new ClassPathResource("firebase-service-account.json");
            if (localFallback.exists()) {
                try (InputStream is = localFallback.getInputStream()) {
                    return GoogleCredentials.fromStream(is);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load Firebase credentials: {}", e.getMessage());
        }

        return null;
    }
}
