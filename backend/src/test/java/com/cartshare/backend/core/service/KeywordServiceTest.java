package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock private Firestore firestore;
    @Mock private CollectionReference collectionReference;
    @Mock private DocumentReference documentReference;
    @Mock private Query query;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private ApiFuture<QuerySnapshot> querySnapshotFuture;

    // Separate Futures for clarity
    @Mock private com.google.api.core.ApiFuture<QuerySnapshot> futureQuerySnapshot;
    @Mock private com.google.api.core.ApiFuture<WriteResult> futureWriteResult;

    private KeywordService keywordService;

    @BeforeEach
    void setUp() {
        keywordService = new KeywordService(firestore);
        // Common setup: collection("keywords") always returns our mock
        lenient().when(firestore.collection("keywords")).thenReturn(collectionReference);
    }

    @Test
    @DisplayName("Create Keywords For Product: Should create keywords if they don't exist")
    void shouldCreateKeywordsForProduct() throws Exception {
        // Arrange
        List<String> searchKeywords = List.of("arroz", "agulhao");

        // Mock the "exists" check chain
        when(collectionReference.whereEqualTo(eq("keyword"), anyString())).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(0); // 0 means it doesn't exist

        // Mock the "save" chain
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any(Keyword.class))).thenReturn(futureWriteResult);

        // Act
        keywordService.createKeywordsForProduct("Product", "cat-1", searchKeywords);

        // Assert
        verify(documentReference, times(2)).set(any(Keyword.class));
    }

    @Test
    @DisplayName("Create Keywords For Product: Should skip existing keywords")
    void shouldSkipExistingKeywords() throws Exception {
        // Arrange
        List<String> searchKeywords = List.of("cafe", "novo");

        when(collectionReference.whereEqualTo(eq("keyword"), anyString())).thenReturn(query);
        when(query.get()).thenReturn(futureQuerySnapshot);

        // First call returns size 1 (exists), second call returns size 0 (doesn't exist)
        QuerySnapshot existsSnapshot = mock(QuerySnapshot.class);
        QuerySnapshot notExistsSnapshot = mock(QuerySnapshot.class);
        when(existsSnapshot.size()).thenReturn(1);
        when(notExistsSnapshot.size()).thenReturn(0);

        when(futureQuerySnapshot.get())
                .thenReturn(existsSnapshot)
                .thenReturn(notExistsSnapshot);

        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any(Keyword.class))).thenReturn(futureWriteResult);

        // Act
        keywordService.createKeywordsForProduct("Coffee", "cat-1", searchKeywords);

        // Assert: Only 1 set() call because "cafe" was skipped
        verify(documentReference, times(1)).set(any(Keyword.class));
    }

    @Test
    @DisplayName("Get All Keywords: Should return all keywords from collection")
    void shouldGetAllKeywords() throws Exception {
        // Arrange
        List<Keyword> expected = List.of(new Keyword("kw1", "cat1"));

        when(collectionReference.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(expected);

        // Act
        List<Keyword> result = keywordService.getAllKeywords();

        // Assert
        assertEquals(1, result.size());
        assertEquals("kw1", result.getFirst().keyword());
    }

    @Test
    @DisplayName("Create Keywords For Product: Should handle Firestore exceptions gracefully")
    void shouldHandleFirestoreExceptionGracefully() throws Exception {
        // Arrange
        when(collectionReference.whereEqualTo(anyString(), anyString())).thenThrow(new RuntimeException("DB Down"));

        // Act & Assert
        assertDoesNotThrow(() ->
                keywordService.createKeywordsForProduct("Test", "cat", List.of("kw"))
        );
    }

    // ===== GET KEYWORDS BY CATEGORY =====

    @Test
    @DisplayName("getKeywordsByCategory: Should return keywords for specified category")
    void getKeywordsByCategory_Success() throws Exception {
        // Arrange
        String categoryId = "BEVERAGES";
        Keyword keyword1 = new Keyword("coca", categoryId);
        Keyword keyword2 = new Keyword("pepsi", categoryId);
        List<Keyword> expectedKeywords = List.of(keyword1, keyword2);

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(expectedKeywords);

        // Act
        List<Keyword> result = keywordService.getKeywordsByCategory(categoryId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(keyword1, keyword2);
        verify(collectionReference).whereEqualTo("categoryId", categoryId);
        verify(query).get();
    }

    @Test
    @DisplayName("getKeywordsByCategory: Should return empty list when no keywords exist for category")
    void getKeywordsByCategory_NoKeywordsFound() throws Exception {
        // Arrange
        String categoryId = "NONEXISTENT";
        List<Keyword> emptyList = List.of();

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(emptyList);

        // Act
        List<Keyword> result = keywordService.getKeywordsByCategory(categoryId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(collectionReference).whereEqualTo("categoryId", categoryId);
        verify(query).get();
    }

    @Test
    @DisplayName("getKeywordsByCategory: Should filter by exact category ID match")
    void getKeywordsByCategory_ExactMatch() throws Exception {
        // Arrange
        String categoryId = "DAIRY";
        Keyword keyword = new Keyword("milk", categoryId);

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(List.of(keyword));

        // Act
        List<Keyword> result = keywordService.getKeywordsByCategory(categoryId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().categoryId()).isEqualTo(categoryId);
        verify(collectionReference).whereEqualTo("categoryId", categoryId);
    }

    @Test
    @DisplayName("getKeywordsByCategory: Should return multiple keywords for same category")
    void getKeywordsByCategory_MultipleKeywords() throws Exception {
        // Arrange
        String categoryId = "FRUITS";
        Keyword keyword1 = new Keyword("apple", categoryId);
        Keyword keyword2 = new Keyword("banana", categoryId);
        Keyword keyword3 = new Keyword("orange", categoryId);
        List<Keyword> expectedKeywords = List.of(keyword1, keyword2, keyword3);

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(expectedKeywords);

        // Act
        List<Keyword> result = keywordService.getKeywordsByCategory(categoryId);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(keyword1, keyword2, keyword3);
    }

    @Test
    @DisplayName("getKeywordsByCategory: Should handle category with special characters")
    void getKeywordsByCategory_SpecialCharactersCategory() throws Exception {
        // Arrange
        String categoryId = "OTHERS-SPECIAL_FOOD";
        Keyword keyword = new Keyword("unusual", categoryId);

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(List.of(keyword));

        // Act
        List<Keyword> result = keywordService.getKeywordsByCategory(categoryId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().categoryId()).isEqualTo(categoryId);
        verify(collectionReference).whereEqualTo("categoryId", categoryId);
    }

    @Test
    @DisplayName("getKeywordsByCategory: Should throw ExecutionException when Firestore fails")
    void getKeywordsByCategory_FirestoreExecutionException() throws Exception {
        // Arrange
        String categoryId = "BEVERAGES";

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new ExecutionException("Firestore error", new RuntimeException()));

        // Act & Assert
        assertThrows(ExecutionException.class, () ->
                keywordService.getKeywordsByCategory(categoryId)
        );
    }

    @Test
    @DisplayName("getKeywordsByCategory: Should throw InterruptedException when thread is interrupted")
    void getKeywordsByCategory_InterruptedException() throws Exception {
        // Arrange
        String categoryId = "DAIRY";

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new InterruptedException("Thread interrupted"));

        // Act & Assert
        assertThrows(InterruptedException.class, () ->
                keywordService.getKeywordsByCategory(categoryId)
        );
    }

    @Test
    @DisplayName("getKeywordsByCategory: Should preserve keyword data when retrieving")
    void getKeywordsByCategory_PreservesKeywordData() throws Exception {
        // Arrange
        String categoryId = "SWEETS";
        Keyword keyword = new Keyword("chocolate", categoryId);

        when(collectionReference.whereEqualTo("categoryId", categoryId)).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(List.of(keyword));

        // Act
        List<Keyword> result = keywordService.getKeywordsByCategory(categoryId);

        // Assert
        assertThat(result).hasSize(1);
        Keyword retrieved = result.getFirst();
        assertThat(retrieved.keyword()).isEqualTo("chocolate");
        assertThat(retrieved.categoryId()).isEqualTo(categoryId);
    }

    // ===== GET KEYWORD COUNT =====

    @Test
    @DisplayName("getKeywordCount: Should return correct keyword count")
    void getKeywordCount_Success() throws Exception {
        // Arrange
        long expectedCount = 15L;

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn((int) expectedCount);

        // Act
        long result = keywordService.getKeywordCount();

        // Assert
        assertThat(result).isEqualTo(expectedCount);
        verify(collectionReference).get();
    }

    @Test
    @DisplayName("getKeywordCount: Should return zero when no keywords exist")
    void getKeywordCount_EmptyCollection() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(0);

        // Act
        long result = keywordService.getKeywordCount();

        // Assert
        assertThat(result).isEqualTo(0L);
        verify(collectionReference).get();
    }

    @Test
    @DisplayName("getKeywordCount: Should return correct count for large number of keywords")
    void getKeywordCount_LargeCount() throws Exception {
        // Arrange
        long expectedCount = 1000L;

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn((int) expectedCount);

        // Act
        long result = keywordService.getKeywordCount();

        // Assert
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("getKeywordCount: Should return long type")
    void getKeywordCount_ReturnTypeLong() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(5);

        // Act
        long result = keywordService.getKeywordCount();

        // Assert
        assertThat(result).isInstanceOf(Long.class);
        assertThat(result).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("getKeywordCount: Should throw ExecutionException when Firestore fails")
    void getKeywordCount_FirestoreExecutionException() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new ExecutionException("Firestore error", new RuntimeException()));

        // Act & Assert
        assertThrows(ExecutionException.class, () ->
                keywordService.getKeywordCount()
        );
    }

    @Test
    @DisplayName("getKeywordCount: Should throw InterruptedException when thread is interrupted")
    void getKeywordCount_InterruptedException() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new InterruptedException("Thread interrupted"));

        // Act & Assert
        assertThrows(InterruptedException.class, () ->
                keywordService.getKeywordCount()
        );
    }

    @Test
    @DisplayName("getKeywordCount: Should count only keywords collection")
    void getKeywordCount_OnlyCountsKeywordsCollection() throws Exception {
        // Arrange
        long expectedCount = 42L;

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn((int) expectedCount);

        // Act
        keywordService.getKeywordCount();

        // Assert
        verify(firestore).collection("keywords");
        verify(collectionReference).get();
        verify(querySnapshotFuture).get();
    }

    @Test
    @DisplayName("getKeywordCount: Should return one when single keyword exists")
    void getKeywordCount_SingleKeyword() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(1);

        // Act
        long result = keywordService.getKeywordCount();

        // Assert
        assertThat(result).isEqualTo(1L);
    }


}