package com.cartshare.backend.infrastructure.firestore;

import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class FirestoreHealthChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FirestoreHealthChecker.class);
    private final Firestore firestore;

    public FirestoreHealthChecker(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("🔍 Testing Firestore connectivity...");
        try {
            // Attempt to retrieve a dummy document reference
            // This doesn't cost money/read operations if we don't call .get()
            // But we call .listCollections() or a simple check to verify auth
            firestore.listCollections().iterator().hasNext();

            log.info("Successfully connected to Firestore project: {}",
                    firestore.getOptions().getProjectId());
        } catch (Exception e) {
            log.error("❌ Firestore connection test failed!");
            log.error("Reason: {}", e.getMessage());
            log.error("Check if the key at your path is valid and has 'Cloud Datastore User' permissions.");
        }
    }
}
