package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Crucial for Firestore fluent API mocking
class CategoryServiceTest {

    @Mock
    private Firestore firestore;
    @Mock private CollectionReference collectionReference;
    @Mock private DocumentReference documentReference;

    // Document specific mocks
    @Mock private ApiFuture<DocumentSnapshot> futureDocumentSnapshot;
    @Mock private DocumentSnapshot documentSnapshot;

    // Collection/Query specific mocks
    @Mock private ApiFuture<QuerySnapshot> futureQuerySnapshot;
    @Mock private QuerySnapshot querySnapshot;

    // Write specific mocks
    @Mock private ApiFuture<WriteResult> futureWrite;
    @Mock private WriteResult writeResult;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        // Use doReturn for chained calls to bypass strict argument matching during setup
        doReturn(collectionReference).when(firestore).collection("categories");
        doReturn(documentReference).when(collectionReference).document(anyString());
    }

    // ===== GET CATEGORY BY ID =====

    @Test
    @DisplayName("getCategoryById: Should return category when it exists")
    void getCategoryById_Success() throws Exception {
        String categoryId = "tech";
        Category expectedCategory = new Category(categoryId, "Technology", "Main", 1);

        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(Category.class)).thenReturn(expectedCategory);

        Category result = categoryService.getCategoryById(categoryId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(categoryId);
        verify(documentReference).get();
    }

    @Test
    @DisplayName("getCategoryById: Should throw exception when category does not exist")
    void getCategoryById_NotFound() throws Exception {
        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> categoryService.getCategoryById("unknown"));
    }

    // ===== GET CATEGORY COUNT =====

    @Test
    @DisplayName("getCategoryCount: Should return correct count")
    void getCategoryCount_Success() throws Exception {
        // Correcting: collectionReference.get() returns ApiFuture<QuerySnapshot>
        when(collectionReference.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(5);

        long result = categoryService.getCategoryCount();

        assertEquals(5, result);
    }

    @Test
    @DisplayName("getCategoryCount: Should return zero when no categories exist")
    void getCategoryCount_Empty() throws Exception {
        when(collectionReference.get()).thenReturn(futureQuerySnapshot);
        when(futureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(0);

        long result = categoryService.getCategoryCount();

        assertEquals(0, result);
    }

    // ===== CREATE CATEGORY =====

    @Test
    @DisplayName("createCategory: Should save new category when ID is unique")
    void createCategory_Success() throws Exception {
        String id = "new-cat";

        // Stub existence check
        doReturn(futureDocumentSnapshot).when(documentReference).get();
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // Stub save operation
        doReturn(futureWrite).when(documentReference).set(any(Category.class));
        when(futureWrite.get()).thenReturn(writeResult);

        Category result = categoryService.createCategory(id, "New", "Sub", 1);

        assertThat(result.id()).isEqualTo(id);
        verify(documentReference).set(any(Category.class));
    }

    @Test
    @DisplayName("createCategory: Should throw exception when category already exists")
    void createCategory_AlreadyExists() throws Exception {
        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory("existing-cat", "Name", "Class", 1)
        );

        verify(documentReference, never()).set(any(Category.class));
    }

    // ===== DELETE CATEGORY =====

    @Test
    @DisplayName("deleteCategory: Should delete category when it exists")
    void deleteCategory_Success() throws Exception {
        // Exists check
        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        // Delete operation
        when(documentReference.delete()).thenReturn(futureWrite);
        when(futureWrite.get()).thenReturn(writeResult);

        categoryService.deleteCategory("to-delete");

        verify(documentReference).delete();
    }

    @Test
    @DisplayName("deleteCategory: Should throw exception when category does not exist")
    void deleteCategory_NotFound() throws Exception {
        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> categoryService.deleteCategory("missing"));
        verify(documentReference, never()).delete();
    }
}