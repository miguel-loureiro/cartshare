package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Product;


import com.cartshare.backend.shared.util.SearchUtils;
import com.cartshare.backend.shared.util.StringUtils;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceTest {

    @Mock private Firestore firestore;
    @Mock private CollectionReference collectionReference;
    @Mock private DocumentReference documentReference;
    @Mock private Transaction transaction;
    @Mock private DocumentSnapshot documentSnapshot;
    @Mock private ApiFuture<DocumentSnapshot> futureSnapshot;

    @InjectMocks
    private ProductService productService;

    private MockedStatic<StringUtils> mockedStringUtils;
    private MockedStatic<ProductCategoryMatcher> mockedMatcher;
    private MockedStatic<SearchUtils> mockedSearchUtils;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        mockedStringUtils = mockStatic(StringUtils.class);
        mockedMatcher = mockStatic(ProductCategoryMatcher.class);
        mockedSearchUtils = mockStatic(SearchUtils.class);

        doReturn(collectionReference).when(firestore).collection("products");
        doReturn(documentReference).when(collectionReference).document(anyString());

        // Intercept the transaction lambda
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            // 1. Get the lambda/function passed to runTransaction
            Transaction.Function<Product> function = (Transaction.Function<Product>) invocation.getArgument(0);

            // 2. CALL THE CORRECT METHOD NAME: updateCallback
            Product result = function.updateCallback(transaction);

            // 3. Return the result as a future
            return ApiFutures.immediateFuture(result);
        });
    }

    @AfterEach
    void tearDown() {
        if (mockedStringUtils != null) mockedStringUtils.close();
        if (mockedMatcher != null) mockedMatcher.close();
        if (mockedSearchUtils != null) mockedSearchUtils.close();
    }

    @Test
    @DisplayName("addOrGetProduct: Return existing product if found in Firestore")
    void addOrGetProduct_ReturnsExisting() throws Exception {
        String name = "Arroz";
        String id = "arroz";
        Product existing = Product.of(id, name, "alimentos", true, List.of("arroz"));

        mockedStringUtils.when(() -> StringUtils.toSafeId(name)).thenReturn(id);

        // The service calls transaction.get(docRef).get()
        when(transaction.get(documentReference)).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(Product.class)).thenReturn(existing);

        Product result = productService.addOrGetProduct(name, List.of());

        assertThat(result).isEqualTo(existing);
        verify(transaction, never()).set(any(), any());
    }

    @Test
    @DisplayName("addOrGetProduct: Create new product if not found")
    void addOrGetProduct_CreatesNew() throws Exception {
        String name = "Chocolate";
        String id = "chocolate";
        String resolvedCat = "DOCES";
        List<String> keywords = List.of("choc", "late");

        mockedStringUtils.when(() -> StringUtils.toSafeId(name)).thenReturn(id);
        mockedMatcher.when(() -> ProductCategoryMatcher.resolveCategory(anyString(), anyList(), anyString()))
                .thenReturn(resolvedCat);
        mockedSearchUtils.when(() -> SearchUtils.generateSearchKeywords(name)).thenReturn(keywords);

        when(transaction.get(documentReference)).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        Product result = productService.addOrGetProduct(name, List.of());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.isOfficial()).isFalse();

        // Verify that the transaction actually tried to save the new product
        verify(transaction).set(eq(documentReference), any(Product.class));
    }
}