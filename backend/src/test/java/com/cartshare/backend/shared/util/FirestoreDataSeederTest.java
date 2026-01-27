package com.cartshare.backend.shared.util;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.AutocompleteService;
import com.cartshare.backend.infrastructure.excel.ExcelReader;
import com.cartshare.backend.infrastructure.excel.FirestoreExcelImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirestoreDataSeederTest {
    @Mock private FirestoreExcelImporter importer;
    @Mock private AutocompleteService autocompleteService;
    @Mock private ResourceLoader resourceLoader;
    @Mock private Resource kwResource;
    @Mock private Resource prodResource;
    @InjectMocks
    private FirestoreDataSeeder firestoreDataSeeder;

    @Test
    @DisplayName("run: Successful seeding should process Keywords and Products")
    void run_SuccessfulSeeding_ShouldInvokeRelevantServices() throws Exception {
        // --- GIVEN ---
        // Mock keyword resource
        when(resourceLoader.getResource("classpath:excel/keywords.xlsx")).thenReturn(kwResource);
        when(kwResource.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));

        // Mock product resource
        when(resourceLoader.getResource("classpath:excel/Products.xlsx")).thenReturn(prodResource);
        when(prodResource.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(new byte[0]));

        // Prepare test data - ExcelReader will be called twice
        List<List<String>> kwData = List.of(
                List.of("Keyword"),  // Header (will be skipped)
                List.of("Arroz")     // Data row
        );
        List<List<String>> prodData = List.of(
                List.of("Product Name"),  // Header (will be skipped)
                List.of("Feijão")        // Data row
        );

        try (MockedStatic<ExcelReader> excelReader = mockStatic(ExcelReader.class)) {
            // Mock ExcelReader.read() to return different data on consecutive calls
            excelReader.when(() -> ExcelReader.read(any()))
                    .thenReturn(kwData)    // 1st call: Keywords
                    .thenReturn(prodData)  // 2nd call: Products
                    .thenReturn(List.of()); // 3rd+ call: Safety net

            // Mock keyword generation for product search keywords
            when(importer.generateSearchKeywords(anyString())).thenReturn(List.of("test"));

            // Mock the import methods (they return void)
            doNothing().when(importer).importKeywordsFromList(any());
            doNothing().when(importer).importProductsFromList(any());

            // --- WHEN ---
            firestoreDataSeeder.run();

            // --- THEN ---
            // Verify imports were called with correct data
            ArgumentCaptor<List<Keyword>> keywordCaptor = ArgumentCaptor.forClass(List.class);
            verify(importer, times(1)).importKeywordsFromList(keywordCaptor.capture());
            assertThat(keywordCaptor.getValue()).hasSize(1);
            assertThat(keywordCaptor.getValue().get(0).keyword()).isEqualTo("Arroz");

            ArgumentCaptor<List<Product>> productCaptor = ArgumentCaptor.forClass(List.class);
            verify(importer, times(1)).importProductsFromList(productCaptor.capture());
            assertThat(productCaptor.getValue()).hasSize(1);

            // Verify search keyword generation was called
            verify(importer, times(1)).generateSearchKeywords("Feijão");
        }
    }

    @Test
    @DisplayName("run: When exception occurs, should catch and log error")
    void run_WhenExceptionOccurs_ShouldCatchAndLog() throws Exception {
        // --- GIVEN ---
        when(resourceLoader.getResource("classpath:excel/keywords.xlsx"))
                .thenThrow(new RuntimeException("Seed file missing"));

        // --- WHEN/THEN ---
        // Should not throw exception (it's caught internally)
        assertDoesNotThrow(() -> firestoreDataSeeder.run());
    }
}