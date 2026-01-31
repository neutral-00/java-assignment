package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ProductTest {

    @Test
    public void testProductConstructorAndSetters() {
        // Test the parameterized constructor (Targets red line in JaCoCo)
        Product product = new Product("TEST-PRODUCT");

        // Test fields
        product.description = "A test product description";
        product.price = new BigDecimal("19.99");
        product.stock = 50;

        assertEquals("TEST-PRODUCT", product.name);
        assertEquals("A test product description", product.description);
        assertEquals(new BigDecimal("19.99"), product.price);
        assertEquals(50, product.stock);
    }

    @Test
    public void testDefaultConstructor() {
        // Targets the default constructor coverage
        Product product = new Product();
        assertNotNull(product);
        assertNull(product.name);
    }
}