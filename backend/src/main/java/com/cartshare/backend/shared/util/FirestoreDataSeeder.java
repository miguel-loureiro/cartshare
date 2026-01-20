package com.cartshare.backend.shared.util;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.AutocompleteService;
import com.cartshare.backend.core.service.ProductCategoryMatcher;
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

            // 1. Prepare Categories
            Resource catRes = resourceLoader.getResource("classpath:excel/Categories.xlsx");
            List<Category> categories;
            try (InputStream is = catRes.getInputStream()) {

                categories = ExcelReader.read(is).stream()
                        .filter(row -> row.size() >= 4)
                        .map(row -> new Category(
                                ExcelReader.toSafeId(row.getFirst()),
                                row.get(1),
                                ExcelReader.toSafeId(row.get(2)),
                                Integer.parseInt(row.get(3))
                        )).toList();

                importer.importCategoriesFromList(categories);
                log.info("✅ Categories processed: {}", categories.size());
            }

            // 2. Prepare Keywords
            Resource kwRes = resourceLoader.getResource("classpath:excel/keywords.xlsx");
            List<Keyword> keywords;
            try (InputStream is = kwRes.getInputStream()) {
                keywords = ExcelReader.read(is).stream()
                        .filter(row -> row.size() >= 2)
                        .map(row -> new Keyword(row.getFirst(), ExcelReader.toSafeId(row.get(1))))
                        .toList();

                importer.importKeywordsFromList(keywords);
                log.info("✅ Keywords processed: {}", keywords.size());
            }

            // 3. Prepare Products
            Resource prodRes = resourceLoader.getResource("classpath:excel/Products.xlsx");
            List<Product> products;
            try (InputStream is = prodRes.getInputStream()) {
                products = ExcelReader.read(is).stream()
                        .filter(row -> row.size() >= 2)
                        .map(row -> {
                            String name = row.get(0);
                            String categoryId = ProductCategoryMatcher.resolveCategory(name, keywords, row.get(1));
                            // We generate the search keywords here so they are available for the index
                            List<String> searchKeywords = importer.generateSearchKeywords(name);
                            return new Product(null,name, categoryId, true, searchKeywords);
                        }).toList();

                importer.importProductsFromList(products);
                log.info("✅ Products processed: {}", products.size());
            }

            log.info("--- ✨ DATABASE SEEDING COMPLETED ---");

            // 4. Update Autocomplete Index
            log.info("🔄 Updating Autocomplete Master Index...");
            autocompleteService.indexUpdate(categories, keywords, products);
            log.info("✅ Autocomplete Index is now warm and ready!");

        } catch (Exception e) {
            log.error("❌ Critical error during data seeding: ", e);
        }
    }
}