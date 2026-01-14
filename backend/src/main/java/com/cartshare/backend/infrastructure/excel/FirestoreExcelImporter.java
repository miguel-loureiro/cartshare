package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.service.ProductCategoryMatcher;
import com.google.cloud.firestore.Firestore;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class FirestoreExcelImporter {

    private static final Logger log = LoggerFactory.getLogger(FirestoreExcelImporter.class);
    private final Firestore firestore;
    @Setter
    private boolean dryRun = false;

    public FirestoreExcelImporter(Firestore firestore) {
        this.firestore = firestore;
    }

    public void importCategories(InputStream is) throws Exception {
        ExcelReader.read(is).forEach(row -> {
            if (row.size() >= 4) {
                // ID and Classification are sanitized to CAPS_SNAKE_CASE
                String id = ExcelReader.toSafeId(row.get(0));
                String name = row.get(1);
                String classification = ExcelReader.toSafeId(row.get(2));
                int priority = Integer.parseInt(row.get(3));

                Category cat = new Category(id, name, classification, priority);

                if (dryRun) {
                    log.info("[DRY RUN] Skip Firestore write: categories/{} -> {}", id, cat);
                } else {
                    firestore.collection("categories").document(id).set(cat);
                }
            }
        });
    }

    public void importKeywords(InputStream is) throws Exception {
        ExcelReader.read(is).forEach(r -> {
            if (r.size() >= 2) {
                String keywordId = r.get(0);
                var data = Map.of(
                        "keyword", keywordId,
                        "categoryId", r.get(1)
                );

                if (dryRun) {
                    log.info("[DRY RUN] Skip Firestore write: keywords/{} -> {}", keywordId, data);
                } else {
                    firestore.collection("keywords").document(keywordId).set(data);
                }
            }
        });
    }

    public void importProducts(InputStream is, List<Keyword> keywords) throws Exception {
        ExcelReader.read(is).forEach(r -> {
            if (r.size() >= 2) {
                String productName = r.get(0);
                String rawCategory = r.get(1);

                String category = ProductCategoryMatcher.resolveCategory(
                        productName,
                        keywords,
                        rawCategory
                );

                String docId = productName.replace(" ", "_");

                // Generate keywords for better searching on Mobile
                List<String> searchKeywords = generateSearchKeywords(productName);

                var data = Map.of(
                        "productName", productName,
                        "categoryId", category,
                        "searchKeywords", searchKeywords // New field for Firestore
                );

                if (dryRun) {
                    log.info("[DRY RUN] Skip Firestore write: products/{} -> {}", docId, data);
                } else {
                    firestore.collection("products").document(docId).set(data);
                }
            }
        });
    }

    private List<String> generateSearchKeywords(String name) {
        if (name == null || name.isBlank()) return List.of();

        // Split by spaces, remove punctuation, convert to lowercase
        return Arrays.stream(name.toLowerCase().split("\\s+"))
                .map(word -> word.replaceAll("[^a-zA-Z0-9áéíóúâêîôûàèìòùçãõ]", ""))
                .filter(word -> word.length() > 2) // Optional: skip tiny words like "de", "do"
                .distinct()
                .toList();
    }
}
