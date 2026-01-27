package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.infrastructure.excel.ExcelReader;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordService {
    private final Firestore firestore;
    /**
     * Create keywords for a product.
     * Keywords are used for autocomplete suggestions.
     * @param productName Name of the product (for logging)
     * @param searchKeywords List of keywords to create
     */
    public void createKeywordsForProduct(String productName, List<String> searchKeywords) {
        try {
            log.info("🔑 Creating keywords for product: {} ({})", productName, searchKeywords.size());
            for (String keyword : searchKeywords) {
                createKeywordIfNotExists(keyword);
            }
            log.info("✅ Keywords created for product: {}", productName);
        } catch (Exception e) {
            log.error("⚠️ Error creating keywords for product {}: ", productName, e);
        }
    }
    /**
     * Create a keyword if it doesn't already exist.
     * Uses the keyword itself as the document ID for O(1) existence checks.
     */
    private void createKeywordIfNotExists(String keyword) throws ExecutionException, InterruptedException {
        String docId = ExcelReader.toSafeId(keyword);
        DocumentReference docRef = firestore.collection("keywords").document(docId);
        // Using a direct document check is faster and cheaper than a query
        if (!docRef.get().get().exists()) {
            Keyword kw = new Keyword(keyword);
            docRef.set(kw).get();
            log.debug("✅ Keyword created: {}", keyword);
        }
    }
    /**
     * Get all keywords
     */
    public List<Keyword> getAllKeywords() throws ExecutionException, InterruptedException {
        return firestore.collection("keywords").get().get().toObjects(Keyword.class);
    }
    /**
     * Get total keyword count
     */
    public long getKeywordCount() throws ExecutionException, InterruptedException {
        return firestore.collection("keywords").get().get().size();
    }
}