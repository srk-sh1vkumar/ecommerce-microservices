package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(String category);
    Page<Product> findByCategory(String category, Pageable pageable);

    List<Product> findByNameContainingIgnoreCase(String name);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Product> findByStockQuantityGreaterThan(Integer quantity);
    Page<Product> findByStockQuantityGreaterThan(Integer quantity, Pageable pageable);

    Page<Product> findAll(Pageable pageable);
}