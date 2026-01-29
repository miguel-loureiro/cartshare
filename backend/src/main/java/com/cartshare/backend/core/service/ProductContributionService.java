package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.infrastructure.excel.FirestoreExcelImporter;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductContributionService {

    private final Firestore firestore;
    private final FirestoreExcelImporter importer;
    private final AutocompleteService autocompleteService;
    private final KeywordService keywordService;

    private static final String DEFAULT_CATEGORY = "OUTROS";

    /**
     * Contribute a new product to the system.
     * Process:
     * 1. Validate product name
     * 2. Check if product exists
     * 3. Resolve and validate category
     * 4. Generate search keywords
     * 5. Create and save product
     * 6. Create keywords for autocomplete
     * 7. Update autocomplete index
     *
     * @param productName Name of the product
     * @return Product with generated ID
     * @throws IllegalArgumentException if validation fails
     * @throws ExecutionException if Firestore operation fails
     * @throws InterruptedException if thread is interrupted
     */
    public Product contributeProduct(String productName)
            throws ExecutionException, InterruptedException {

        // 1. Validate
        productName = validateProductName(productName);
        log.info("📝 New product contribution: {}", productName);

        // 2. Check if exists
        if (productExists(productName)) {
            throw new IllegalArgumentException("Product already exists in the system");
        }

        // 3. Generate keywords
        List<String> searchKeywords = importer.generateSearchKeywords(productName);
        log.info("🔑 Generated keywords: {}", searchKeywords);

        // 4. Create and save
        Product product = Product.createUserContributed(productName, searchKeywords);
        Product savedProduct = saveProduct(product);
        log.info("✅ Product saved: {} (ID: {}, isOfficial: false)", productName, savedProduct.id());

        // 5. Create keywords
        keywordService.createKeywordsForProduct(productName, searchKeywords);

        // 6. Update autocomplete
        updateAutocompleteIndex();

        return savedProduct;
    }

    /**
     * Check if a product with the given name exists
     */
    public boolean productExists(String productName) throws ExecutionException, InterruptedException {
        return !firestore.collection("products")
                .whereEqualTo("productName", productName)
                .get()
                .get()
                .isEmpty();
    }

    /**
     * Get all official products (from Excel seed)
     */
    public List<Product> getOfficialProducts() throws ExecutionException, InterruptedException {
        List<Product> products = firestore.collection("products")
                .whereEqualTo("isOfficial", true)
                .get()
                .get()
                .toObjects(Product.class);

        log.info("📦 Official products fetched: {}", products.size());
        return products;
    }

    /**
     * Get all user-contributed products
     */
    public List<Product> getUserContributedProducts() throws ExecutionException, InterruptedException {
        List<Product> products = firestore.collection("products")
                .whereEqualTo("isOfficial", false)
                .get()
                .get()
                .toObjects(Product.class);

        log.info("👥 User-contributed products fetched: {}", products.size());
        return products;
    }

    /**
     * Get all products (both official and user-contributed)
     */
    public List<Product> getAllProducts() throws ExecutionException, InterruptedException {
        List<Product> products = firestore.collection("products")
                .get()
                .get()
                .toObjects(Product.class);

        log.info("📦 All products fetched: {}", products.size());
        return products;
    }

    /**
     * Get product statistics
     */
    public Map<String, Object> getProductStats() throws ExecutionException, InterruptedException {
        long totalProducts = firestore.collection("products").get().get().size();
        long officialProducts = firestore.collection("products")
                .whereEqualTo("isOfficial", true)
                .get()
                .get()
                .size();
        long userContributedProducts = totalProducts - officialProducts;

        return Map.of(
                "total", totalProducts,
                "official", officialProducts,
                "userContributed", userContributedProducts
        );
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Validate product name
     */
    private String validateProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        return productName.trim();
    }

    /**
     * Save product with generated UUID
     */
    private Product saveProduct(Product product) throws ExecutionException, InterruptedException {
        String productId = UUID.randomUUID().toString();

        Product productWithId = Product.of(
                productId,
                product.productName(),
                product.isOfficial(),
                product.searchKeywords()
        );

        firestore.collection("products")
                .document(productId)
                .set(productWithId)
                .get();

        return productWithId;
    }

    /**
     * Update autocomplete index
     */
    private void updateAutocompleteIndex() {
        try {
            QuerySnapshot keywordsSnapshot = firestore.collection("keywords").get().get();
            List<Keyword> keywords = keywordsSnapshot.toObjects(Keyword.class);

            QuerySnapshot productsSnapshot = firestore.collection("products").get().get();
            List<Product> products = productsSnapshot.toObjects(Product.class);

            autocompleteService.indexUpdate(keywords, products);
            log.info("🔄 Autocomplete index updated");

        } catch (InterruptedException | ExecutionException e) {
            log.error("⚠️ Failed to update autocomplete index: ", e);
        }
    }
}
