package com.cartshare.backend.core.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.NonNull;
import java.util.List;@Document(collectionName = "products")

public record Product(
        @DocumentId String id,
        @NonNull String productName,
        boolean isOfficial,
        List<String> searchKeywords
) {
    public Product {
        if (productName.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        searchKeywords = (searchKeywords == null) ? List.of() : searchKeywords;
    }

    /**
     * Create a new Product without ID (for new documents)
     * Used when creating products from user input or initial seed
     */
    public static Product create(String productName, boolean isOfficial, List<String> searchKeywords) {
        return new Product(null, productName, isOfficial, searchKeywords);
    }

    /**
     * Create a new Product with all fields
     * Used when loading from Firestore or testing
     */
    public static Product of(String id, String productName, boolean isOfficial, List<String> searchKeywords) {
        return new Product(id, productName, isOfficial, searchKeywords);
    }

    /**
     * Create an official product (from Excel seed)
     */
    public static Product createOfficial(String productName, List<String> searchKeywords) {
        return new Product(null, productName,true, searchKeywords);
    }

    /**
     * Create a user-contributed product
     */
    public static Product createUserContributed(String productName, List<String> searchKeywords) {
        return new Product(null, productName,false, searchKeywords);
    }
}