package com.cartshare.backend.api.controller;

import com.cartshare.backend.api.v1.ContributeProductRequest;
import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.AutocompleteService;
import com.cartshare.backend.core.service.CategoryService;
import com.cartshare.backend.core.service.KeywordService;
import com.cartshare.backend.core.service.ProductContributionService;
import com.cartshare.backend.infrastructure.excel.FirestoreExcelImporter;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final CategoryService categoryService;

    private static final String DEFAULT_CATEGORY = "OUTROS";

    // ===== DATA SYNC =====

    /**
     * Initial sync: Download all data for mobile app
     */
    @GetMapping("/initial")
    public ResponseEntity<?> initialSync() {
        try {
            log.info("📱 Initial sync requested");

            List<Category> categories = categoryService.getAllCategories();
            List<Keyword> keywords = keywordService.getAllKeywords();
            List<Product> products = productContributionService.getAllProducts();

            Map<String, Object> syncData = Map.of(
                    "categories", categories,
                    "keywords", keywords,
                    "products", products,
                    "timestamp", System.currentTimeMillis(),
                    "syncStatus", "success"
            );

            log.info("✅ Initial sync: {} categories, {} keywords, {} products",
                    categories.size(), keywords.size(), products.size());

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

    // ===== CATEGORIES =====

    /**
     * Get category suggestions for new product
     */
    @GetMapping("/categories/suggestions")
    public ResponseEntity<?> getCategorySuggestions(@RequestParam String productName) {
        try {
            if (productName == null || productName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product name is required"));
            }

            log.info("🎯 Category suggestions for: {}", productName);

            List<Category> categories = categoryService.getAllCategories();

            Map<String, Object> response = Map.of(
                    "productName", productName,
                    "categories", categories,
                    "defaultCategory", DEFAULT_CATEGORY,
                    "message", "Select a category or use default (OUTROS)"
            );

            return ResponseEntity.ok(response);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error fetching categories: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch categories"));
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
            long categoriesCount = categoryService.getCategoryCount();
            long keywordsCount = keywordService.getKeywordCount();
            Map<String, Object> productsStats = productContributionService.getProductStats();

            long totalProducts = (long) productsStats.get("total");
            long officialProducts = (long) productsStats.get("official");
            long userContributed = (long) productsStats.get("userContributed");

            Map<String, Object> stats = Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "categoriesCount", categoriesCount,
                    "keywordsCount", keywordsCount,
                    "productsCount", totalProducts,
                    "productsBreakdown", Map.of(
                            "official", officialProducts,
                            "userContributed", userContributed
                    ),
                    "totalRecords", categoriesCount + keywordsCount + totalProducts
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
                "categoryId", product.categoryId(),
                "isOfficial", product.isOfficial(),
                "generatedKeywords", product.searchKeywords(),
                "keywordCount", product.searchKeywords().size()
        );
    }
}