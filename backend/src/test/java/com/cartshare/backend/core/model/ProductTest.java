package com.cartshare.backend.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - Record Product")
class ProductTest {

    @Test
    @DisplayName("Deve criar um produto oficial com palavras-chave de busca")
    void shouldCreateOfficialProductWithSearchKeywords() {
        // Arrange
        String name = "Abacaxi Pérola";
        boolean isOfficial = true;
        List<String> keywords = List.of("abacaxi", "perola");

        // Act
        Product product = new Product(null,name,  isOfficial, keywords);

        // Assert
        assertAll("Validação do Produto Oficial",
                () -> assertEquals(name, product.productName()),
                () -> assertTrue(product.isOfficial(), "Produto vindo do Excel deve ser oficial"),
                () -> assertEquals(2, product.searchKeywords().size()),
                () -> assertTrue(product.searchKeywords().contains("abacaxi"))
        );
    }

    @Test
    @DisplayName("Deve garantir imutabilidade da lista de keywords")
    void shouldEnsureKeywordsAreImmutable() {
        // Arrange
        Product product = new Product(null,"Leite", true, List.of("leite"));

        // Assert
        // A lista retornada por List.of() já é imutável
        assertThrows(UnsupportedOperationException.class, () -> {
            product.searchKeywords().add("novo");
        }, "A lista de keywords não deve permitir modificação direta");
    }
}