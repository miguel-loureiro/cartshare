package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordService {

    private final Firestore firestore;

    /**
     * Create keywords for a product.
     *
     * For each keyword:
     * 1. Check if it already exists
     * 2. If not, create a new Keyword entity
     *
     * Keywords are used for autocomplete suggestions.
     *
     * @param productName Name of the product (for logging)
     * @param categoryId Category ID to assign to keywords
     * @param searchKeywords List of keywords to create
     */
    public void createKeywordsForProduct(String productName, String categoryId, List<String> searchKeywords) {
        try {
            log.info("🔑 Creating keywords for product: {} ({})", productName, searchKeywords.size());

            for (String keyword : searchKeywords) {
                createKeywordIfNotExists(keyword, categoryId);
            }

            log.info("✅ Keywords created for product: {}", productName);

        } catch (InterruptedException | ExecutionException e) {
            log.error("⚠️ Error creating keywords: ", e);
            // Don't fail the product creation if keywords fail
        }
    }

    /**
     * Create a keyword if it doesn't already exist
     */
    private void createKeywordIfNotExists(String keyword, String categoryId)
            throws ExecutionException, InterruptedException {

        // Check if keyword exists
        long exists = firestore.collection("keywords")
                .whereEqualTo("keyword", keyword)
                .get()
                .get()
                .size();

        if (exists == 0) {
            Keyword kw = new Keyword(keyword, categoryId);
            String keywordId = UUID.randomUUID().toString();
            firestore.collection("keywords").document(keywordId).set(kw).get();
            log.debug("✅ Keyword created: {}", keyword);
        }
    }

    /**
     * Get all keywords
     */
    public List<Keyword> getAllKeywords() throws ExecutionException, InterruptedException {
        return firestore.collection("keywords")
                .get()
                .get()
                .toObjects(Keyword.class);
    }

    /**
     * Get keywords by category
     */
    public List<Keyword> getKeywordsByCategory(String categoryId) throws ExecutionException, InterruptedException {
        return firestore.collection("keywords")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .get()
                .toObjects(Keyword.class);
    }

    /**
     * Get keyword count
     */
    public long getKeywordCount() throws ExecutionException, InterruptedException {
        return firestore.collection("keywords").get().get().size();
    }
}
