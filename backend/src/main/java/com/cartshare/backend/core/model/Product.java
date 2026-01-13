package com.cartshare.backend.core.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.NonNull;

import java.util.List;

@Document(collectionName = "products")
public record Product(
        String productName,
        @NonNull String categoryId,
        boolean isOfficial,             // true = Excel, false = User
        List<String> searchKeywords     // Generated automatically
) {}
