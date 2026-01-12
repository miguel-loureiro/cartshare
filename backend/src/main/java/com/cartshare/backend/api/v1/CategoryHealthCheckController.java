package com.cartshare.backend.api.v1;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.infrastructure.firestore.CategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CategoryHealthCheckController {

    private final CategoryRepository categoryRepository;

    public CategoryHealthCheckController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/test-category")
    public String testCategory() {
        try {
            Category cleaningCategory = new Category(
                    UUID.randomUUID().toString(),
                    "Limpeza",
                    "Casa",
                    Arrays.asList("detergente", "lixívia", "amaciador")
            );

            // Using .block() to wait for the reactive result for this test
            Category saved = categoryRepository.save(cleaningCategory).block();

            assert saved != null;
            return "✅ Firestore Success! Category '"+ saved.name() +"' saved with ID: " + saved.id();
        } catch (Exception e) {
            return "❌ Firestore Connection Failed: " + e.getMessage();
        }
    }
}
