package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests - AutocompleteService")
class AutocompleteServiceTest {

    private AutocompleteService service;

    @BeforeEach
    void setUp() {
        service = new AutocompleteService();
    }

    @Test
    @DisplayName("Should index and suggest keywords from both Keywords and Products")
    void shouldIndexAndSuggestCorrectly() {
        // Arrange
        List<Keyword> keywords = List.of(
                new Keyword("Arroz", "GRAOS")
        );
        List<Product> products = List.of(
                new Product("Arroz Agulhão", "GRAOS", true, List.of("arroz", "agulhão"))
        );

        // Act
        service.indexUpdate(keywords, products);
        List<String> suggestions = service.suggest("arr");

        // Assert
        assertAll("Basic Suggestion",
                () -> assertTrue(suggestions.contains("arroz")),
                () -> assertEquals(1, suggestions.size(), "Should only have 'arroz' as it starts with 'arr'")
        );
    }

    @Test
    @DisplayName("Should be case-insensitive and handle whitespace")
    void shouldHandleCaseAndWhitespace() {
        // Arrange
        service.indexUpdate(List.of(new Keyword("Maçã", "FRUTAS")), List.of());

        // Act & Assert
        assertAll("Input Normalization",
                () -> assertEquals(1, service.suggest("MAÇ").size(), "Uppercase input should work"),
                () -> assertEquals(1, service.suggest("  maç  ").size(), "Input with spaces should be trimmed")
        );
    }

    @Test
    @DisplayName("Should return multiple categories for the same keyword")
    void shouldMapKeywordToMultipleCategories() {
        // Arrange
        // "Leite" exists in both Dairy and Vegan categories
        List<Keyword> keywords = List.of(
                new Keyword("Leite", "LACTICINIOS"),
                new Keyword("Leite", "VEGANO")
        );

        // Act
        service.indexUpdate(keywords, List.of());
        Set<String> categories = service.getCategoriesForKeyword("leite");

        // Assert
        assertEquals(2, categories.size());
        assertTrue(categories.containsAll(Set.of("LACTICINIOS", "VEGANO")));
    }

    @Test
    @DisplayName("Should limit results to 10 and keep them sorted")
    void shouldLimitAndSortResults() {
        // Arrange (11 items)
        List<Keyword> keywords = List.of(
                new Keyword("Abacate", "1"), new Keyword("Abacaxi", "1"),
                new Keyword("Abobora", "1"), new Keyword("Abobrinha", "1"),
                new Keyword("Acelga", "1"), new Keyword("Acerola", "1"),
                new Keyword("Alface", "1"), new Keyword("Alho", "1"),
                new Keyword("Arroz", "1"),
                new Keyword("Azeite", "1"),
                new Keyword("Azeitona", "1") // This is the 11th alphabetically
        );

        service.indexUpdate(keywords, List.of());

        // Act
        List<String> result = service.suggest("a");

        // Assert
        assertAll("Constraints",
                () -> assertEquals(10, result.size(), "Should not exceed 10 suggestions"),
                () -> assertEquals("abacate", result.getFirst(), "First item should be abacate"),
                // Azeitona is the last one alphabetically, so it should be the one cut off
                () -> assertFalse(result.contains("azeitona"), "Azeitona should be cut off by the limit"),
                () -> assertTrue(result.contains("arroz"), "Arroz should be included as it is 9th alphabetically")
        );
    }

    @Test
    @DisplayName("Should return empty list for null, blank or non-matching terms")
    void shouldHandleEdgeCases() {
        service.indexUpdate(List.of(new Keyword("Teste", "T")), List.of());

        assertAll("Edge Cases",
                () -> assertTrue(service.suggest(null).isEmpty()),
                () -> assertTrue(service.suggest("").isEmpty()),
                () -> assertTrue(service.suggest("   ").isEmpty()),
                () -> assertTrue(service.suggest("xyz").isEmpty())
        );
    }
}