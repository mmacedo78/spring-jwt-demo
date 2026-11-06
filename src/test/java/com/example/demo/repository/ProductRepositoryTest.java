package com.example.demo.repository;

import com.example.demo.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    private Product product(String name, String price) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        return p;
    }

    @Test
    void saveGeneratesId() {
        Product saved = repository.save(product("Teclado", "199.90"));

        assertNotNull(saved.getId());
    }

    @Test
    void findAllReturnsEverySavedProduct() {
        repository.save(product("A", "1.00"));
        repository.save(product("B", "2.00"));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteRemovesProduct() {
        Product saved = repository.save(product("Temp", "3.00"));

        repository.deleteById(saved.getId());

        assertFalse(repository.existsById(saved.getId()));
    }
}
