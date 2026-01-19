package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.infrastructure.excel.FirestoreExcelImporter;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductContributionServiceTest {

    @Mock private Firestore firestore;
    @Mock private FirestoreExcelImporter importer;
    @Mock
    private AutocompleteService autocompleteService;
    @Mock private KeywordService keywordService;

    // Firestore structure mocks
    @Mock private CollectionReference productsCollection;
    @Mock private CollectionReference categoriesCollection;
    @Mock private CollectionReference keywordsCollection;
    @Mock private DocumentReference documentReference;
    @Mock private Query query;

    // Result mocks
    @Mock private ApiFuture<QuerySnapshot> futureQuerySnapshot;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private ApiFuture<DocumentSnapshot> futureDocumentSnapshot;
    @Mock private DocumentSnapshot documentSnapshot;
    @Mock private ApiFuture<WriteResult> futureWrite;

    @InjectMocks
    private ProductContributionService contributionService;

    @BeforeEach
    void setUp() throws Exception {
        // Base collections
        doReturn(productsCollection).when(firestore).collection("products");
        doReturn(categoriesCollection).when(firestore).collection("categories");
        doReturn(keywordsCollection).when(firestore).collection("keywords");

        // Document/Query chaining
        doReturn(documentReference).when(productsCollection).document(anyString());
        doReturn(documentReference).when(categoriesCollection).document(anyString());
        doReturn(query).when(productsCollection).whereEqualTo(anyString(), any());

        // Default: Mock snapshots for the "updateAutocompleteIndex" which runs at the end of many methods
        when(productsCollection.get()).thenReturn(futureQuerySnapshot);
        when(categoriesCollection.get()).thenReturn(futureQuerySnapshot);
        when(keywordsCollection.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
    }

    @Test
    @DisplayName("contributeProduct: Should successfully save new user product")
    void contributeProduct_Success() throws Exception {
        // Arrange
        String productName = "Apple iPhone";
        String categoryId = "electronics";
        List<String> keywords = List.of("apple", "iphone");

        // 1. Mock productExists check (Returns empty result = doesn't exist)
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);

        // 2. Mock category resolution check
        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        // 3. Mock importer and saving
        when(importer.generateSearchKeywords(productName)).thenReturn(keywords);
        when(documentReference.set(any(Product.class))).thenReturn(futureWrite);

        // Act
        Product result = contributionService.contributeProduct(productName, categoryId);

        // Assert
        assertThat(result.productName()).isEqualTo(productName);
        assertThat(result.isOfficial()).isFalse();
        verify(keywordService).createKeywordsForProduct(eq(productName), eq(categoryId), eq(keywords));
        verify(autocompleteService).indexUpdate(anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("contributeProduct: Should throw exception if product already exists")
    void contributeProduct_AlreadyExists() throws Exception {
        // Arrange
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                contributionService.contributeProduct("Existing Product", "some-cat")
        );
    }

    @Test
    @DisplayName("getProductStats: Should calculate correct stats")
    void getProductStats_Success() throws Exception {
        // Arrange
        // Total products mock
        when(productsCollection.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.size()).thenReturn(10);

        // Official products query mock
        // We need a specific mock for this query to differentiate from the total count
        Query officialQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> officialFuture = mock(ApiFuture.class);
        QuerySnapshot officialSnapshot = mock(QuerySnapshot.class);

        when(productsCollection.whereEqualTo("isOfficial", true)).thenReturn(officialQuery);
        when(officialQuery.get()).thenReturn(officialFuture);
        when(officialFuture.get()).thenReturn(officialSnapshot);
        when(officialSnapshot.size()).thenReturn(7);

        // Act
        var stats = contributionService.getProductStats();

        // Assert
        assertThat(stats.get("total")).isEqualTo(10L);
        assertThat(stats.get("official")).isEqualTo(7L);
        assertThat(stats.get("userContributed")).isEqualTo(3L);
    }

    @Test
    @DisplayName("contributeProduct: Should use default category when none provided")
    void contributeProduct_DefaultCategory() throws Exception {
        // Arrange
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true); // Product doesn't exist
        when(importer.generateSearchKeywords(anyString())).thenReturn(List.of("test"));
        when(documentReference.set(any(Product.class))).thenReturn(futureWrite);

        // Act
        Product result = contributionService.contributeProduct("Test Product", null);

        // Assert
        assertThat(result.categoryId()).isEqualTo("OUTROS");
        verify(documentReference, never()).get(); // Should not check existence of "OUTROS"
    }

    // ===== VALIDATION & CATEGORY TESTS =====

    @Test
    @DisplayName("contributeProduct: Should throw exception for null product name")
    void contributeProduct_NullName() {
        assertThrows(IllegalArgumentException.class, () ->
                contributionService.contributeProduct(null, "category")
        );
    }

    @Test
    @DisplayName("contributeProduct: Should throw exception for non-existent category")
    void contributeProduct_CategoryNotFound() throws Exception {
        // Arrange
        String productName = "Unique Product";
        String categoryId = "fake-cat";

        // Product doesn't exist
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);

        // Category DOES NOT exist
        when(categoriesCollection.document(categoryId)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                contributionService.contributeProduct(productName, categoryId)
        );
        assertThat(ex.getMessage()).contains("does not exist");
    }

    // ===== DATA RETRIEVAL TESTS (Increases coverage on loops/conversions) =====

    @Test
    @DisplayName("getOfficialProducts: Should return list of official products")
    void getOfficialProducts_Success() throws Exception {
        // Arrange
        Product p1 = Product.of("1", "Rice", "cat1", true, List.of());
        when(productsCollection.whereEqualTo("isOfficial", true)).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Product.class)).thenReturn(List.of(p1));

        // Act
        List<Product> results = contributionService.getOfficialProducts();

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isOfficial()).isTrue();
    }

    @Test
    @DisplayName("getAllProducts: Should return all products in collection")
    void getAllProducts_Success() throws Exception {
        // Arrange
        when(productsCollection.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Product.class)).thenReturn(List.of(
                Product.of("1", "P1", "C1", true, List.of()),
                Product.of("2", "P2", "C1", false, List.of())
        ));

        // Act
        List<Product> results = contributionService.getAllProducts();

        // Assert
        assertThat(results).hasSize(2);
    }

    // ===== EDGE CASES & ERROR HANDLING =====

    @Test
    @DisplayName("getProductStats: Should handle zero products correctly")
    void getProductStats_Empty() throws Exception {
        // Arrange
        // 1. Mock for: firestore.collection("products").get()
        when(productsCollection.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(0);

        // 2. Mock for: firestore.collection("products").whereEqualTo("isOfficial", true).get()
        Query officialQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> officialFuture = mock(ApiFuture.class);
        QuerySnapshot officialSnapshot = mock(QuerySnapshot.class);

        when(productsCollection.whereEqualTo("isOfficial", true)).thenReturn(officialQuery);
        when(officialQuery.get()).thenReturn(officialFuture);
        when(officialFuture.get()).thenReturn(officialSnapshot);
        when(officialSnapshot.size()).thenReturn(0);

        // Act
        var stats = contributionService.getProductStats();

        // Assert
        assertThat(stats.get("total")).isEqualTo(0L);
        assertThat(stats.get("official")).isEqualTo(0L);
        assertThat(stats.get("userContributed")).isEqualTo(0L);
    }

    @Test
    @DisplayName("getUserContributedProducts: Should return filtered list")
    void getUserContributedProducts_Success() throws Exception {
        when(productsCollection.whereEqualTo("isOfficial", false)).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Product.class)).thenReturn(List.of(
                Product.of("id1", "User Product", "cat", false, List.of())
        ));

        List<Product> results = contributionService.getUserContributedProducts();
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("updateAutocompleteIndex: Should log error but not crash when Firestore fails")
    void updateAutocompleteIndex_HandlesException() throws Exception {
        // --- Arrange ---
        String productName = "Valid Product";
        String categoryId = "OUTROS";

        // 1. Mock the "products" collection for the 'productExists' and 'saveProduct' calls
        CollectionReference productsCollection = mock(CollectionReference.class);
        when(firestore.collection("products")).thenReturn(productsCollection);

        // Mock productExists (MUST return empty/false)
        Query query = mock(Query.class);
        ApiFuture<QuerySnapshot> successFuture = mock(ApiFuture.class);
        QuerySnapshot successSnapshot = mock(QuerySnapshot.class);

        when(productsCollection.whereEqualTo("productName", productName)).thenReturn(query);
        when(query.get()).thenReturn(successFuture);
        when(successFuture.get()).thenReturn(successSnapshot);
        when(successSnapshot.isEmpty()).thenReturn(true);

        // Mock saveProduct
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> futureWrite = mock(ApiFuture.class);
        when(productsCollection.document(anyString())).thenReturn(docRef);
        when(docRef.set(any(Product.class))).thenReturn(futureWrite);
        when(futureWrite.get()).thenReturn(mock(WriteResult.class));

        // 2. Mock the "categories" collection for the 'updateAutocompleteIndex' phase
        // This is where we force the failure
        CollectionReference categoriesCollection = mock(CollectionReference.class);
        when(firestore.collection("categories")).thenReturn(categoriesCollection);

        ApiFuture<QuerySnapshot> failingFuture = mock(ApiFuture.class);
        when(categoriesCollection.get()).thenReturn(failingFuture);

        // Simulate Firestore ExecutionException
        when(failingFuture.get()).thenThrow(new ExecutionException(new RuntimeException("Firestore Down")));

        // 3. Mock keywords generation
        when(importer.generateSearchKeywords(anyString())).thenReturn(List.of("test"));

        // --- Act ---
        // We expect this NOT to throw an exception because the service catches it internally
        Product result = contributionService.contributeProduct(productName, null);

        // --- Assert ---
        assertThat(result).isNotNull();
        assertThat(result.productName()).isEqualTo(productName);

        // Verify that indexUpdate was never called because the exception happened during data fetch
        verify(autocompleteService, never()).indexUpdate(any(), any(), any());

        // Ensure the product was actually saved despite the later failure
        verify(docRef).set(any(Product.class));
    }
}