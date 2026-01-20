package com.cartshare.backend.core.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.NonNull;

@Document(collectionName = "categories")
public record Category(
        @DocumentId @NonNull String id, // categoryId column
        @NonNull String name,           // name column
        @NonNull String classification, // classification column (NEW)
        int priority                    // priority column
) {}
