package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;

import java.util.List;

public class ProductCategoryMatcher {

    public static String resolveCategory(
            String productName,
            List<Keyword> keywords,
            String fallbackCategory
    ) {
        return keywords.stream()
                .filter(k -> productName.toLowerCase().contains(k.keyword()))
                .map(Keyword::categoryId)
                .findFirst()
                .orElse(fallbackCategory);
    }
}
