package com.cartshare.backend.api.controller;

import com.cartshare.backend.api.v1.ContributeProductRequest;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock private com.google.cloud.firestore.Firestore firestore;
    @Mock private ProductContributionService productContributionService;
    @Mock private KeywordService keywordService;

    private SyncController syncController;

    @BeforeEach
    void setUp() {
        syncController = new SyncController(firestore, productContributionService, keywordService);
    }

    // ===== DATA SYNC TESTS =====

    @Test
    @DisplayName("initialSync: Should return products and keywords for mobile-side grouping")
    void initialSync_Success() throws Exception {
        // Arrange - Keywords no longer need a category ID
        when(keywordService.getAllKeywords()).thenReturn(List.of(new Keyword("pão")));
        when(productContributionService.getAllProducts()).thenReturn(List.of());

        // Act
        ResponseEntity<?> response = syncController.initialSync();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("syncStatus", "success");
        assertThat(body).containsKey("keywords");
        assertThat(body).containsKey("products");
        assertThat(body).doesNotContainKey("categories");
    }

    // ===== PRODUCT CONTRIBUTION TESTS =====

    @Test
    @DisplayName("contributeProduct: Should return 201 and valid response DTO")
    void contributeProduct_Success() throws Exception {
        // Arrange - Product.of no longer takes a categoryId
        ContributeProductRequest request = new ContributeProductRequest("Milk", "Store_Label_To_Be_Ignored");
        Product mockProduct = Product.of("uuid", "Milk", false, List.of("milk"));

        when(productContributionService.contributeProduct(anyString(), any())).thenReturn(mockProduct);

        // Act
        ResponseEntity<?> response = syncController.contributeProduct(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("productName", "Milk");
        assertThat(body).containsEntry("message", "Product added successfully");
    }

    // ===== STATISTICS TESTS =====

    @Test
    @DisplayName("getDataStats: Should aggregate totals for keywords and products")
    void getDataStats_Success() throws Exception {
        // Arrange
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
        // Total = 10 (keywords) + 20 (products) = 30
        assertThat(body).containsEntry("totalRecords", 30L);
        assertThat(body).doesNotContainKey("categoriesCount");
    }

    // ===== PRODUCT TYPE FILTERS =====

    @Test
    @DisplayName("getOfficialProducts: Should return list without category metadata")
    void getOfficialProducts_Success() throws Exception {
        // Arrange
        List<Product> officialList = List.of(Product.of("1", "Milk", true, List.of()));
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
    @DisplayName("getUserContributedProducts: Should return list successfully")
    void getUserContributedProducts_Success() throws Exception {
        // Arrange
        List<Product> mockProducts = List.of(new Product("id", "Bread", false, List.of()));
        when(productContributionService.getUserContributedProducts()).thenReturn(mockProducts);

        // Act
        ResponseEntity<?> response = syncController.getUserContributedProducts();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("type", "user-contributed");
    }

    // ===== HEALTH & ERROR HANDLING =====

    @Test
    @DisplayName("healthCheck: Simple pulse check")
    void healthCheck_ReturnsOk() {
        ResponseEntity<?> response = syncController.healthCheck();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("checkProductExists: Validation check")
    void checkProductExists_Success() throws Exception {
        when(productContributionService.productExists("Apple")).thenReturn(true);
        ResponseEntity<?> response = syncController.checkProductExists("Apple");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("initialSync: Should return 500 when service fails")
    void initialSync_Failure() throws Exception {
        when(keywordService.getAllKeywords()).thenThrow(new ExecutionException(new RuntimeException("DB Down")));

        ResponseEntity<?> response = syncController.initialSync();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("syncStatus", "failed");
    }

    @Test
    @DisplayName("contributeProduct: Should return 500 on execution error")
    void contributeProduct_InternalError() throws Exception {
        when(productContributionService.contributeProduct(anyString(), any()))
                .thenThrow(new InterruptedException("Timeout"));

        ResponseEntity<?> response = syncController.contributeProduct(new ContributeProductRequest("Milk", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("getDataStats: Should return 500 when stats calculation fails")
    void getDataStats_Failure() throws Exception {
        when(keywordService.getKeywordCount()).thenThrow(new ExecutionException(new RuntimeException("Error")));

        ResponseEntity<?> response = syncController.getDataStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("contributeProduct: Should return 400 when service throws IllegalArgumentException")
    void contributeProduct_BadRequest() throws Exception {
        // Simulate service validation failing
        when(productContributionService.contributeProduct(anyString(), any()))
                .thenThrow(new IllegalArgumentException("Invalid Name"));

        ResponseEntity<?> response = syncController.contributeProduct(new ContributeProductRequest("", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("error");
    }

    @Test
    @DisplayName("checkProductExists: Should return 400 for null or empty name")
    void checkProductExists_InvalidInput() throws Exception {
        // Test Null
        ResponseEntity<?> responseNull = syncController.checkProductExists(null);
        assertThat(responseNull.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Test Empty String
        ResponseEntity<?> responseEmpty = syncController.checkProductExists("   ");
        assertThat(responseEmpty.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("getOfficialProducts: Should return 500 when Firestore fails")
    void getOfficialProducts_Failure() throws Exception {
        when(productContributionService.getOfficialProducts()).thenThrow(new ExecutionException(new RuntimeException()));
        ResponseEntity<?> response = syncController.getOfficialProducts();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("getUserContributedProducts: Should return 500 when Firestore fails")
    void getUserContributedProducts_Failure() throws Exception {
        when(productContributionService.getUserContributedProducts()).thenThrow(new ExecutionException(new RuntimeException()));
        ResponseEntity<?> response = syncController.getUserContributedProducts();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}