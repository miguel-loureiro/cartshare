package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.shared.util.SearchUtils;
import com.cartshare.backend.shared.util.StringUtils;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final Firestore firestore;
    // You might need to inject the keywords list or a KeywordRepository here

    public Product addOrGetProduct(String originalName, List<Keyword> currentKeywords) throws Exception {
        String docId = StringUtils.toSafeId(originalName);
        DocumentReference docRef = firestore.collection("products").document(docId);

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef).get();

            if (snapshot.exists()) {
                return snapshot.toObject(Product.class);
            }

            String resolvedCategory = ProductCategoryMatcher.resolveCategory(
                    originalName,
                    currentKeywords,
                    "OUTROS" // Default fallback
            );

            Product newProduct = new Product(
                    docId,
                    originalName,
                    resolvedCategory,
                    false, // Not official (added by user)
                    SearchUtils.generateSearchKeywords(originalName)
            );

            transaction.set(docRef, newProduct);
            return newProduct;
        }).get();
    }
}
