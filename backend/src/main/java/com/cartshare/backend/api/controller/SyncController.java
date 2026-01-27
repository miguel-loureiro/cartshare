package com.cartshare.backend.api.controller;

import com.cartshare.backend.api.v1.ContributeProductRequest;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.KeywordService;
import com.cartshare.backend.core.service.ProductContributionService;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SyncController {

    private final Firestore firestore;
    private final ProductContributionService productContributionService;
    private final KeywordService keywordService;

    // ===== DATA SYNC =====
    /**
     * Initial sync: Download all data for mobile app
     */
    @GetMapping("/initial")
    public ResponseEntity<?> initialSync() {
        try {
            log.info("📱 Initial sync requested");
            List<Keyword> keywords = keywordService.getAllKeywords();
            List<Product> products = productContributionService.getAllProducts();
            Map<String, Object> syncData = Map.of(
                    "keywords", keywords,
                    "products", products,
                    "timestamp", System.currentTimeMillis(),
                    "syncStatus", "success"
            );

            log.info("✅ Initial sync: {} keywords, {} products",
                    keywords.size(), products.size());

            return ResponseEntity.ok(syncData);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Sync failed: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("syncStatus", "failed", "error", "Failed to prepare sync data"));
        }
    }

    // ===== PRODUCT CONTRIBUTION =====

    /**
     * User contributes a new product
     */
    @PostMapping("/contribute/product")
    public ResponseEntity<?> contributeProduct(@RequestBody ContributeProductRequest request) {
        try {
            Product product = productContributionService.contributeProduct(
                    request.getProductName(),
                    request.getCategoryId()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(buildProductResponse(product));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error adding product: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add product"));
        }
    }

    /**
     * Check if product exists
     */
    @GetMapping("/product/exists")
    public ResponseEntity<?> checkProductExists(@RequestParam String productName) {
        try {
            if (productName == null || productName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product name is required"));
            }

            boolean exists = productContributionService.productExists(productName);
            log.info("🔍 Product '{}' exists: {}", productName, exists);

            return ResponseEntity.ok(Map.of(
                    "productName", productName,
                    "exists", exists
            ));

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error checking product: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check product"));

        }

    }



    // ===== PRODUCTS BY TYPE =====

    /**
     * Get only official products
     */
    @GetMapping("/products/official")
    public ResponseEntity<?> getOfficialProducts() {
        try {
            List<Product> products = productContributionService.getOfficialProducts();
            return ResponseEntity.ok(Map.of(
                    "products", products,
                    "count", products.size(),
                    "type", "official"
            ));
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error fetching official products: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch official products"));
        }
    }

    /**
     * Get only user-contributed products
     */
    @GetMapping("/products/user-contributed")
    public ResponseEntity<?> getUserContributedProducts() {
        try {
            List<Product> products = productContributionService.getUserContributedProducts();
            return ResponseEntity.ok(Map.of(
                    "products", products,
                    "count", products.size(),
                    "type", "user-contributed"
            ));
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error fetching user-contributed products: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user-contributed products"));
        }
    }

    // ===== STATISTICS =====

    /**
     * Get data statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDataStats() {
        try {
            long keywordsCount = keywordService.getKeywordCount();
            Map<String, Object> productsStats = productContributionService.getProductStats();
            long totalProducts = (long) productsStats.get("total");
            long officialProducts = (long) productsStats.get("official");
            long userContributed = (long) productsStats.get("userContributed");
            Map<String, Object> stats = Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "keywordsCount", keywordsCount,
                    "productsCount", totalProducts,
                    "productsBreakdown", Map.of(
                            "official", officialProducts,
                            "userContributed", userContributed
                    ),
                    "totalRecords", keywordsCount + totalProducts
            );

            return ResponseEntity.ok(stats);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error fetching stats: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch statistics"));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis(),
                "service", "Firestore Sync Service"
        ));
    }

    // ===== HELPER METHODS =====

    /**
     * Build product response DTO
     */
    private Map<String, Object> buildProductResponse(Product product) {
        return Map.of(
                "message", "Product added successfully",
                "productId", product.id(),
                "productName", product.productName(),
                "isOfficial", product.isOfficial(),
                "generatedKeywords", product.searchKeywords(),
                "keywordCount", product.searchKeywords().size()
        );
    }
}