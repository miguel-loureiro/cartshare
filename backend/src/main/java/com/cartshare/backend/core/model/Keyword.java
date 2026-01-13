package com.cartshare.backend.core.model;

import com.google.cloud.spring.data.firestore.Document;
import lombok.NonNull;

@Document(collectionName = "keywords")
public record Keyword(
        String keyword,
        @NonNull String categoryId
) {}
