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
        String categoryId = "ALIMENTOS";
        boolean isOfficial = true;
        List<String> keywords = List.of("abacaxi", "perola");

        // Act
        Product product = new Product(name, categoryId, isOfficial, keywords);

        // Assert
        assertAll("Validação do Produto Oficial",
                () -> assertEquals(name, product.productName()),
                () -> assertEquals(categoryId, product.categoryId()),
                () -> assertTrue(product.isOfficial(), "Produto vindo do Excel deve ser oficial"),
                () -> assertEquals(2, product.searchKeywords().size()),
                () -> assertTrue(product.searchKeywords().contains("abacaxi"))
        );
    }

    @Test
    @DisplayName("Deve criar um produto de usuário (pendente) com categoria padrão")
    void shouldCreateUserContributedProduct() {
        // Arrange
        String name = "Dragon Fruit";
        String categoryId = "OUTROS"; // Padrão para novas entradas de usuário
        boolean isOfficial = false;   // Aguardando revisão no Dashboard
        List<String> keywords = List.of("dragon", "fruit");

        // Act
        Product product = new Product(name, categoryId, isOfficial, keywords);

        // Assert
        assertAll("Validação do Produto de Usuário",
                () -> assertFalse(product.isOfficial(), "Entradas de usuários não devem ser oficiais por padrão"),
                () -> assertEquals("OUTROS", product.categoryId(), "Deve usar a categoria padrão configurada")
        );
    }

    @Test
    @DisplayName("Deve garantir imutabilidade da lista de keywords")
    void shouldEnsureKeywordsAreImmutable() {
        // Arrange
        Product product = new Product("Leite", "BEBIDAS", true, List.of("leite"));

        // Assert
        // A lista retornada por List.of() já é imutável
        assertThrows(UnsupportedOperationException.class, () -> {
            product.searchKeywords().add("novo");
        }, "A lista de keywords não deve permitir modificação direta");
    }
}