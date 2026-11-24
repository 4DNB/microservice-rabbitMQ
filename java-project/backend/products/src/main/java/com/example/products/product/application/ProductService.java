package com.example.products.product.application;

import com.example.products.product.persistence.ProductEntity;
import com.example.products.product.persistence.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductEntity create(String name, String description, BigDecimal price, Integer stock, String imageUrl) {
        return repository.save(new ProductEntity(name, description, price, stock, imageUrl));
    }

    public List<ProductEntity> findAll() {
        return repository.findAll();
    }

    public ProductEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    public ProductEntity update(Long id, String name, String description, BigDecimal price, Integer stock, String imageUrl) {
        ProductEntity product = findById(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setImageUrl(imageUrl);
        return repository.save(product);
    }

    public void delete(Long id) {
        ProductEntity product = findById(id);
        repository.delete(product);
    }
}
