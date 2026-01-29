package com.cartshare.backend.infrastructure.firestore;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties class for Firestore configuration.
 * This uses Spring's @ConfigurationProperties for better property binding.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "google.cloud")
public class FirestoreProperties {
    private String projectId;
    private String keyPath;

    @Override
    public String toString() {
        return "FirestoreProperties{" +
                "projectId='" + projectId + '\'' +
                ", keyPath='" + keyPath + '\'' +
                '}';
    }
}

