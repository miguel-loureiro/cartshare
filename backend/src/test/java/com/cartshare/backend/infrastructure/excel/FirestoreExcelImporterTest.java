package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.ProductCategoryMatcher;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirestoreExcelImporterTest {
    @Mock
    private Firestore firestore;
    @Mock
    private CollectionReference collectionReference;
    @Mock
    private DocumentReference documentReference;
    @Mock
    private WriteBatch writeBatch;
    @Mock
    private InputStream inputStream;
    @Mock
    private ApiFuture<List<WriteResult>> mockApiFuture;

    private FirestoreExcelImporter importer;

    @BeforeEach
    void setUp() {
        importer = new FirestoreExcelImporter(firestore);
    }

    @Test
    @DisplayName("Import Categories: Should sanitize IDs and save to Firestore")
    void shouldImportCategoriesCorrectly() throws Exception {
        // Arrange
        List<List<String>> mockExcelData = List.of(
                List.of("Alimentação", "Alimentos", "Mercearia", "1")
        );

        try (MockedStatic<ExcelReader> mockedReader = mockStatic(ExcelReader.class)) {
            mockedReader.when(() -> ExcelReader.read(any())).thenReturn(mockExcelData);
            mockedReader.when(() -> ExcelReader.toSafeId("Alimentação")).thenReturn("alimentacao");
            mockedReader.when(() -> ExcelReader.toSafeId("Mercearia")).thenReturn("mercearia");

            when(firestore.batch()).thenReturn(writeBatch);
            when(firestore.collection("categories")).thenReturn(collectionReference);
            when(collectionReference.document("alimentacao")).thenReturn(documentReference);
            when(writeBatch.commit()).thenReturn(mockApiFuture);

            // Act
            importer.importCategories(inputStream);

            // Assert
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(writeBatch).set(any(DocumentReference.class), captor.capture());

            Category saved = captor.getValue();
            assertEquals("alimentacao", saved.id());
            assertEquals("Alimentos", saved.name());
            assertEquals("mercearia", saved.classification());
            assertEquals(1, saved.priority());

            verify(writeBatch).commit();
        }
    }

    @Test
    @DisplayName("Import Products: Should generate search keywords and use safe Doc ID")
    @SuppressWarnings("unchecked")
    void shouldImportProductsWithKeywords() throws Exception {
        // Arrange
        List<List<String>> mockExcelData = List.of(
                List.of("Arroz Agulhão", "ALIMENTOS")
        );

        try (MockedStatic<ExcelReader> mockedReader = mockStatic(ExcelReader.class);
             MockedStatic<ProductCategoryMatcher> mockedMatcher = mockStatic(ProductCategoryMatcher.class)) {

            mockedReader.when(() -> ExcelReader.read(any())).thenReturn(mockExcelData);
            mockedReader.when(() -> ExcelReader.toSafeId("Arroz Agulhão")).thenReturn("arroz_agulhao");

            mockedMatcher.when(() -> ProductCategoryMatcher.resolveCategory(
                    eq("Arroz Agulhão"),
                    anyList(),
                    eq("ALIMENTOS")
            )).thenReturn("alimentos");

            when(firestore.batch()).thenReturn(writeBatch);
            when(firestore.collection("products")).thenReturn(collectionReference);
            when(collectionReference.document("arroz_agulhao")).thenReturn(documentReference);
            when(writeBatch.commit()).thenReturn(mockApiFuture);

            // Act
            importer.importProducts(inputStream, List.of());

            // Assert
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(writeBatch).set(any(DocumentReference.class), captor.capture());

            Product saved = captor.getValue();
            assertEquals("Arroz Agulhão", saved.productName());
            assertEquals("alimentos", saved.categoryId());
            assertTrue(saved.isOfficial());

            List<String> keywords = saved.searchKeywords();
            assertTrue(keywords.contains("arroz"));
            assertTrue(keywords.contains("agulhao"));

            verify(writeBatch).commit();
        }
    }

    @Test
    @DisplayName("Dry Run: Should NOT call Firestore when dryRun is true")
    void shouldNotWriteToFirestoreDuringDryRun() throws Exception {
        // Arrange
        importer.setDryRun(true);
        List<List<String>> mockExcelData = List.of(
                List.of("LIMPEZA", "Limpeza", "CASA", "2")
        );

        try (MockedStatic<ExcelReader> mockedReader = mockStatic(ExcelReader.class)) {
            mockedReader.when(() -> ExcelReader.read(any())).thenReturn(mockExcelData);
            mockedReader.when(() -> ExcelReader.toSafeId("LIMPEZA")).thenReturn("limpeza");
            mockedReader.when(() -> ExcelReader.toSafeId("CASA")).thenReturn("casa");

            // Act
            importer.importCategories(inputStream);

            // Assert - firestore.batch() should NOT be called in dry run mode
            verify(firestore, never()).batch();
            verifyNoInteractions(writeBatch);
        }
    }

    @Test
    @DisplayName("Import Keywords: Should generate keyword entries")
    void shouldImportKeywordsCorrectly() throws Exception {
        // Arrange
        List<Keyword> mockKeywords = List.of(
                new Keyword("Arroz", "alimentos"),
                new Keyword("Feijão", "alimentos"),
                new Keyword("Macarrão", "alimentos")
        );

        when(firestore.batch()).thenReturn(writeBatch);
        when(firestore.collection("keywords")).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(writeBatch.commit()).thenReturn(mockApiFuture);

        // Act
        importer.importKeywordsFromList(mockKeywords);

        // Assert
        verify(writeBatch, times(3)).set(eq(documentReference), any(Keyword.class));
        verify(writeBatch, times(1)).commit();
    }

    @Test
    @DisplayName("Generate Search Keywords: Should split and clean product names")
    void shouldGenerateSearchKeywordsCorrectly() {
        // Act
        List<String> keywords = importer.generateSearchKeywords("iPhone 15 Pro Max");

        // Assert
        assertTrue(keywords.contains("iphone"));
        assertTrue(keywords.contains("pro"));
        assertTrue(keywords.contains("max"));
        // "15" is removed because it's only numbers (regex removes all non-alphanumeric)
        assertEquals(3, keywords.size());
    }

    @Test
    @DisplayName("Generate Search Keywords: Should handle accents and special chars")
    void shouldGenerateSearchKeywordsWithAccents() {
        // Act
        List<String> keywords = importer.generateSearchKeywords("Açúcar & Sal - Qualidade Premium");

        // Assert
        // Both forms should be present for better search coverage
        assertTrue(keywords.contains("acucar")); // Normalized form
        assertTrue(keywords.contains("açúcar")); // Original with accents

        assertTrue(keywords.contains("sal"));
        assertTrue(keywords.contains("qualidade"));
        assertTrue(keywords.contains("premium"));
    }

    @Test
    @DisplayName("Generate Search Keywords: Should filter out short words")
    void shouldFilterShortKeywords() {
        // Act
        List<String> keywords = importer.generateSearchKeywords("O pão de trigo integral");

        // Assert
        // Words must be 3+ characters, so "O" (1 char) and "de" (2 chars) are filtered
        assertFalse(keywords.contains("o")); // Too short (1 char)
        assertFalse(keywords.contains("de")); // Too short (2 chars)
        assertTrue(keywords.contains("pao")); // "pão" → "pao" (3 chars, ã becomes a)
        assertTrue(keywords.contains("trigo")); // 5 chars
        assertTrue(keywords.contains("integral")); // 8 chars
    }

    @Test
    @DisplayName("Generate Search Keywords: Should return empty list for blank input")
    void shouldHandleBlankInput() {
        // Act & Assert
        assertTrue(importer.generateSearchKeywords("").isEmpty());
        assertTrue(importer.generateSearchKeywords(null).isEmpty());
        assertTrue(importer.generateSearchKeywords("   ").isEmpty());
    }

    @Test
    @DisplayName("Import Categories From List: Should save list directly")
    void shouldImportCategoriesFromList() throws Exception {
        // Arrange
        List<Category> categories = List.of(
                new Category("alimentos", "Alimentos", "MERCEARIA", 1),
                new Category("limpeza", "Limpeza", "CASA", 2)
        );

        when(firestore.batch()).thenReturn(writeBatch);
        when(firestore.collection("categories")).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(writeBatch.commit()).thenReturn(mockApiFuture);

        // Act
        importer.importCategoriesFromList(categories);

        // Assert
        verify(writeBatch, times(2)).set(any(DocumentReference.class), any(Category.class));
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("Import Products From List: Should save products with batching")
    void shouldImportProductsFromList() throws Exception {
        // Arrange
        List<Product> products = List.of(
                new Product(null,"Arroz", "alimentos", true, List.of("arroz")),
                new Product(null,"Feijão", "alimentos", true, List.of("feijao"))
        );

        when(firestore.batch()).thenReturn(writeBatch);
        when(firestore.collection("products")).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(writeBatch.commit()).thenReturn(mockApiFuture);

        // Act
        importer.importProductsFromList(products);

        // Assert
        verify(writeBatch, times(2)).set(any(DocumentReference.class), any(Product.class));
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("Dry Run: Categories From List should not write")
    void shouldNotWriteCategoriesListDuringDryRun() throws Exception {
        // Arrange
        importer.setDryRun(true);
        List<Category> categories = List.of(
                new Category("alimentos", "Alimentos", "MERCEARIA", 1)
        );

        // Act
        importer.importCategoriesFromList(categories);

        // Assert
        verify(firestore, never()).batch();
        verifyNoInteractions(writeBatch);
    }

    @Test
    @DisplayName("Dry Run: Products From List should not write")
    void shouldNotWriteProductsListDuringDryRun() throws Exception {
        // Arrange
        importer.setDryRun(true);
        List<Product> products = List.of(
                new Product(null,"Arroz", "alimentos", true, List.of("arroz"))
        );

        // Act
        importer.importProductsFromList(products);

        // Assert
        verify(firestore, never()).batch();
        verifyNoInteractions(writeBatch);
    }

    @Test
    @DisplayName("Normalize Accents: Should remove diacritical marks")
    void shouldNormalizeAccentsCorrectly() {
        // Act
        List<String> keywords = importer.generateSearchKeywords("Açúcar Café Pão Naïve");

        // Assert - Both accented AND normalized forms should be present
        assertTrue(keywords.contains("acucar")); // Normalized form
        assertTrue(keywords.contains("açúcar")); // Original form

        assertTrue(keywords.contains("cafe"));   // Normalized form
        assertTrue(keywords.contains("café"));   // Original form

        assertTrue(keywords.contains("pao"));    // Normalized form
        assertTrue(keywords.contains("pão"));    // Original form

        assertTrue(keywords.contains("naive"));  // Normalized form
        assertTrue(keywords.contains("naïve"));  // Original form (if 3+ chars)
    }
}