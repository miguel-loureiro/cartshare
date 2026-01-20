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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("getCategorySuggestions: Should return 400 when product name is blank")
    void getCategorySuggestions_InvalidInput() {
        ResponseEntity<?> response = syncController.getCategorySuggestions("   ");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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


    @Test
    @DisplayName("checkProductExists: Should return 200 and exists=true")
    void checkProductExists_DirectCall() throws Exception {
        // GIVEN
        when(productContributionService.productExists("Apple")).thenReturn(true);

        // WHEN
        ResponseEntity<?> response = syncController.checkProductExists("Apple");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("exists")).isEqualTo(true);
    }

    @Test
    @DisplayName("getCategorySuggestions: Should return bad request for empty input")
    void getCategorySuggestions_EmptyInput() {
        // WHEN
        ResponseEntity<?> response = syncController.getCategorySuggestions("  ");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("getOfficialProducts: Should handle service exceptions (500 error)")
    void getOfficialProducts_HandleException() throws Exception {
        // GIVEN
        when(productContributionService.getOfficialProducts())
                .thenThrow(new ExecutionException("DB Down", new RuntimeException()));

        // WHEN
        ResponseEntity<?> response = syncController.getOfficialProducts();

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("getUserContributedProducts: Should return list of products")
    void getUserContributedProducts_Success() throws Exception {
        // GIVEN
        List<Product> mockProducts = List.of(new Product("id", "Bread", "CAT", false, List.of()));
        when(productContributionService.getUserContributedProducts()).thenReturn(mockProducts);

        // WHEN
        ResponseEntity<?> response = syncController.getUserContributedProducts();

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("count")).isEqualTo(1);
        assertThat(body.get("type")).isEqualTo("user-contributed");
    }

    @Test
    @DisplayName("getCategorySuggestions: Should return 200 and list of categories")
    void getCategorySuggestions_Success() throws Exception {
        // Arrange
        List<Category> mockCats = List.of(new Category("CAT1", "Test Cat", "Class", 1));
        when(categoryService.getAllCategories()).thenReturn(mockCats);

        // Act
        ResponseEntity<?> response = syncController.getCategorySuggestions("Banana");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("productName", "Banana");
        assertThat(body).containsEntry("defaultCategory", "OUTROS");
        assertThat((List<?>) body.get("categories")).hasSize(1);
    }

    @Test
    @DisplayName("getCategorySuggestions: Should return 500 when service fails")
    void getCategorySuggestions_InternalError() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenThrow(new InterruptedException("Timeout"));

        // Act
        ResponseEntity<?> response = syncController.getCategorySuggestions("Banana");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("getOfficialProducts: Should return 200 and official products list")
    void getOfficialProducts_Success() throws Exception {
        // Arrange
        List<Product> officialList = List.of(Product.of("1", "Milk", "C1", true, List.of()));
        when(productContributionService.getOfficialProducts()).thenReturn(officialList);

        // Act
        ResponseEntity<?> response = syncController.getOfficialProducts();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("type", "official");
        assertThat(body).containsEntry("count", 1);
    }

    @Test
    @DisplayName("getUserContributedProducts: Should return 500 when service fails")
    void getUserContributedProducts_InternalError() throws Exception {
        // Arrange
        when(productContributionService.getUserContributedProducts())
                .thenThrow(new ExecutionException(new RuntimeException("DB Error")));

        // Act
        ResponseEntity<?> response = syncController.getUserContributedProducts();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("checkProductExists: Should return 500 when productExists service fails")
    void checkProductExists_InternalError_Coverage() throws Exception {
        // Arrange
        when(productContributionService.productExists(anyString()))
                .thenThrow(new InterruptedException("Interrupted"));

        // Act
        ResponseEntity<?> response = syncController.checkProductExists("TestProduct");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Failed to check product");
    }
}