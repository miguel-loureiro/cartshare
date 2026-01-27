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
    @Mock private AutocompleteService autocompleteService;
    @Mock private KeywordService keywordService;
    @Mock private CollectionReference productsCollection;
    @Mock private CollectionReference keywordsCollection;
    @Mock private DocumentReference documentReference;
    @Mock private Query query;
    @Mock private ApiFuture<QuerySnapshot> futureQuerySnapshot;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private ApiFuture<DocumentSnapshot> futureDocumentSnapshot;
    @Mock private DocumentSnapshot documentSnapshot;
    @Mock private ApiFuture<WriteResult> futureWrite;
    @InjectMocks private ProductContributionService contributionService;
    @BeforeEach
    void setUp() throws Exception {
        doReturn(productsCollection).when(firestore).collection("products");
        doReturn(keywordsCollection).when(firestore).collection("keywords");
        doReturn(documentReference).when(productsCollection).document(anyString());
        doReturn(query).when(productsCollection).whereEqualTo(anyString(), any());
        when(productsCollection.get()).thenReturn(futureQuerySnapshot);
        when(keywordsCollection.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
    }

    @Test
    @DisplayName("contributeProduct: Should successfully save new user product")
    void contributeProduct_Success() throws Exception {
        String productName = "Apple iPhone";
        List<String> keywords = List.of("apple", "iphone");
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);
        when(importer.generateSearchKeywords(productName)).thenReturn(keywords);
        when(documentReference.set(any(Product.class))).thenReturn(futureWrite);
        Product result = contributionService.contributeProduct(productName, null);
        assertThat(result.productName()).isEqualTo(productName);
        assertThat(result.isOfficial()).isFalse();
        verify(keywordService).createKeywordsForProduct(eq(productName), eq(keywords));
        verify(autocompleteService).indexUpdate(anyList(), anyList());
    }

    @Test
    @DisplayName("contributeProduct: Should throw exception if product already exists")
    void contributeProduct_AlreadyExists() throws Exception {
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () ->
                contributionService.contributeProduct("Existing Product", null)
        );
    }

    @Test
    @DisplayName("getProductStats: Should calculate correct stats without categories")
    void getProductStats_Success() throws Exception {
        when(productsCollection.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.size()).thenReturn(10);
        Query officialQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> officialFuture = mock(ApiFuture.class);
        QuerySnapshot officialSnapshot = mock(QuerySnapshot.class);
        when(productsCollection.whereEqualTo("isOfficial", true)).thenReturn(officialQuery);
        when(officialQuery.get()).thenReturn(officialFuture);
        when(officialFuture.get()).thenReturn(officialSnapshot);
        when(officialSnapshot.size()).thenReturn(7);
        var stats = contributionService.getProductStats();
        assertThat(stats.get("total")).isEqualTo(10L);
        assertThat(stats.get("official")).isEqualTo(7L);
        assertThat(stats.get("userContributed")).isEqualTo(3L);
    }

    @Test
    @DisplayName("contributeProduct: Should throw exception for null product name")
    void contributeProduct_NullName() {
        assertThrows(IllegalArgumentException.class, () ->
                contributionService.contributeProduct(null, null)
        );
    }

    @Test
    @DisplayName("getOfficialProducts: Should return list of official products")
    void getOfficialProducts_Success() throws Exception {
        Product p1 = Product.of("1", "Rice", true, List.of());
        when(productsCollection.whereEqualTo("isOfficial", true)).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Product.class)).thenReturn(List.of(p1));
        List<Product> results = contributionService.getOfficialProducts();
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("updateAutocompleteIndex: Should log error but not crash when Firestore fails")
    void updateAutocompleteIndex_HandlesException() throws Exception {
        String productName = "Valid Product";
        when(productsCollection.whereEqualTo("productName", productName)).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);
        when(documentReference.set(any(Product.class))).thenReturn(futureWrite);
        when(futureWrite.get()).thenReturn(mock(WriteResult.class));
        ApiFuture<QuerySnapshot> failingFuture = mock(ApiFuture.class);
        when(keywordsCollection.get()).thenReturn(failingFuture);
        when(failingFuture.get()).thenThrow(new ExecutionException(new RuntimeException("Firestore Down")));
        when(importer.generateSearchKeywords(anyString())).thenReturn(List.of("test"));
        Product result = contributionService.contributeProduct(productName, null);
        assertThat(result).isNotNull();
        verify(autocompleteService, never()).indexUpdate(any(), any());
    }

    @Test
    @DisplayName("getUserContributedProducts: Should return list of user products")
    void getUserContributedProducts_Success() throws Exception {
        Product p = Product.of("2", "Cookie", false, List.of());
        when(productsCollection.whereEqualTo("isOfficial", false)).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.toObjects(Product.class)).thenReturn(List.of(p));

        List<Product> results = contributionService.getUserContributedProducts();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).productName()).isEqualTo("Cookie");
    }

    @Test
    @DisplayName("getAllProducts: Should return every product in collection")
    void getAllProducts_Success() throws Exception {
        // GIVEN: Use real Product instances instead of mocks
        Product p1 = Product.of("1", "Rice", true, List.of("rice"));
        Product p2 = Product.of("2", "Beans", false, List.of("beans"));

        when(productsCollection.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.toObjects(Product.class)).thenReturn(List.of(p1, p2));

        // WHEN
        List<Product> results = contributionService.getAllProducts();

        // THEN
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Product::productName).containsExactly("Rice", "Beans");
    }

    @Test
    @DisplayName("contributeProduct: Should succeed when a valid category ID is provided")
    void contributeProduct_WithValidCategory_Success() throws Exception {
        String catId = "VEGETABLES";
        CollectionReference categoriesCollection = mock(CollectionReference.class);
        DocumentReference catDocRef = mock(DocumentReference.class);

        // Mock the category existence check
        when(firestore.collection("categories")).thenReturn(categoriesCollection);
        when(categoriesCollection.document(catId)).thenReturn(catDocRef);
        when(catDocRef.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        // Standard mocks for the rest of the method
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);
        when(importer.generateSearchKeywords(anyString())).thenReturn(List.of("test"));
        when(documentReference.set(any(Product.class))).thenReturn(futureWrite);

        Product result = contributionService.contributeProduct("Lettuce", catId);
        assertThat(result).isNotNull();
        verify(documentReference).set(any(Product.class));
    }

    @Test
    @DisplayName("contributeProduct: Should throw exception if provided category does not exist")
    void contributeProduct_InvalidCategory_ThrowsException() throws Exception {
        // --- GIVEN ---
        String catId = "NON_EXISTENT";
        String productName = "Some Product";

        // 1. Properly chain the category mocks
        CollectionReference categoriesCollection = mock(CollectionReference.class);
        DocumentReference catDocRef = mock(DocumentReference.class);

        // Use doReturn to match your setUp style and avoid stubbing issues
        doReturn(categoriesCollection).when(firestore).collection("categories");
        when(categoriesCollection.document(catId)).thenReturn(catDocRef);
        when(catDocRef.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false); // The core of the test

        // 2. Mock the product existence check (needed because it's called before category check)
        when(productsCollection.whereEqualTo("productName", productName)).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);

        // --- WHEN / THEN ---
        assertThrows(IllegalArgumentException.class, () ->
                contributionService.contributeProduct(productName, catId)
        );
    }

    @Test
    @DisplayName("contributeProduct: Should throw exception for blank product name")
    void contributeProduct_BlankName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                contributionService.contributeProduct("   ", null)
        );
    }
}