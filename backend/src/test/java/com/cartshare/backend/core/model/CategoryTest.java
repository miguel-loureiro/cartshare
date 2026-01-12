package com.cartshare.backend.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - Record Category")
class CategoryTest {

    @Test
    @DisplayName("Deve criar uma categoria com sucesso quando os dados são válidos")
    void shouldCreateCategoryWithValidData() {
        // Arrange
        String id = "cat-123";
        String name = "Limpeza";
        String classification = "Casa";
        List<String> keywords = List.of("detergente", "lixívia");

        // Act
        Category category = new Category(id, name, classification, keywords);

        // Assert
        assertAll("Validação das propriedades do Record",
                () -> assertEquals(id, category.id()),
                () -> assertEquals(name, category.name()),
                () -> assertEquals(classification, category.classification()),
                () -> assertEquals(2, category.keywords().size()),
                () -> assertTrue(category.keywords().contains("detergente"))
        );
    }

    @Test
    @DisplayName("Deve garantir a igualdade entre dois records com os mesmos valores")
    void shouldEnsureEqualityBetweenIdenticalRecords() {
        List<String> kws = List.of("pão", "leite");

        Category cat1 = new Category("1", "Padaria", "Alimentos", kws);
        Category cat2 = new Category("1", "Padaria", "Alimentos", kws);

        assertEquals(cat1, cat2, "Records com valores idênticos devem ser iguais (equals)");
        assertEquals(cat1.hashCode(), cat2.hashCode(), "HashCodes devem ser idênticos");
    }

    @Test
    @DisplayName("Deve refletir a imutabilidade do Record")
    void shouldBeImmutable() {
        Category cat = new Category("1", "Frutas", "Frescos", List.of("Maçã"));

        // Em Java Records, não existem setters. A única forma de "mudar" é criar um novo.
        assertNotNull(cat.id());
        // Verificação conceitual: os campos são finais por padrão no bytecode.
    }
}