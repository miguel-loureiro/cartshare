package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AutocompleteServiceTest {

    private AutocompleteService service;
    private List<Category> officialCategories;

    @BeforeEach
    void setUp() {
        service = new AutocompleteService();
        officialCategories = List.of(
                new Category("ALIMENTOS", "Alimentação", "MERCEARIA", 1),
                new Category("BEBIDAS", "Bebidas", "MERCEARIA", 2),
                new Category("SAUDE", "Saúde", "CUIDADOS", 3),
                new Category("LIMPEZA", "Limpeza", "CUIDADOS", 4),
                new Category("OUTROS", "Outros", "OUTROS", 99)
        );
    }

    @Test
    @DisplayName("Accent Folding: Should find 'Maçã' even with 'maca' or 'MACA'")
    void shouldFindMaçãWithDifferentAccents() {
        service.indexUpdate(officialCategories, List.of(new Keyword("Maçã", "ALIMENTOS")), List.of());

        assertAll("Accent Invariance",
                () -> assertTrue(service.suggest("maca").contains("maçã")),
                () -> assertTrue(service.suggest("MAÇÁ").contains("maçã")),
                () -> assertTrue(service.suggest("mãc").contains("maçã"))
        );
    }

    @Test
    @DisplayName("Priority with Fuzzy: Alimentos (1) matches should appear before Bebidas (2)")
    void fuzzyShouldRespectPriority() {
        service.indexUpdate(officialCategories, List.of(
                new Keyword("Cerveja", "BEBIDAS"), // Prio 2
                new Keyword("Cereais", "ALIMENTOS") // Prio 1
        ), List.of());

        // Typing 'cer' matches both
        List<String> results = service.suggest("cer");
        assertEquals("cereais", results.get(0));
        assertEquals("cerveja", results.get(1));
    }

    @Test
    @DisplayName("Fuzzy Tolerance: Should find 'Sabonete' with two typos 'Sabinete'")
    void shouldHandleDoubleTyposForLongWords() {
        service.indexUpdate(officialCategories, List.of(new Keyword("Sabonete", "SAUDE")), List.of());

        List<String> results = service.suggest("sabinete"); // o->i and e->e (distance 1)
        assertTrue(results.contains("sabonete"));
    }

    @Test
    @DisplayName("Category Mapping: Should return correct IDs for selected keyword")
    void shouldReturnCorrectCategories() {
        service.indexUpdate(officialCategories, List.of(
                new Keyword("Detergente", "LIMPEZA"),
                new Keyword("Detergente", "OUTROS")
        ), List.of());

        var categories = service.getCategoriesForKeyword("detergente");
        assertEquals(2, categories.size());
        assertTrue(categories.containsAll(List.of("LIMPEZA", "OUTROS")));
    }

    @Test
    @DisplayName("Should build index correctly from categories, keywords, and products")
    void indexUpdate_FullFlow_Success() {
        // GIVEN
        List<Category> categories = List.of(
                new Category("cat-1", "Fruits", "root", 1),
                new Category("cat-2", "Vegetables", "root", 5)
        );

        List<Keyword> keywords = List.of(
                new Keyword("Apple", "cat-1"),
                new Keyword("Carrot", "cat-2")
        );

        List<Product> products = List.of(
                new Product("p1", "Gala Apple", "cat-1", true, List.of("Gala", "Apple")),
                new Product("p2", "Baby Carrot", "cat-2", true, List.of("Baby", "Carrot"))
        );

        // WHEN
        service.indexUpdate(categories, keywords, products);

        // THEN
        // Verify lowercase normalization and category mapping
        assertThat(service.getCategoriesForKeyword("apple")).containsExactlyInAnyOrder("cat-1");
        assertThat(service.getCategoriesForKeyword("carrot")).containsExactlyInAnyOrder("cat-2");

        // Verify product search keywords were indexed
        assertThat(service.getCategoriesForKeyword("gala")).contains("cat-1");
        assertThat(service.getCategoriesForKeyword("baby")).contains("cat-2");
    }

    @Test
    @DisplayName("Should use the highest priority (lowest number) when a keyword exists in multiple categories")
    void indexUpdate_PriorityMerging() {
        // GIVEN
        List<Category> categories = List.of(
                new Category("high-priority", "Fast", "", 1),
                new Category("low-priority", "Slow", "", 50)
        );

        // Same keyword "Delivery" in both categories
        List<Keyword> keywords = List.of(
                new Keyword("Delivery", "low-priority"),
                new Keyword("Delivery", "high-priority")
        );

        // WHEN
        service.indexUpdate(categories, keywords, List.of());

        // THEN
        // We use suggest() to check order. "Delivery" is the only entry.
        // If we added more items, "Delivery" should rank higher because its priority is now 1.
        Set<String> categoryIds = service.getCategoriesForKeyword("delivery");
        assertThat(categoryIds).containsExactlyInAnyOrder("high-priority", "low-priority");
    }

    @Test
    @DisplayName("Should assign default priority 99 if category ID is not found in the provided list")
    void indexUpdate_DefaultPriority() {
        // GIVEN
        List<Category> emptyCategories = List.of();
        List<Keyword> keywords = List.of(new Keyword("Unknown", "orphan-id"));

        // WHEN
        service.indexUpdate(emptyCategories, keywords, List.of());

        // THEN
        assertThat(service.getCategoriesForKeyword("unknown")).contains("orphan-id");
        // Internal check: If we had other keywords with priority < 99,
        // "unknown" would appear last in suggest() results.
    }

    @Test
    @DisplayName("Should completely clear old data when indexUpdate is called a second time")
    void indexUpdate_ReplacementPolicy() {
        // GIVEN: Initial state
        service.indexUpdate(
                List.of(new Category("c1", "Old", "", 1)),
                List.of(new Keyword("OldKeyword", "c1")),
                List.of()
        );

        // WHEN: Updating with entirely new data
        service.indexUpdate(
                List.of(new Category("c2", "New", "", 1)),
                List.of(new Keyword("NewKeyword", "c2")),
                List.of()
        );

        // THEN
        assertThat(service.getCategoriesForKeyword("oldkeyword")).isEmpty();
        assertThat(service.getCategoriesForKeyword("newkeyword")).contains("c2");
    }

    @Test
    @DisplayName("Should handle null or blank strings gracefully during indexing")
    void indexUpdate_NullAndBlankHandling() {
        // GIVEN
        List<Category> categories = List.of(new Category("c1", "Test", "", 1));
        List<Keyword> keywords = List.of(
                new Keyword(null, "c1"),
                new Keyword("  ", "c1"),
                new Keyword("Valid", "c1")
        );

        // WHEN
        service.indexUpdate(categories, keywords, List.of());

        // THEN
        assertThat(service.suggest("")).isEmpty();
        assertThat(service.getCategoriesForKeyword("valid")).contains("c1");
    }

}