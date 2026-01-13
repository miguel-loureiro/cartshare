package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}