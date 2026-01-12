package com.cartshare.backend.infrastructure.firestore;

import com.cartshare.backend.core.model.Category;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends FirestoreReactiveRepository<Category> {
}
