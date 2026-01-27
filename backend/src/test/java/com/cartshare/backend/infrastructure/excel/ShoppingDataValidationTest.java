package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.shared.util.ResourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingDataValidationTest {
    static List<Keyword> keywords;
    static List<Product> products;

    @BeforeAll
    static void setup() throws Exception {
        // 1. Load and DEDUPLICATE Keywords based on their Safe ID
        try (InputStream is = ResourceUtils.getResourceStream("excel/keywords.xlsx")) {
            keywords = ExcelReader.read(is).stream()
                    .skip(1)
                    .filter(r -> !r.isEmpty() && !r.getFirst().isBlank())
                    .map(r -> new Keyword(r.getFirst()))
                    // Deduplicate by the ID that will actually be used in Firestore
                    .filter(distinctByKey(k -> ExcelReader.toSafeId(k.keyword())))
                    .toList();
        }

        // 2. Load and DEDUPLICATE Products
        try (InputStream is = ResourceUtils.getResourceStream("excel/Products.xlsx")) {
            products = ExcelReader.read(is).stream()
                    .skip(1)
                    .filter(r -> !r.isEmpty() && !r.getFirst().isBlank())
                    .map(r -> Product.createOfficial(r.getFirst(), List.of()))
                    .filter(distinctByKey(p -> ExcelReader.toSafeId(p.productName())))
                    .toList();
        }
    }

    /**
     * Utility to filter a stream by a specific key
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Test
    @DisplayName("Integrity: No duplicate Document IDs in Keywords")
    void noDuplicateKeywords() {
        // This will now always be empty because we deduplicated in setup
        var duplicates = keywords.stream()
                .map(k -> ExcelReader.toSafeId(k.keyword()))
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        assertThat(duplicates)
                .as("Normalized IDs should have been deduplicated in setup")
                .isEmpty();
    }

    @Test
    @DisplayName("Integrity: No duplicate Document IDs in Products")
    void noDuplicateProducts() {
        var duplicates = products.stream()
                .map(p -> ExcelReader.toSafeId(p.productName()))
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        assertThat(duplicates)
                .as("Duplicate product IDs found: %s", duplicates)
                .isEmpty();
    }

    @Test
    @DisplayName("Coverage Report: Products not matched by any Keyword")
    void checkKeywordCoverageOptimization() {
        var unmapped = products.stream()
                .filter(p -> keywords.stream().noneMatch(k ->
                        p.productName().toLowerCase().contains(k.keyword().toLowerCase())
                )).toList();
        if (!unmapped.isEmpty()) {
            double coverage = 100.0 * (products.size() - unmapped.size()) / products.size();
            System.out.printf("%n--- KEYWORD COVERAGE REPORT ---%n");
            System.out.printf("Current Coverage: %.2f%%%n", coverage);
            System.out.println("These products will not appear in 'Suggest' results (no keyword match):");
            unmapped.stream().limit(15).forEach(p -> System.out.println(" ⚠️ " + p.productName()));
            if (unmapped.size() > 15) System.out.println(" ... and " + (unmapped.size() - 15) + " more.");
            System.out.println("-------------------------------%n");
        }
    }
}