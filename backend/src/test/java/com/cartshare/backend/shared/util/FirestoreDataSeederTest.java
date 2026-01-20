package com.cartshare.backend.shared.util;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.AutocompleteService;
import com.cartshare.backend.core.service.ProductCategoryMatcher;
import com.cartshare.backend.infrastructure.excel.ExcelReader;
import com.cartshare.backend.infrastructure.excel.FirestoreExcelImporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirestoreDataSeederTest {

    @Mock private FirestoreExcelImporter importer;
    @Mock private AutocompleteService autocompleteService;
    @Mock private ResourceLoader resourceLoader;
    @Mock private Resource resource;

    @InjectMocks
    private FirestoreDataSeeder firestoreDataSeeder;

    @Test
    void run_SuccessfulSeeding_ShouldInvokeAllServices() throws Exception {
        // --- GIVEN ---
        // Mock Resource Loading
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        InputStream emptyIs = new ByteArrayInputStream(new byte[0]);
        when(resource.getInputStream()).thenReturn(emptyIs);

        // Prepare Dummy Data
        List<List<String>> catData = List.of(List.of("id1", "Name", "parent", "1"));
        List<List<String>> kwData = List.of(List.of("keyword", "catId"));
        List<List<String>> prodData = List.of(List.of("Product Name", "RawCat"));

        // We use MockedStatic to control the utility classes
        try (MockedStatic<ExcelReader> excelReader = mockStatic(ExcelReader.class);
             MockedStatic<ProductCategoryMatcher> matcher = mockStatic(ProductCategoryMatcher.class)) {

            excelReader.when(() -> ExcelReader.read(any(InputStream.class)))
                    .thenReturn(catData)  // 1st call (Categories)
                    .thenReturn(kwData)   // 2nd call (Keywords)
                    .thenReturn(prodData); // 3rd call (Products)

            excelReader.when(() -> ExcelReader.toSafeId(anyString())).thenAnswer(inv -> inv.getArgument(0));
            matcher.when(() -> ProductCategoryMatcher.resolveCategory(anyString(), anyList(), anyString()))
                    .thenReturn("resolved-cat-id");

            when(importer.generateSearchKeywords(anyString())).thenReturn(List.of("prod", "name"));

            // --- WHEN ---
            firestoreDataSeeder.run();

            // --- THEN ---
            // Verify DB imports were called
            verify(importer, times(1)).importCategoriesFromList(anyList());
            verify(importer, times(1)).importKeywordsFromList(anyList());
            verify(importer, times(1)).importProductsFromList(anyList());

            // Verify Autocomplete index was updated
            verify(autocompleteService, times(1)).indexUpdate(anyList(), anyList(), anyList());
        }
    }

    @Test
    void run_WhenExceptionOccurs_ShouldCatchAndLog() throws Exception {
        // --- GIVEN ---
        when(resourceLoader.getResource(anyString())).thenThrow(new RuntimeException("File not found"));

        // --- WHEN ---
        // Should not throw exception due to try-catch in 'run'
        firestoreDataSeeder.run();

        // --- THEN ---
        verifyNoInteractions(importer);
        verifyNoInteractions(autocompleteService);
    }
}