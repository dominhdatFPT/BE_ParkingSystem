package com.swp.parking.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Configures Firebase Admin SDK without requiring service account secrets in Git.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-json:${FIREBASE_SERVICE_ACCOUNT_JSON:}}")
    private String firebaseJson;

    @Value("${firebase.service-account-path:${FIREBASE_SERVICE_ACCOUNT_PATH:}}")
    private String firebaseFilePath;

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        loadCredentials().ifPresentOrElse(credentials -> {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized");
        }, () -> log.warn("Firebase credentials not found; Google login will be unavailable"));
    }

    private Optional<GoogleCredentials> loadCredentials() {
        if (StringUtils.hasText(firebaseJson)) {
            try (InputStream serviceAccount = new ByteArrayInputStream(
                    firebaseJson.getBytes(StandardCharsets.UTF_8))) {
                return Optional.of(GoogleCredentials.fromStream(serviceAccount));
            } catch (IOException ex) {
                log.warn("Unable to load Firebase credentials from FIREBASE_SERVICE_ACCOUNT_JSON", ex);
            }
        }

        if (StringUtils.hasText(firebaseFilePath)) {
            try (InputStream serviceAccount = new FileInputStream(firebaseFilePath)) {
                return Optional.of(GoogleCredentials.fromStream(serviceAccount));
            } catch (IOException ex) {
                log.warn("Unable to load Firebase credentials from path '{}'", firebaseFilePath, ex);
            }
        }

        String googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(googleApplicationCredentials)) {
            try (InputStream serviceAccount = new FileInputStream(googleApplicationCredentials)) {
                return Optional.of(GoogleCredentials.fromStream(serviceAccount));
            } catch (IOException ex) {
                log.warn("Unable to load Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS '{}'",
                        googleApplicationCredentials, ex);
            }
        }

        for (String resourceName : new String[]{"firebase-service-account.json", "firebase-service-account"}) {
            ClassPathResource localFallback = new ClassPathResource(resourceName);
            if (localFallback.exists()) {
                try (InputStream serviceAccount = localFallback.getInputStream()) {
                    return Optional.of(GoogleCredentials.fromStream(serviceAccount));
                } catch (IOException ex) {
                    log.warn("Unable to load Firebase credentials from classpath resource '{}'", resourceName, ex);
                }
            }
        }

        try {
            return Optional.of(GoogleCredentials.getApplicationDefault());
        } catch (IOException ex) {
            log.warn("Unable to load Google application default credentials", ex);
            return Optional.empty();
        }
    }
}
