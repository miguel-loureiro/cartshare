package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Category;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
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

    @Mock private Firestore firestore;
    @Mock
    private CollectionReference collectionReference;
    @Mock private DocumentReference documentReference;
    @Mock private InputStream inputStream;

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
            mockedReader.when(() -> ExcelReader.toSafeId("Alimentação")).thenReturn("ALIMENTAÇÃO");
            mockedReader.when(() -> ExcelReader.toSafeId("Mercearia")).thenReturn("MERCEARIA");

            when(firestore.collection("categories")).thenReturn(collectionReference);
            when(collectionReference.document("ALIMENTAÇÃO")).thenReturn(documentReference);

            // Act
            importer.importCategories(inputStream);

            // Assert
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(documentReference).set(captor.capture());

            Category saved = captor.getValue();
            assertEquals("ALIMENTAÇÃO", saved.id());
            assertEquals("MERCEARIA", saved.classification());
            assertEquals(1, saved.priority());
        }
    }

    @Test
    @DisplayName("Import Products: Should generate search keywords and replace spaces in Doc ID")
    @SuppressWarnings("unchecked")
    void shouldImportProductsWithKeywords() throws Exception {
        // Arrange
        List<List<String>> mockExcelData = List.of(
                List.of("Arroz Agulhão", "ALIMENTOS")
        );

        try (MockedStatic<ExcelReader> mockedReader = mockStatic(ExcelReader.class)) {
            mockedReader.when(() -> ExcelReader.read(any())).thenReturn(mockExcelData);

            when(firestore.collection("products")).thenReturn(collectionReference);
            when(collectionReference.document("Arroz_Agulhão")).thenReturn(documentReference);

            // Act
            importer.importProducts(inputStream, List.of());

            // Assert
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(documentReference).set(captor.capture());

            Map<String, Object> saved = captor.getValue();
            assertEquals("Arroz Agulhão", saved.get("productName"));

            List<String> keywords = (List<String>) saved.get("searchKeywords");
            assertTrue(keywords.contains("arroz"));
            assertTrue(keywords.contains("agulhão"));
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
            // 1. Stub the file reading
            mockedReader.when(() -> ExcelReader.read(any())).thenReturn(mockExcelData);

            // 2. IMPORTANT: Tell Mockito to use the real sanitization logic
            // Otherwise, toSafeId returns null, breaking the @NonNull Category record
            mockedReader.when(() -> ExcelReader.toSafeId(anyString())).thenCallRealMethod();

            // Act
            importer.importCategories(inputStream);

            // Assert
            verifyNoInteractions(firestore);
        }
    }
}