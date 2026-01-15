package com.cartshare.backend.api.controller;

import com.cartshare.backend.api.v1.ContributeProductRequest;
import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.AutocompleteService;
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
    private final FirestoreExcelImporter importer;
    private final AutocompleteService autocompleteService;

    private static final String DEFAULT_CATEGORY = "OUTROS";

    // ===== INITIAL DATA SYNC FOR MOBILE APPS =====

    /**
     * Endpoint for mobile apps to download all categories, keywords, and products
     * on first app launch or manual refresh.
     */
    @GetMapping("/initial")
    public ResponseEntity<?> initialSync() {
        try {
            log.info("📱 Initial sync requested");

            Map<String, Object> syncData = new HashMap<>();

            // Fetch all categories
            QuerySnapshot categoriesSnapshot = firestore.collection("categories").get().get();
            List<Category> categories = categoriesSnapshot.toObjects(Category.class);
            syncData.put("categories", categories);

            // Fetch all keywords
            QuerySnapshot keywordsSnapshot = firestore.collection("keywords").get().get();
            List<Keyword> keywords = keywordsSnapshot.toObjects(Keyword.class);
            syncData.put("keywords", keywords);

            // Fetch all products (both official and user-contributed)
            QuerySnapshot productsSnapshot = firestore.collection("products").get().get();
            List<Product> products = productsSnapshot.toObjects(Product.class);
            syncData.put("products", products);

            syncData.put("timestamp", System.currentTimeMillis());
            syncData.put("syncStatus", "success");

            log.info("✅ Initial sync data prepared: {} categories, {} keywords, {} products",
                    categories.size(), keywords.size(), products.size());

            return ResponseEntity.ok(syncData);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error preparing initial sync data: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "syncStatus", "failed",
                            "error", "Failed to prepare sync data"
                    ));
        }
    }

    // ===== USER SUBMITS NEW PRODUCT =====

    /**
     * User submits a product that doesn't exist in the system.
     * This endpoint:
     * 1. Creates the product (marked as user-contributed)
     * 2. Auto-generates keywords from product name
     * 3. Creates or assigns to category
     * 4. Updates autocomplete index
     * Request body:
     * {
     *   "productName": "iPhone 15",
     *   "categoryId": "electronics",    // Optional - null defaults to OUTROS
     * }
     */
    @PostMapping("/contribute/product")
    public ResponseEntity<?> contributeProduct(@RequestBody ContributeProductRequest request) {
        try {
            // Validate input
            if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product name is required"));
            }

            String productName = request.getProductName().trim();
            log.info("📝 New product contribution: {}", productName);

            // Check if product already exists (case-insensitive)
            boolean exists = !firestore.collection("products")
                    .whereEqualTo("productName", productName)
                    .get().get().isEmpty();

            if (exists) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Product already exists in the system"));
            }

            // Determine category
            String categoryId = request.getCategoryId();
            if (categoryId == null || categoryId.trim().isEmpty()) {
                categoryId = DEFAULT_CATEGORY;
                log.info("📦 No category specified, using default: {}", DEFAULT_CATEGORY);
            } else {
                // Verify category exists
                if (!firestore.collection("categories").document(categoryId).get().get().exists()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Category '" + categoryId + "' does not exist"));
                }
            }

            // Auto-generate search keywords from product name
            List<String> searchKeywords = importer.generateSearchKeywords(productName);
            log.info("🔑 Generated keywords: {}", searchKeywords);

            // Create product (isOfficial = false for user contributions)
            // isOfficial = true only for products from initial Excel seed
            Product product = new Product(productName, categoryId, false, searchKeywords);
            String productId = UUID.randomUUID().toString();
            firestore.collection("products").document(productId).set(product).get();
            log.info("✅ User-contributed product saved: {} (ID: {}, isOfficial: false)", productName, productId);

            // Auto-generate keywords for this product
            createAutoKeywordsForProduct(productName, categoryId, searchKeywords);

            // Update autocomplete index
            updateAutocompleteIndex();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Product added successfully",
                            "productId", productId,
                            "productName", productName,
                            "categoryId", categoryId,
                            "generatedKeywords", searchKeywords
                    ));

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error adding product: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add product"));
        }
    }

    /**
     * Helper method to auto-generate keywords from the product name.
     * These keywords help with autocomplete suggestions.
     * Example: "iPhone 15 Pro Max" → ["iPhone", "15", "Pro", "Max", "iPhone 15", etc.]
     */
    private void createAutoKeywordsForProduct(String productName, String categoryId, List<String> searchKeywords) {
        try {
            log.info("🔑 Creating keywords for product: {}", productName);

            // Convert search keywords to actual Keyword entities
            for (String keyword : searchKeywords) {
                // Check if keyword already exists
                long exists = firestore.collection("keywords")
                        .whereEqualTo("keyword", keyword)
                        .get().get().size();

                if (exists == 0) {
                    Keyword kw = new Keyword(keyword, categoryId);
                    String keywordId = UUID.randomUUID().toString();
                    firestore.collection("keywords").document(keywordId).set(kw).get();
                    log.debug("✅ Keyword created: {}", keyword);
                }
            }

            log.info("✅ Keywords created for product: {}", productName);

        } catch (InterruptedException | ExecutionException e) {
            log.error("⚠️ Error creating keywords: ", e);
            // Don't fail the product creation if keywords fail
        }
    }

    // ===== USER REQUESTS CATEGORY SUGGESTIONS =====

    /**
     * When user types a new product, they can request category suggestions
     * based on the product name and existing categories.
     * This helps guide them in choosing the right category.
     */
    @GetMapping("/categories/suggestions")
    public ResponseEntity<?> getCategorySuggestions(@RequestParam String productName) {
        try {
            if (productName == null || productName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product name is required"));
            }

            log.info("🎯 Requesting category suggestions for: {}", productName);

            // Fetch all categories
            QuerySnapshot snapshot = firestore.collection("categories").get().get();
            List<Category> categories = snapshot.toObjects(Category.class);

            // Add default category at the end
            categories.add(new Category(DEFAULT_CATEGORY, "Others", "MISC", 999));

            Map<String, Object> response = new HashMap<>();
            response.put("productName", productName);
            response.put("categories", categories);
            response.put("defaultCategory", DEFAULT_CATEGORY);
            response.put("message", "Select a category or use default (OUTROS)");

            return ResponseEntity.ok(response);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error fetching categories: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch categories"));
        }
    }

    // ===== VERIFY PRODUCT EXISTS =====

    /**
     * Mobile app can check if a product already exists before showing
     * the "add new product" form.
     */
    @GetMapping("/product/exists")
    public ResponseEntity<?> checkProductExists(@RequestParam String productName) {
        try {
            if (productName == null || productName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product name is required"));
            }

            long count = firestore.collection("products")
                    .whereEqualTo("productName", productName)
                    .get().get().size();

            boolean exists = count > 0;
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

    // ===== FILTER PRODUCTS BY TYPE =====

    /**
     * Get only official products (from initial Excel seed)
     */
    @GetMapping("/products/official")
    public ResponseEntity<?> getOfficialProducts() {
        try {
            List<Product> officialProducts = firestore.collection("products")
                    .whereEqualTo("isOfficial", true)
                    .get().get()
                    .toObjects(Product.class);

            log.info("📦 Official products fetched: {}", officialProducts.size());
            return ResponseEntity.ok(Map.of(
                    "products", officialProducts,
                    "count", officialProducts.size(),
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
            List<Product> userProducts = firestore.collection("products")
                    .whereEqualTo("isOfficial", false)
                    .get().get()
                    .toObjects(Product.class);

            log.info("👥 User-contributed products fetched: {}", userProducts.size());
            return ResponseEntity.ok(Map.of(
                    "products", userProducts,
                    "count", userProducts.size(),
                    "type", "user-contributed"
            ));

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error fetching user-contributed products: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user-contributed products"));
        }
    }

    // ===== HEALTH CHECK & STATS =====

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis(),
                "service", "Firestore Sync Service"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDataStats() {
        try {
            long categoriesCount = firestore.collection("categories").get().get().size();
            long keywordsCount = firestore.collection("keywords").get().get().size();
            long productsCount = firestore.collection("products").get().get().size();

            // Separate official (from Excel seed) vs user-contributed
            long officialProducts = firestore.collection("products")
                    .whereEqualTo("isOfficial", true)
                    .get().get().size();

            long userContributedProducts = productsCount - officialProducts;

            return ResponseEntity.ok(Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "categoriesCount", categoriesCount,
                    "keywordsCount", keywordsCount,
                    "productsCount", productsCount,
                    "productsBreakdown", Map.of(
                            "official", officialProducts,
                            "userContributed", userContributedProducts
                    ),
                    "totalRecords", categoriesCount + keywordsCount + productsCount,
                    "message", "Official products: " + officialProducts + " | User-contributed: " + userContributedProducts
            ));

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Error fetching stats: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch statistics"));
        }
    }

    // ===== UPDATE AUTOCOMPLETE INDEX =====

    /**
     * Called after any product/keyword changes to refresh the autocomplete index.
     */
    private void updateAutocompleteIndex() {
        try {
            QuerySnapshot categoriesSnapshot = firestore.collection("categories").get().get();
            List<Category> categories = categoriesSnapshot.toObjects(Category.class);

            QuerySnapshot keywordsSnapshot = firestore.collection("keywords").get().get();
            List<Keyword> keywords = keywordsSnapshot.toObjects(Keyword.class);

            QuerySnapshot productsSnapshot = firestore.collection("products").get().get();
            List<Product> products = productsSnapshot.toObjects(Product.class);

            autocompleteService.indexUpdate(categories, keywords, products);
            log.info("🔄 Autocomplete index updated");

        } catch (InterruptedException | ExecutionException e) {
            log.error("⚠️ Failed to update autocomplete index: ", e);
        }
    }
}

