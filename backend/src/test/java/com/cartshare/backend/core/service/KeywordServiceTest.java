package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
        assertEquals("kw1", result.get(0).keyword());
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
}