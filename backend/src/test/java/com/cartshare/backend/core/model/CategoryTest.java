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
        String id = "LIMPEZA";
        String name = "Limpeza";
        String classification = "CASA";
        int priority = 4;

        // Act
        Category category = new Category(id, name, classification, priority);

        // Assert
        assertAll("Validação das propriedades do Record Category",
                () -> assertEquals(id, category.id(), "O ID deve ser mantido em CAPS"),
                () -> assertEquals(name, category.name(), "O nome de exibição deve estar correto"),
                () -> assertEquals(classification, category.classification(), "A classificação deve estar correta"),
                () -> assertEquals(priority, category.priority(), "A prioridade deve ser o valor definido")
        );
    }

    @Test
    @DisplayName("Deve garantir a igualdade entre dois records com os mesmos valores")
    void shouldEnsureEqualityBetweenIdenticalRecords() {
        // Arrange & Act
        Category cat1 = new Category("ALIMENTOS", "Alimentação", "MERCEARIA", 1);
        Category cat2 = new Category("ALIMENTOS", "Alimentação", "MERCEARIA", 1);

        // Assert
        assertAll("Igualdade de Records",
                () -> assertEquals(cat1, cat2, "Records com valores idênticos devem ser iguais (equals)"),
                () -> assertEquals(cat1.hashCode(), cat2.hashCode(), "HashCodes devem ser idênticos"),
                () -> assertNotSame(cat1, cat2, "Devem ser instâncias diferentes na memória")
        );
    }

    @Test
    @DisplayName("Deve refletir a imutabilidade do Record")
    void shouldBeImmutable() {
        // Arrange
        Category cat = new Category("SAUDE", "Saúde", "CUIDADOS", 3);

        // Assert
        // Em Java Records, os componentes são acessados via métodos que não permitem alteração.
        // A verificação abaixo garante que os valores permanecem os mesmos após a criação.
        assertAll("Imutabilidade",
                () -> assertEquals("SAUDE", cat.id()),
                () -> assertEquals("Saúde", cat.name()),
                () -> assertEquals("CUIDADOS", cat.classification()),
                () -> assertEquals(3, cat.priority())
        );

        // Nota: Não existem métodos 'setID' ou 'setPriority' em Records compilados.
    }
}