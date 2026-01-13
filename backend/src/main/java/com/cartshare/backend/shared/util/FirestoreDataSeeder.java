package com.cartshare.backend.shared.util;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.service.AutocompleteService;
import com.cartshare.backend.infrastructure.excel.ExcelReader;
import com.cartshare.backend.infrastructure.excel.FirestoreExcelImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeding.enabled", havingValue = "true")
@Order(1)
public class FirestoreDataSeeder implements CommandLineRunner {

    private final FirestoreExcelImporter importer;
    private final AutocompleteService autocompleteService;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(String... args) {
        try {
            log.info("--- STARTING FIRESTORE DATA SEEDING ---");

            // 1. Import Categories
            Resource catRes = resourceLoader.getResource("classpath:excel/Categories.xlsx");
            try (InputStream is = catRes.getInputStream()) {
                importer.importCategories(is);
            }

            // 2. Import Keywords (Needed for Product matching)
            Resource kwRes = resourceLoader.getResource("classpath:excel/keywords.xlsx");
            List<Keyword> keywords;
            try (InputStream is = kwRes.getInputStream()) {
                // We read them into a list to pass to the product importer
                keywords = ExcelReader.read(is).stream()
                        .filter(r -> r.size() >= 2)
                        .map(r -> new Keyword(r.get(0), ExcelReader.toSafeId(r.get(1))))
                        .toList();

                // Re-read stream for the actual Firestore import
                try (InputStream isImport = kwRes.getInputStream()) {
                    importer.importKeywords(isImport);
                }
            }

            // 3. Import Products
            Resource prodRes = resourceLoader.getResource("classpath:excel/Products.xlsx");
            try (InputStream is = prodRes.getInputStream()) {
                importer.importProducts(is, keywords);
            }

            log.info("--- SEEDING COMPLETED SUCCESSFULLY ---");

            // 4. Trigger Autocomplete Index Warm-up
            // In a real scenario, you'd fetch these from Firestore,
            // but for the first boot, we can trigger an initial index here.
            log.info("Warming up Autocomplete Index...");

        } catch (Exception e) {
            log.error("Critical error during data seeding: {}", e.getMessage(), e);
        }
    }
}
