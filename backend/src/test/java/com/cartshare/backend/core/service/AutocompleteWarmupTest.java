package com.cartshare.backend.core.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutocompleteWarmupTest {

    @Mock private Firestore firestore;
    @Mock
    private AutocompleteService autocompleteService;

    @InjectMocks
    private AutocompleteWarmup autocompleteWarmup;

    // Helper mocks for the Firestore chain
    private CollectionReference collectionReference;
    private ApiFuture<QuerySnapshot> apiFuture;
    private QuerySnapshot querySnapshot;

    @BeforeEach
    void setUp() {
        collectionReference = mock(CollectionReference.class);
        apiFuture = mock(ApiFuture.class);
        querySnapshot = mock(QuerySnapshot.class);

        // Standard stubbing for the firestore.collection("...").get() chain
        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.get()).thenReturn(apiFuture);
    }

    @Test
    @DisplayName("run: Should fetch all collections and update index successfully")
    void run_SuccessfulWarmup() throws Exception {
        // Arrange
        when(apiFuture.get()).thenReturn(querySnapshot);
        // Simulate finding 1 document for each of the 3 calls
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(querySnapshot.getDocuments()).thenReturn(List.of(doc));

        // Act
        autocompleteWarmup.run();

        // Assert
        verify(autocompleteService, times(1)).indexUpdate(anyList(), anyList(), anyList());
        verify(firestore, times(3)).collection(anyString());
    }

    @Test
    @DisplayName("run: Should retry 3 times when collection is empty")
    void run_RetriesWhenEmpty() throws Exception {
        // Arrange
        when(apiFuture.get()).thenReturn(querySnapshot);
        // Simulate empty documents
        when(querySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        // Act
        autocompleteWarmup.run();

        // Assert
        // 3 collections * 3 attempts = 9 calls to get()
        verify(apiFuture, times(9)).get();
        // Since it stayed empty, indexUpdate should be called with empty lists
        verify(autocompleteService).indexUpdate(anyList(), anyList(), anyList());
    }

    @Test
    @DisplayName("run: Should handle exceptions without crashing the application")
    void run_HandlesExceptionGracefully() throws Exception {
        // Arrange
        when(apiFuture.get()).thenThrow(new RuntimeException("Firestore Timeout"));

        // Act
        autocompleteWarmup.run();

        // Assert
        // Verification that indexUpdate was NEVER called due to the exception
        verify(autocompleteService, never()).indexUpdate(any(), any(), any());
    }
}