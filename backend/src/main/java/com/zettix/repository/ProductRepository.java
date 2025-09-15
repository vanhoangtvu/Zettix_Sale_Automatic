package com.zettix.repository;

import com.zettix.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByIsActiveTrue();
    
    List<Product> findByProductType(Product.ProductType productType);
    
    List<Product> findByCategory(String category);
    
    List<Product> findByIsActiveTrueAndProductType(Product.ProductType productType);
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.availableQuantity > 0")
    List<Product> findAvailableProducts();
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.availableQuantity > 0 AND p.productType = :productType")
    List<Product> findAvailableProductsByType(@Param("productType") Product.ProductType productType);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    Long countActiveProducts();
    
    @Query("SELECT SUM(p.soldQuantity) FROM Product p WHERE p.isActive = true")
    Long getTotalSoldQuantity();
}
