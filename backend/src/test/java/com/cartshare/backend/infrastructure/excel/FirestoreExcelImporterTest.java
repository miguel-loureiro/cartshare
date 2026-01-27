package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirestoreExcelImporterTest {
    @Mock private Firestore firestore;
    @Mock private CollectionReference collectionReference;
    @Mock private DocumentReference documentReference;
    @Mock private WriteBatch writeBatch;
    @Mock private InputStream inputStream;
    @Mock private ApiFuture<List<WriteResult>> mockApiFuture;
    private FirestoreExcelImporter importer;

    @BeforeEach
    void setUp() {
        importer = new FirestoreExcelImporter(firestore);
    }

    @Test
    @DisplayName("Import Products: Should generate search keywords and use safe Doc ID")
    void shouldImportProductsWithKeywords() throws Exception {
        List<List<String>> mockExcelData = List.of(List.of("Arroz Agulhão"));
        try (MockedStatic<ExcelReader> mockedReader = mockStatic(ExcelReader.class)) {
            mockedReader.when(() -> ExcelReader.read(any())).thenReturn(mockExcelData);
            mockedReader.when(() -> ExcelReader.toSafeId("Arroz Agulhão")).thenReturn("arroz-agulhao");
            when(firestore.batch()).thenReturn(writeBatch);
            when(firestore.collection("products")).thenReturn(collectionReference);
            when(collectionReference.document("arroz-agulhao")).thenReturn(documentReference);
            when(writeBatch.commit()).thenReturn(mockApiFuture);
            importer.importProducts(inputStream);
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(writeBatch).set(eq(documentReference), captor.capture());
            Product saved = captor.getValue();
            assertEquals("Arroz Agulhão", saved.productName());
            assertTrue(saved.isOfficial());
            assertTrue(saved.searchKeywords().contains("arroz"));
            assertTrue(saved.searchKeywords().contains("agulhao"));
            verify(writeBatch).commit();
        }
    }

    @Test
    @DisplayName("Import Keywords: Should generate entries using keyword as ID")
    void shouldImportKeywordsCorrectly() throws Exception {
        List<Keyword> mockKeywords = List.of(new Keyword("Arroz"), new Keyword("Feijão"));
        when(firestore.batch()).thenReturn(writeBatch);
        when(firestore.collection("keywords")).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(writeBatch.commit()).thenReturn(mockApiFuture);
        importer.importKeywordsFromList(mockKeywords);
        verify(writeBatch, times(2)).set(eq(documentReference), any(Keyword.class));
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("Dry Run: Should NOT write to Firestore")
    void shouldNotWriteToFirestoreDuringDryRun() throws Exception {
        importer.setDryRun(true);
        importer.importKeywordsFromList(List.of(new Keyword("Test")));
        verify(firestore, never()).batch();
        verifyNoInteractions(writeBatch);
    }

    @Test
    @DisplayName("Generate Keywords: Should produce accented and stripped forms")
    void shouldGenerateKeywordsCorrectly() {
        // Act
        List<String> keywords = importer.generateSearchKeywords("Pão Integral");

        // Assert
        assertTrue(keywords.contains("pao"), "Should contain stripped version 'pao'");
        assertTrue(keywords.contains("pão"), "Should contain original version 'pão'");
        assertTrue(keywords.contains("integral"), "Should contain 'integral'");

        // Test with short words and filters
        List<String> shortWords = importer.generateSearchKeywords("O pão de sal");

        // Assertions based on .filter(word -> word.length() >= 3)
        assertFalse(shortWords.contains("o"), "'o' (length 1) must be filtered out");
        assertFalse(shortWords.contains("de"), "'de' (length 2) must be filtered out");

        // "sal" (length 3) satisfies >= 3, so it MUST be true
        assertTrue(shortWords.contains("sal"), "'sal' (length 3) should be kept");
        assertTrue(shortWords.contains("pão"));
        assertTrue(shortWords.contains("pao"));
    }

    @Test
    @DisplayName("Generate Keywords: Should handle complex accents and ignore special characters")
    void shouldHandleComplexAccents() {
        // Act - Corrected method name to generateSearchKeywords
        List<String> keywords = importer.generateSearchKeywords("Naïve Café!");

        // Assert
        assertTrue(keywords.contains("naive"));
        assertTrue(keywords.contains("naïve"));
        assertTrue(keywords.contains("cafe"));
        assertTrue(keywords.contains("café"));

        // Ensure the regex [^a-z0-9...] removed the '!'
        assertFalse(keywords.contains("café!"));
    }

    @Test
    @DisplayName("Generate Keywords: Should return empty list for null or blank input")
    void shouldHandleInvalidInput() {
        assertTrue(importer.generateSearchKeywords(null).isEmpty());
        assertTrue(importer.generateSearchKeywords("   ").isEmpty());
    }

    @Test
    @DisplayName("Import Products From List: Should save official products")
    void shouldImportProductsFromList() throws Exception {
        List<Product> products = List.of(Product.createOfficial("Leite", List.of("leite")));
        when(firestore.batch()).thenReturn(writeBatch);
        when(firestore.collection("products")).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(writeBatch.commit()).thenReturn(mockApiFuture);
        importer.importProductsFromList(products);
        verify(writeBatch).set(any(DocumentReference.class), any(Product.class));
        verify(writeBatch).commit();
    }

    @Test
    @DisplayName("Normalize Accents: High fidelity check for Portuguese chars")
    void shouldNormalizeAccentsCorrectly() {
        List<String> keywords = importer.generateSearchKeywords("Açúcar Café Naïve");
        assertTrue(keywords.contains("acucar"));
        assertTrue(keywords.contains("açúcar"));
        assertTrue(keywords.contains("cafe"));
        assertTrue(keywords.contains("café"));
        assertTrue(keywords.contains("naive"));
        assertTrue(keywords.contains("naïve"));
    }
}