package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final Firestore firestore;

    /**
     * Get all categories
     */
    public List<Category> getAllCategories() throws ExecutionException, InterruptedException {
        List<Category> categories = firestore.collection("categories")
                .get()
                .get()
                .toObjects(Category.class);

        log.info("📦 Categories fetched: {}", categories.size());
        return categories;
    }

    /**
     * Get category by ID
     */
    public Category getCategoryById(String categoryId) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection("categories")
                .document(categoryId)
                .get()
                .get();

        if (!doc.exists()) {
            throw new IllegalArgumentException("Category '" + categoryId + "' not found");
        }

        return doc.toObject(Category.class);
    }

    /**
     * Check if category exists
     */
    public boolean categoryExists(String categoryId) throws ExecutionException, InterruptedException {
        return firestore.collection("categories")
                .document(categoryId)
                .get()
                .get()
                .exists();
    }

    /**
     * Get category count
     */
    public long getCategoryCount() throws ExecutionException, InterruptedException {
        return firestore.collection("categories").get().get().size();
    }

    /**
     * Create a category
     */
    public Category createCategory(String id, String name, String classification, int priority)
            throws ExecutionException, InterruptedException {

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Category ID is required");
        }

        if (categoryExists(id)) {
            throw new IllegalArgumentException("Category '" + id + "' already exists");
        }

        Category category = new Category(id, name, classification, priority);
        firestore.collection("categories")
                .document(id)
                .set(category)
                .get();

        log.info("✅ Category created: {}", id);
        return category;
    }

    /**
     * Delete a category
     */
    public void deleteCategory(String categoryId) throws ExecutionException, InterruptedException {
        if (!categoryExists(categoryId)) {
            throw new IllegalArgumentException("Category '" + categoryId + "' not found");
        }

        firestore.collection("categories")
                .document(categoryId)
                .delete()
                .get();

        log.info("✅ Category deleted: {}", categoryId);
    }
}
