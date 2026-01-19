package com.cartshare.backend.api.controller;

import com.cartshare.backend.api.v1.ContributeProductRequest;
import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.CategoryService;
import com.cartshare.backend.core.service.KeywordService;
import com.cartshare.backend.core.service.ProductContributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock
    private com.google.cloud.firestore.Firestore firestore;
    @Mock private ProductContributionService productContributionService;
    @Mock private KeywordService keywordService;
    @Mock private CategoryService categoryService;

    private SyncController syncController;

    @BeforeEach
    void setUp() {
        syncController = new SyncController(firestore, productContributionService, keywordService, categoryService);
    }

    @Test
    @DisplayName("initialSync: Should return full sync data when services succeed")
    void initialSync_Success() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(List.of(new Category("C1", "Cat 1", "Class", 1)));
        when(keywordService.getAllKeywords()).thenReturn(List.of(new Keyword("key", "C1")));
        when(productContributionService.getAllProducts()).thenReturn(List.of());

        // Act
        ResponseEntity<?> response = syncController.initialSync();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("syncStatus", "success");
        assertThat(body).containsKey("timestamp");
        assertThat((List<?>) body.get("categories")).hasSize(1);
    }

    @Test
    @DisplayName("initialSync: Should return 500 when a service fails")
    void initialSync_Failure() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenThrow(new ExecutionException(new RuntimeException("DB Down")));

        // Act
        ResponseEntity<?> response = syncController.initialSync();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("syncStatus", "failed");
    }

    @Test
    @DisplayName("contributeProduct: Should return 201 Created on success")
    void contributeProduct_Success() throws Exception {
        // Arrange
        ContributeProductRequest request = new ContributeProductRequest("Milk", "Dairy");
        Product mockProduct = Product.of("uuid", "Milk", "Dairy", false, List.of("milk", "cow"));

        when(productContributionService.contributeProduct("Milk", "Dairy")).thenReturn(mockProduct);

        // Act
        ResponseEntity<?> response = syncController.contributeProduct(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("productName", "Milk");
        assertThat(body).containsEntry("message", "Product added successfully");
    }

    @Test
    @DisplayName("contributeProduct: Should return 400 when service throws IllegalArgumentException")
    void contributeProduct_ValidationError() throws Exception {
        // Arrange
        ContributeProductRequest request = new ContributeProductRequest("", null);
        when(productContributionService.contributeProduct(any(), any()))
                .thenThrow(new IllegalArgumentException("Name required"));

        // Act
        ResponseEntity<?> response = syncController.contributeProduct(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("checkProductExists: Should return boolean status")
    void checkProductExists_ReturnsStatus() throws Exception {
        // Arrange
        when(productContributionService.productExists("Bread")).thenReturn(true);

        // Act
        ResponseEntity<?> response = syncController.checkProductExists("Bread");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("exists", true);
    }

    @Test
    @DisplayName("healthCheck: Should return healthy status")
    void healthCheck_ReturnsOk() {
        ResponseEntity<?> response = syncController.healthCheck();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().toString()).contains("healthy");
    }

    // ===== CATEGORY SUGGESTIONS TESTS =====

    @Test
    @DisplayName("getCategorySuggestions: Should return categories when name is valid")
    void getCategorySuggestions_Success() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(new Category("C1", "Test", "Class", 1)));

        ResponseEntity<?> response = syncController.getCategorySuggestions("Valid Product");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("productName", "Valid Product");
        assertThat(body).containsEntry("defaultCategory", "OUTROS");
    }

    @Test
    @DisplayName("getCategorySuggestions: Should return 400 when product name is blank")
    void getCategorySuggestions_InvalidInput() {
        ResponseEntity<?> response = syncController.getCategorySuggestions("   ");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===== PRODUCTS BY TYPE TESTS =====

    @Test
    @DisplayName("getOfficialProducts: Should return list of official products")
    void getOfficialProducts_Success() throws Exception {
        when(productContributionService.getOfficialProducts()).thenReturn(List.of());

        ResponseEntity<?> response = syncController.getOfficialProducts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("type", "official");
    }

    @Test
    @DisplayName("getUserContributedProducts: Should return list of user products")
    void getUserContributedProducts_Success() throws Exception {
        when(productContributionService.getUserContributedProducts()).thenReturn(List.of());

        ResponseEntity<?> response = syncController.getUserContributedProducts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("type", "user-contributed");
    }

    // ===== STATISTICS TESTS =====

    @Test
    @DisplayName("getDataStats: Should aggregate stats from all services")
    void getDataStats_Success() throws Exception {
        // Arrange
        when(categoryService.getCategoryCount()).thenReturn(5L);
        when(keywordService.getKeywordCount()).thenReturn(10L);
        when(productContributionService.getProductStats()).thenReturn(Map.of(
                "total", 20L,
                "official", 15L,
                "userContributed", 5L
        ));

        // Act
        ResponseEntity<?> response = syncController.getDataStats();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("totalRecords", 35L); // 5 + 10 + 20
        assertThat(body).containsKey("productsBreakdown");
    }

    @Test
    @DisplayName("getDataStats: Should return 500 when services fail")
    void getDataStats_Failure() throws Exception {
        when(categoryService.getCategoryCount()).thenThrow(new ExecutionException(new RuntimeException("Error")));

        ResponseEntity<?> response = syncController.getDataStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===== REMAINING EDGE CASES =====

    @Test
    @DisplayName("checkProductExists: Should return 400 for null name")
    void checkProductExists_NullInput() {
        ResponseEntity<?> response = syncController.checkProductExists(null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("contributeProduct: Should return 500 on database failure")
    void contributeProduct_InternalError() throws Exception {
        when(productContributionService.contributeProduct(any(), any()))
                .thenThrow(new ExecutionException(new RuntimeException("Firestore error")));

        ResponseEntity<?> response = syncController.contributeProduct(new ContributeProductRequest("Name", "Cat"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}