package com.cartshare.backend.core.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.NonNull;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document(collectionName = "categories")
public record Category(
        @DocumentId
        @NonNull String id,

        @NonNull String name,           // Ex: "Limpeza"
        @NonNull String classification,
        // Ex: "Casa"
        @NonNull List<String> keywords  // Ex: ["detergente", "lixívia", "amaciador"]
) {}
