package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.shared.util.ResourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingDataValidationTest {

    static Set<String> categoryIds;
    static List<Keyword> keywords;
    static List<Product> products;

    @BeforeAll
    static void setup() throws Exception {
        // 1. Load Categories
        // Now expects: categoryId, name, classification, priority
        try (InputStream is = ResourceUtils.getResourceStream("excel/Categories.xlsx")) {
            categoryIds = ExcelReader.read(is).stream()
                    .filter(r -> !r.isEmpty())
                    .map(r -> ExcelReader.toSafeId(r.getFirst())) // Column A: sanitized categoryId
                    .collect(Collectors.toSet());
        }

        // 2. Load Keywords
        // Expects: keyword, categoryId
        try (InputStream is = ResourceUtils.getResourceStream("excel/keywords.xlsx")) {
            keywords = ExcelReader.read(is).stream()
                    .filter(r -> r.size() >= 2)
                    .map(r -> new Keyword(
                            r.getFirst(),
                            ExcelReader.toSafeId(r.get(1)) // Ensure categoryId is sanitized
                    ))
                    .toList();
        }

        // 3. Load Products
        // Now expects: productName, categoryId
        // We set isOfficial=true and generate keywords to simulate the real Importer behavior
        try (InputStream is = ResourceUtils.getResourceStream("excel/Products.xlsx")) {
            products = ExcelReader.read(is).stream()
                    .filter(r -> r.size() >= 2)
                    .map(r -> {
                        String name = r.get(0);
                        String catId = ExcelReader.toSafeId(r.get(1));
                        // Matches the 4-param constructor: name, categoryId, isOfficial, searchKeywords
                        return Product.createOfficial(name, catId, List.of());
                    })
                    .toList();
        }
    }

    @Test
    @DisplayName("Garantir que todas as palavras-chave apontam para categorias existentes")
    void allKeywordsMustHaveValidCategory() {
        var invalid = keywords.stream()
                .filter(k -> !categoryIds.contains(k.categoryId()))
                .toList();

        assertThat(invalid)
                .as("Keywords with invalid categoryId found in keywords.xlsx (Check for typos or missing categories)")
                .isEmpty();
    }

    @Test
    @DisplayName("Garantir que todos os produtos do Excel têm categorias válidas")
    void allProductsMustHaveValidCategory() {
        var invalid = products.stream()
                .filter(p -> !categoryIds.contains(p.categoryId()))
                .toList();

        assertThat(invalid)
                .as("Products with invalid categoryId found in Products.xlsx")
                .isEmpty();
    }

    @Test
    @DisplayName("Relatório de Cobertura: Produtos que não seriam capturados por palavras-chave")
    void checkKeywordCoverageOptimization() {
        var unmapped = products.stream()
                .filter(p -> keywords.stream().noneMatch(k ->
                        p.productName().toLowerCase().contains(k.keyword().toLowerCase())
                ))
                .toList();

        if (!unmapped.isEmpty()) {
            double coverage = 100.0 * (products.size() - unmapped.size()) / products.size();
            System.out.printf("%n--- RELATÓRIO DE COBERTURA DE KEYWORDS ---%n");
            System.out.printf("Cobertura Atual: %.2f%%%n", coverage);
            System.out.println("Estes produtos dependem de mapeamento explícito no Excel (não possuem keyword correspondente):");
            unmapped.stream().limit(10).forEach(p -> System.out.println(" - " + p.productName()));
            if (unmapped.size() > 10) System.out.println(" ... e mais " + (unmapped.size() - 10) + " itens.");
            System.out.println("------------------------------------------%n");
        }
    }
}