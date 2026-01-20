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

import java.util.List;
import java.util.concurrent.ExecutionException;

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
    @Mock private ApiFuture<DocumentSnapshot> documentSnapshotFuture;

    // Collection/Query specific mocks
    @Mock private ApiFuture<QuerySnapshot> futureQuerySnapshot;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private ApiFuture<QuerySnapshot> querySnapshotFuture;

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
    @DisplayName("deleteCategory: Should throw exception when category does not exist")
    void deleteCategory_NotFound() throws Exception {
        when(documentReference.get()).thenReturn(futureDocumentSnapshot);
        when(futureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> categoryService.deleteCategory("missing"));
        verify(documentReference, never()).delete();
    }

    // ===== GET ALL CATEGORIES =====

    @Test
    @DisplayName("getAllCategories: Should return all categories when collection is not empty")
    void getAllCategories_Success() throws Exception {
        // Arrange
        Category category1 = new Category("tech", "Technology", "Main", 1);
        Category category2 = new Category("food", "Food", "Main", 2);
        List<Category> expectedCategories = List.of(category1, category2);

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Category.class)).thenReturn(expectedCategories);

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(category1, category2);
        verify(collectionReference).get();
        verify(querySnapshotFuture).get();
    }

    @Test
    @DisplayName("getAllCategories: Should return empty list when no categories exist")
    void getAllCategories_EmptyCollection() throws Exception {
        // Arrange
        List<Category> emptyList = List.of();

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Category.class)).thenReturn(emptyList);

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(collectionReference).get();
    }

    @Test
    @DisplayName("getAllCategories: Should return multiple categories")
    void getAllCategories_MultipleCategoriesSuccess() throws Exception {
        // Arrange
        Category category1 = new Category("cat1", "Category 1", "Main", 1);
        Category category2 = new Category("cat2", "Category 2", "Sub", 2);
        Category category3 = new Category("cat3", "Category 3", "Main", 3);
        List<Category> expectedCategories = List.of(category1, category2, category3);

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.toObjects(Category.class)).thenReturn(expectedCategories);

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(category1, category2, category3);
    }

    @Test
    @DisplayName("getAllCategories: Should throw ExecutionException when Firestore fails")
    void getAllCategories_FirestoreExecutionException() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new ExecutionException("Firestore error", new RuntimeException()));

        // Act & Assert
        assertThrows(ExecutionException.class, () ->
                categoryService.getAllCategories()
        );
    }

    @Test
    @DisplayName("getAllCategories: Should throw InterruptedException when thread is interrupted")
    void getAllCategories_InterruptedException() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new InterruptedException("Thread interrupted"));

        // Act & Assert
        assertThrows(InterruptedException.class, () ->
                categoryService.getAllCategories()
        );
    }

    // ===== GET CATEGORY BY ID =====

    @Test
    @DisplayName("getCategoryById: Should return category when it exists")
    void getCategoryById_Success() throws Exception {
        // Arrange
        String categoryId = "tech";
        Category expectedCategory = new Category(categoryId, "Technology", "Main", 1);

        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(Category.class)).thenReturn(expectedCategory);

        // Act
        Category result = categoryService.getCategoryById(categoryId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(categoryId);
        assertThat(result.name()).isEqualTo("Technology");
        verify(collectionReference).document(categoryId);
        verify(documentReference).get();
    }

    @Test
    @DisplayName("getCategoryById: Should throw IllegalArgumentException when category does not exist")
    void getCategoryById_NotFound() throws Exception {
        // Arrange
        String categoryId = "unknown";

        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.getCategoryById(categoryId)
        );

        assertThat(exception.getMessage()).contains(categoryId);
        assertThat(exception.getMessage()).contains("not found");
        verify(documentReference).get();
    }

    @Test
    @DisplayName("getCategoryById: Should return correct category data")
    void getCategoryById_PreservesData() throws Exception {
        // Arrange
        String categoryId = "food";
        Category expectedCategory = new Category(categoryId, "Food & Beverages", "Main", 2);

        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(Category.class)).thenReturn(expectedCategory);

        // Act
        Category result = categoryService.getCategoryById(categoryId);

        // Assert
        assertThat(result.name()).isEqualTo("Food & Beverages");
        assertThat(result.classification()).isEqualTo("Main");
        assertThat(result.priority()).isEqualTo(2);
    }

    @Test
    @DisplayName("getCategoryById: Should throw ExecutionException on Firestore failure")
    void getCategoryById_FirestoreExecutionException() throws Exception {
        // Arrange
        String categoryId = "error";

        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenThrow(new ExecutionException("Firestore error", new RuntimeException()));

        // Act & Assert
        assertThrows(ExecutionException.class, () ->
                categoryService.getCategoryById(categoryId)
        );
    }

    @Test
    @DisplayName("getCategoryById: Should throw InterruptedException when thread is interrupted")
    void getCategoryById_InterruptedException() throws Exception {
        // Arrange
        String categoryId = "test";

        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenThrow(new InterruptedException("Thread interrupted"));

        // Act & Assert
        assertThrows(InterruptedException.class, () ->
                categoryService.getCategoryById(categoryId)
        );
    }

    // ===== GET CATEGORY COUNT =====

    @Test
    @DisplayName("getCategoryCount: Should return correct count")
    void getCategoryCount_Success() throws Exception {
        // Arrange
        int expectedCount = 5;

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(expectedCount);

        // Act
        long result = categoryService.getCategoryCount();

        // Assert
        assertThat(result).isEqualTo(5L);
        verify(collectionReference).get();
        verify(querySnapshotFuture).get();
    }

    @Test
    @DisplayName("getCategoryCount: Should return zero when no categories exist")
    void getCategoryCount_EmptyCollection() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(0);

        // Act
        long result = categoryService.getCategoryCount();

        // Assert
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("getCategoryCount: Should return long type")
    void getCategoryCount_ReturnTypeLong() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(10);

        // Act
        long result = categoryService.getCategoryCount();

        // Assert
        assertThat(result).isInstanceOf(Long.class);
        assertThat(result).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("getCategoryCount: Should return large count correctly")
    void getCategoryCount_LargeCount() throws Exception {
        // Arrange
        int largeCount = 1000;

        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.size()).thenReturn(largeCount);

        // Act
        long result = categoryService.getCategoryCount();

        // Assert
        assertThat(result).isEqualTo(1000L);
    }

    @Test
    @DisplayName("getCategoryCount: Should throw ExecutionException on Firestore failure")
    void getCategoryCount_FirestoreExecutionException() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new ExecutionException("Firestore error", new RuntimeException()));

        // Act & Assert
        assertThrows(ExecutionException.class, () ->
                categoryService.getCategoryCount()
        );
    }

    @Test
    @DisplayName("getCategoryCount: Should throw InterruptedException when thread is interrupted")
    void getCategoryCount_InterruptedException() throws Exception {
        // Arrange
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenThrow(new InterruptedException("Thread interrupted"));

        // Act & Assert
        assertThrows(InterruptedException.class, () ->
                categoryService.getCategoryCount()
        );
    }

    // ===== CREATE CATEGORY - EXCEPTION THROWING FOCUS =====

    @Test
    @DisplayName("createCategory: Should throw IllegalArgumentException when ID is null")
    void createCategory_NullId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory(null, "Name", "Classification", 1)
        );

        assertThat(exception.getMessage()).contains("Category ID is required");
        verify(firestore, never()).collection(anyString());
    }

    @Test
    @DisplayName("createCategory: Should throw IllegalArgumentException when ID is empty string")
    void createCategory_EmptyId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory("", "Name", "Classification", 1)
        );

        assertThat(exception.getMessage()).contains("Category ID is required");
        verify(firestore, never()).collection(anyString());
    }

    @Test
    @DisplayName("createCategory: Should throw IllegalArgumentException when ID is whitespace only")
    void createCategory_WhitespaceId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory("   ", "Name", "Classification", 1)
        );

        assertThat(exception.getMessage()).contains("Category ID is required");
        verify(firestore, never()).collection(anyString());
    }

    @Test
    @DisplayName("createCategory: Should throw IllegalArgumentException when category already exists")
    void createCategory_AlreadyExists_ThrowsException() throws Exception {
        // Arrange
        String id = "existing";

        // Mock categoryExists to return true
        DocumentReference existingDocRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> existingFuture = mock(ApiFuture.class);
        DocumentSnapshot existingSnapshot = mock(DocumentSnapshot.class);

        when(collectionReference.document(id)).thenReturn(existingDocRef);
        when(existingDocRef.get()).thenReturn(existingFuture);
        when(existingFuture.get()).thenReturn(existingSnapshot);
        when(existingSnapshot.exists()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory(id, "Name", "Classification", 1)
        );

        assertThat(exception.getMessage()).contains(id);
        assertThat(exception.getMessage()).contains("already exists");
        verify(documentReference, never()).set(any(), any());
    }

    @Test
    @DisplayName("createCategory: Should throw IllegalArgumentException with proper message for existing category")
    void createCategory_AlreadyExists_ProperErrorMessage() throws Exception {
        // Arrange
        String id = "duplicate-id";

        DocumentReference existingDocRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> existingFuture = mock(ApiFuture.class);
        DocumentSnapshot existingSnapshot = mock(DocumentSnapshot.class);

        when(collectionReference.document(id)).thenReturn(existingDocRef);
        when(existingDocRef.get()).thenReturn(existingFuture);
        when(existingFuture.get()).thenReturn(existingSnapshot);
        when(existingSnapshot.exists()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory(id, "Name", "Classification", 1)
        );

        String message = exception.getMessage();
        assertThat(message).contains("Category");
        assertThat(message).contains(id);
        assertThat(message).contains("already exists");
    }

    @Test
    @DisplayName("createCategory: Should successfully create category when all validations pass")
    void createCategory_ValidInput_Success() throws Exception {
        // Arrange
        String id = "new-category";
        String name = "New Category";
        String classification = "Main";
        int priority = 1;

        // Mock categoryExists check (the first call to document(id))
        DocumentSnapshot checkSnapshot = mock(DocumentSnapshot.class);
        when(checkSnapshot.exists()).thenReturn(false);
        ApiFuture<DocumentSnapshot> checkFuture = mock(ApiFuture.class);
        when(checkFuture.get()).thenReturn(checkSnapshot);

        // Mock the first document reference call for existence check
        DocumentReference checkDocRef = mock(DocumentReference.class);
        when(checkDocRef.get()).thenReturn(checkFuture);

        // Use ArgumentCaptor or multiple thenReturn to handle multiple calls to document()
        // First call: existence check, Second call: set operation
        when(collectionReference.document(id))
                .thenReturn(checkDocRef)      // First call for categoryExists()
                .thenReturn(documentReference); // Second call for set()

        // Mock set operation
        when(documentReference.set(any(Category.class))).thenReturn(futureWrite);
        when(futureWrite.get()).thenReturn(writeResult);

        // Act
        Category result = categoryService.createCategory(id, name, classification, priority);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.classification()).isEqualTo(classification);
        assertThat(result.priority()).isEqualTo(priority);
    }

    // ===== DELETE CATEGORY =====

    @Test
    @DisplayName("deleteCategory: Should delete category when it exists")
    void deleteCategory_Success() throws Exception {
        // Arrange
        String categoryId = "to-delete";

        // Mock categoryExists check
        DocumentSnapshot checkSnapshot = mock(DocumentSnapshot.class);
        when(checkSnapshot.exists()).thenReturn(true);
        ApiFuture<DocumentSnapshot> checkFuture = mock(ApiFuture.class);
        when(checkFuture.get()).thenReturn(checkSnapshot);

        // Mock the first document reference call for existence check
        DocumentReference checkDocRef = mock(DocumentReference.class);
        when(checkDocRef.get()).thenReturn(checkFuture);

        // Use thenReturn chain for multiple calls to document()
        // First call: existence check, Second call: delete operation
        when(collectionReference.document(categoryId))
                .thenReturn(checkDocRef)      // First call for categoryExists()
                .thenReturn(documentReference); // Second call for delete()

        // Mock delete operation
        when(documentReference.delete()).thenReturn(futureWrite);
        when(futureWrite.get()).thenReturn(writeResult);

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        verify(documentReference).delete();
    }

    @Test
    @DisplayName("deleteCategory: Should throw IllegalArgumentException when category does not exist")
    void deleteCategory_NotFound_ThrowsException() throws Exception {
        // Arrange
        String categoryId = "nonexistent";

        // Mock categoryExists to return false
        DocumentReference checkDocRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> checkFuture = mock(ApiFuture.class);
        DocumentSnapshot checkSnapshot = mock(DocumentSnapshot.class);

        when(collectionReference.document(categoryId)).thenReturn(checkDocRef);
        when(checkDocRef.get()).thenReturn(checkFuture);
        when(checkFuture.get()).thenReturn(checkSnapshot);
        when(checkSnapshot.exists()).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.deleteCategory(categoryId)
        );

        assertThat(exception.getMessage()).contains(categoryId);
        assertThat(exception.getMessage()).contains("not found");
        verify(documentReference, never()).delete();
    }

    @Test
    @DisplayName("deleteCategory: Should throw IllegalArgumentException with proper error message")
    void deleteCategory_NotFound_ProperErrorMessage() throws Exception {
        // Arrange
        String categoryId = "missing-category";

        DocumentReference checkDocRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> checkFuture = mock(ApiFuture.class);
        DocumentSnapshot checkSnapshot = mock(DocumentSnapshot.class);

        when(collectionReference.document(categoryId)).thenReturn(checkDocRef);
        when(checkDocRef.get()).thenReturn(checkFuture);
        when(checkFuture.get()).thenReturn(checkSnapshot);
        when(checkSnapshot.exists()).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.deleteCategory(categoryId)
        );

        String message = exception.getMessage();
        assertThat(message).contains("Category");
        assertThat(message).contains(categoryId);
        assertThat(message).contains("not found");
    }

    @Test
    @DisplayName("deleteCategory: Should throw ExecutionException on Firestore failure")
    void deleteCategory_FirestoreExecutionException() throws Exception {
        // Arrange
        String categoryId = "error";

        DocumentReference checkDocRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> checkFuture = mock(ApiFuture.class);

        when(collectionReference.document(categoryId)).thenReturn(checkDocRef);
        when(checkDocRef.get()).thenReturn(checkFuture);
        when(checkFuture.get()).thenThrow(new ExecutionException("Firestore error", new RuntimeException()));

        // Act & Assert
        assertThrows(ExecutionException.class, () ->
                categoryService.deleteCategory(categoryId)
        );
    }

    @Test
    @DisplayName("deleteCategory: Should throw InterruptedException when thread is interrupted")
    void deleteCategory_InterruptedException() throws Exception {
        // Arrange
        String categoryId = "test";

        DocumentReference checkDocRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> checkFuture = mock(ApiFuture.class);

        when(collectionReference.document(categoryId)).thenReturn(checkDocRef);
        when(checkDocRef.get()).thenReturn(checkFuture);
        when(checkFuture.get()).thenThrow(new InterruptedException("Thread interrupted"));

        // Act & Assert
        assertThrows(InterruptedException.class, () ->
                categoryService.deleteCategory(categoryId)
        );
    }

}