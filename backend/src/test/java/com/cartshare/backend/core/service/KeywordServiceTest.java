package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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
    @Mock private DocumentSnapshot documentSnapshot;
    @Mock private ApiFuture<DocumentSnapshot> futureDocumentSnapshot;
    @Mock private ApiFuture<QuerySnapshot> futureQuerySnapshot;
    @Mock private ApiFuture<WriteResult> futureWriteResult;
    @Mock private QuerySnapshot querySnapshot;

    @InjectMocks
    private KeywordService keywordService;

    @BeforeEach
    void setUp() {
        lenient().when(firestore.collection("keywords")).thenReturn(collectionReference);
        // Default behavior: any document request returns our standard mock docRef
        lenient().when(collectionReference.document(anyString())).thenReturn(documentReference);
    }

    // ===== CREATE KEYWORDS FOR PRODUCT =====

    @Test
    @DisplayName("createKeywordsForProduct: Should create keywords if they don't exist")
    void shouldCreateKeywordsForProduct() throws Exception {
        List<String> searchKeywords = List.of("Arroz", "Agulhão");

        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false); // None exist
        when(documentReference.set(any(Keyword.class))).thenReturn(futureWriteResult);
        when(futureWriteResult.get()).thenReturn(mock(WriteResult.class));

        keywordService.createKeywordsForProduct("Product Test", searchKeywords);

        // Verify sanitized IDs: "Arroz" -> "arroz", "Agulhão" -> "agulhao"
        verify(collectionReference).document("arroz");
        verify(collectionReference).document("agulhao");
        verify(documentReference, times(2)).set(any(Keyword.class));
    }

    @Test
    @DisplayName("createKeywordsForProduct: Should skip existing keywords")
    void shouldSkipExistingKeywords() throws Exception {
        List<String> searchKeywords = List.of("cafe", "novo");

        // We create specific mocks to avoid conflicting with the 'anyString' default if needed
        DocumentReference docRefCafe = mock(DocumentReference.class);
        DocumentReference docRefNovo = mock(DocumentReference.class);
        DocumentSnapshot snapCafe = mock(DocumentSnapshot.class);
        DocumentSnapshot snapNovo = mock(DocumentSnapshot.class);
        ApiFuture<DocumentSnapshot> futureCafe = mock(ApiFuture.class);
        ApiFuture<DocumentSnapshot> futureNovo = mock(ApiFuture.class);

        when(collectionReference.document("cafe")).thenReturn(docRefCafe);
        when(collectionReference.document("novo")).thenReturn(docRefNovo);

        // "cafe" exists
        when(docRefCafe.get()).thenReturn(futureCafe);
        when(futureCafe.get()).thenReturn(snapCafe);
        when(snapCafe.exists()).thenReturn(true);

        // "novo" does not exist
        when(docRefNovo.get()).thenReturn(futureNovo);
        when(futureNovo.get()).thenReturn(snapNovo);
        when(snapNovo.exists()).thenReturn(false);
        when(docRefNovo.set(any(Keyword.class))).thenReturn(futureWriteResult);

        keywordService.createKeywordsForProduct("Coffee", searchKeywords);

        verify(docRefCafe, never()).set(any());
        verify(docRefNovo, times(1)).set(any(Keyword.class));
    }

    @Test
    @DisplayName("createKeywordsForProduct: Should handle Firestore exceptions gracefully")
    void shouldHandleFirestoreExceptionGracefully() {
        when(collectionReference.document(anyString())).thenThrow(new RuntimeException("DB Down"));

        assertDoesNotThrow(() ->
                keywordService.createKeywordsForProduct("Test", List.of("kw"))
        );
    }

    // ===== GET ALL KEYWORDS =====

    @Test
    @DisplayName("getAllKeywords: Should return all keywords from collection")
    void shouldGetAllKeywords() throws Exception {
        List<Keyword> expected = List.of(new Keyword("kw1"), new Keyword("kw2"));

        when(collectionReference.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Keyword.class)).thenReturn(expected);

        List<Keyword> result = keywordService.getAllKeywords();

        assertThat(result).hasSize(2).extracting(Keyword::keyword).containsExactly("kw1", "kw2");
    }

    // ===== GET KEYWORD COUNT =====

    @Test
    @DisplayName("getKeywordCount: Should return correct keyword count")
    void getKeywordCount_Success() throws Exception {
        int expectedCount = 15;
        when(collectionReference.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(expectedCount);

        long result = keywordService.getKeywordCount();

        assertThat(result).isEqualTo(15L);
    }

    @Test
    @DisplayName("getKeywordCount: Should throw ExecutionException when Firestore fails")
    void getKeywordCount_FirestoreExecutionException() throws Exception {
        when(collectionReference.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenThrow(new ExecutionException("Error", new RuntimeException()));

        assertThrows(ExecutionException.class, () -> keywordService.getKeywordCount());
    }

    // =========== NORMALIZATION ================
    @Test
    @DisplayName("Normalization: Different variations of a word should point to the same Document ID")
    void shouldNormalizeKeywordsToSameId() throws Exception {
        // Arrange: Three variations of the same word
        List<String> variations = List.of("Maçã", "maca", "MAÇÃ ");

        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);

        // Simulate they don't exist yet
        when(documentSnapshot.exists()).thenReturn(false);
        when(documentReference.set(any(Keyword.class))).thenReturn(futureWriteResult);
        when(futureWriteResult.get()).thenReturn(mock(WriteResult.class));

        // Act
        keywordService.createKeywordsForProduct("Fruit", variations);

        // Assert: All three variations should result in "maca"
        // verify(..., times(3)) because the service calls document() for each iteration
        verify(collectionReference, times(3)).document("maca");

        // Verify it only attempted to set the data if it didn't exist
        // (Note: In a real scenario with the first call, it would exist for the 2nd/3rd call,
        // but here we are testing the ID mapping logic specifically).
    }
}