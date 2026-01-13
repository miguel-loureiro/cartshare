package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProductCategoryMatcherTest {

    private final String FALLBACK = "OUTROS";
    private final List<Keyword> KEYWORDS = List.of(
            new Keyword("arroz", "ALIMENTOS"),
            new Keyword("detergente", "LIMPEZA"),
            new Keyword("leite", "ALIMENTOS")
    );

    @Test
    @DisplayName("Deve retornar a categoria correta quando o nome do produto contém a keyword")
    void shouldReturnCategoryWhenKeywordMatches() {
        String result = ProductCategoryMatcher.resolveCategory("Arroz Agulha", KEYWORDS, FALLBACK);
        assertThat(result).isEqualTo("ALIMENTOS");
    }

    @Test
    @DisplayName("Deve ser insensível a maiúsculas/minúsculas (case-insensitive)")
    void shouldBeCaseInsensitive() {
        String result = ProductCategoryMatcher.resolveCategory("DETERGENTE LÍQUIDO", KEYWORDS, FALLBACK);
        assertThat(result).isEqualTo("LIMPEZA");
    }

    @Test
    @DisplayName("Deve retornar a categoria fallback quando nenhuma keyword for encontrada")
    void shouldReturnFallbackWhenNoMatch() {
        String result = ProductCategoryMatcher.resolveCategory("Pilhas AAA", KEYWORDS, FALLBACK);
        assertThat(result).isEqualTo("OUTROS");
    }

    @Test
    @DisplayName("Deve retornar a primeira keyword encontrada em caso de múltiplas correspondências")
    void shouldReturnFirstMatchWhenMultipleKeywordsExist() {
        // "arroz" vem antes de "leite" na nossa lista KEYWORDS
        String result = ProductCategoryMatcher.resolveCategory("Arroz com leite", KEYWORDS, FALLBACK);
        assertThat(result).isEqualTo("ALIMENTOS");
    }
}