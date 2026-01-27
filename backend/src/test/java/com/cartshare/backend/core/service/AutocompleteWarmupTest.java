package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
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
    @Mock private AutocompleteService autocompleteService;

    @InjectMocks
    private AutocompleteWarmup autocompleteWarmup;

    @Mock private CollectionReference collectionReference;
    @Mock private ApiFuture<QuerySnapshot> apiFuture;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private QueryDocumentSnapshot docSnapshot;

    @BeforeEach
    void setUp() throws Exception {
        // Standard stubbing for the firestore.collection("...").get() chain
        lenient().when(firestore.collection(anyString())).thenReturn(collectionReference);
        lenient().when(collectionReference.get()).thenReturn(apiFuture);
        lenient().when(apiFuture.get()).thenReturn(querySnapshot);
    }

    @Test
    @DisplayName("run: Should fetch Keywords and Products and update index successfully")
    void run_SuccessfulWarmup() throws Exception {
        // Arrange
        // Simulate finding documents so the retry logic doesn't trigger
        when(querySnapshot.getDocuments()).thenReturn(List.of(docSnapshot));

        // Ensure toObject returns a valid instance to avoid NPEs in AutocompleteService
        when(docSnapshot.toObject(Keyword.class)).thenReturn(new Keyword("test"));
        when(docSnapshot.toObject(Product.class)).thenReturn(Product.createOfficial("test", List.of()));

        // Act
        autocompleteWarmup.run();

        // Assert
        // Verify only 2 lists are passed now (keywords, products)
        verify(autocompleteService, times(1)).indexUpdate(anyList(), anyList());

        // Verify only 2 collections are hit (categories was removed)
        verify(firestore, times(2)).collection(anyString());
        verify(firestore).collection("keywords");
        verify(firestore).collection("products");
    }

    @Test
    @DisplayName("run: Should retry when collection is initially empty")
    void run_RetryWhenEmpty() throws Exception {
        // Arrange
        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        when(emptySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        QuerySnapshot fullSnapshot = mock(QuerySnapshot.class);
        when(fullSnapshot.getDocuments()).thenReturn(List.of(docSnapshot));

        // First call returns empty (triggers retry), second call returns data
        when(apiFuture.get()).thenReturn(emptySnapshot, fullSnapshot);

        // Act
        autocompleteWarmup.run();

        // Assert
        // Keywords took 2 calls (initial + 1 retry), Products took 1 call = 3 total
        verify(firestore, times(3)).collection(anyString());
    }
}