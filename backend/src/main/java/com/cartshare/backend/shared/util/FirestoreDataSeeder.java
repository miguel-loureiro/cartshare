package com.cartshare.backend.shared.util;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
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
            log.info("--- 🚀 STARTING FIRESTORE DATA SEEDING ---");
            // 1. Keywords - Skip header row
            Resource kwRes = resourceLoader.getResource("classpath:excel/keywords.xlsx");
            try (InputStream is = kwRes.getInputStream()) {
                List<Keyword> keywords = ExcelReader.read(is).stream()
                        .skip(1) // Skip "Keyword" header
                        .filter(row -> !row.isEmpty())
                        .map(row -> new Keyword(row.getFirst()))
                        .toList();
                importer.importKeywordsFromList(keywords);
                log.info("✅ Keywords processed: {}", keywords.size());
            }
            // 2. Products - Skip header row
            Resource prodRes = resourceLoader.getResource("classpath:excel/Products.xlsx");
            try (InputStream is = prodRes.getInputStream()) {
                List<Product> products = ExcelReader.read(is).stream()
                        .skip(1) // Skip "Product Name" header
                        .filter(row -> !row.isEmpty())
                        .map(row -> {
                            String name = row.getFirst();
                            return Product.createOfficial(name, importer.generateSearchKeywords(name));
                        }).toList();
                importer.importProductsFromList(products);
                log.info("✅ Products processed: {}", products.size());
            }
            // ... rest of the indexing logic
        } catch (Exception e) {
            log.error("❌ Critical error during data seeding: ", e);
        }
    }
}