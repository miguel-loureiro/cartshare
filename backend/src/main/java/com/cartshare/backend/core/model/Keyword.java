package com.cartshare.backend.core.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.NonNull;

@Document(collectionName = "keywords")
public record Keyword(
        @DocumentId String keyword
) {}
