package com.cartshare.backend.infrastructure.firestore;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@EnableConfigurationProperties(FirestoreProperties.class)
public class FirestoreConfig {
    private static final Logger log = LoggerFactory.getLogger(FirestoreConfig.class);

    @Bean
    Firestore firestore(FirestoreProperties properties) throws IOException {
        String projectId = properties.getProjectId();
        String keyPath = properties.getKeyPath();

        log.info("🔧 Initializing Firestore for project: [{}] using key at: [{}]", projectId, keyPath);

        // Safety check to ensure Spring actually injected the values
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("Firestore Config Error: projectId is null or blank. " +
                    "Ensure 'google.cloud.project-id' is defined in application.properties");
        }

        if (keyPath == null || keyPath.isBlank()) {
            throw new IllegalStateException("Firestore Config Error: keyPath is null or blank. " +
                    "Ensure 'google.cloud.key-path' is defined in application.properties");
        }

        try (FileInputStream serviceAccount = new FileInputStream(keyPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            return FirestoreOptions.newBuilder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build()
                    .getService();
        } catch (IOException e) {
            log.error("❌ Critical: Could not load Firestore key file at: {}", keyPath);
            throw e;
        }
    }
}
