package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(2) // Runs AFTER the Data Seeder (if seeder is enabled)
@RequiredArgsConstructor
@Service
public class AutocompleteWarmup implements CommandLineRunner {

    private final Firestore firestore;
    private final AutocompleteService autocompleteService;

    @Override
    public void run(String... args) {
        log.info(">>> Starting Autocomplete Index Warm-up from Firestore...");
        try {
            // 1. Fetch Categories
            Thread.sleep(5000); // Wait 5 seconds
            List<Category> categories = fetchCollection("categories", Category.class);
            log.info("Loaded {} categories for indexing.", categories.size());

            // 2. Fetch Keywords
            List<Keyword> keywords = fetchCollection("keywords", Keyword.class);
            log.info("Loaded {} keywords for indexing.", keywords.size());

            // 3. Fetch Products
            List<Product> products = fetchCollection("products", Product.class);
            log.info("Loaded {} products for indexing.", products.size());

            // 4. Update the Service Index
            autocompleteService.indexUpdate(categories, keywords, products);
            log.info(">>> Autocomplete Index Warm-up COMPLETED successfully.");

        } catch (Exception e) {
            log.error("Failed to warm up autocomplete index: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper to fetch a collection and map it to a Java Record/Class
     */
    private <T> List<T> fetchCollection(String collectionName, Class<T> targetClass) throws Exception {
        int attempts = 0;
        List<QueryDocumentSnapshot> documents = new ArrayList<>();

        // Try up to 3 times with a delay if the collection is empty
        while (documents.isEmpty() && attempts < 3) {
            ApiFuture<QuerySnapshot> future = firestore.collection(collectionName).get();
            documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                log.warn("Collection '{}' is empty, retrying in 2s...", collectionName);
                Thread.sleep(2000);
                attempts++;
            }
        }

        List<T> results = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            results.add(doc.toObject(targetClass));
        }
        return results;
    }
}