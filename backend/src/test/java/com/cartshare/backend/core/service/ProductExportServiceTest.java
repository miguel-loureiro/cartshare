package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductExportServiceTest {

    @Mock
    private ProductContributionService productContributionService;

    private ProductExportService exportService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        exportService = new ProductExportService(productContributionService, objectMapper);
    }

    @Test
    @DisplayName("exportProductsToData: Should correctly serialize the product list")
    void exportProductsToData_Success() throws Exception {
        // Arrange
        Product p1 = Product.of("p1", "Bread", true, List.of("bread", "pao"));
        when(productContributionService.getAllProducts()).thenReturn(List.of(p1));

        // Act
        byte[] result = exportService.exportProductsToData();

        // Assert
        assertThat(result).isNotEmpty();
        String jsonString = new String(result);

        // Check for specific JSON content and "pretty print" structure
        assertThat(jsonString).contains("\"productName\" : \"Bread\"");
        assertThat(jsonString).contains("\"isOfficial\" : true");
        assertThat(jsonString).contains("\"pao\"");
    }
}