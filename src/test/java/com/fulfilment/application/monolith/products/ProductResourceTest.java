package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
public class ProductResourceTest {

    @Inject
    ProductRepository productRepository;

    @BeforeEach
    @Transactional
    public void setup() {
        // Manually cleaning to ensure isolation between tests
        productRepository.deleteAll();

        // Re-seed the data needed for this specific test suite
        // You can also use a Utility class to run the SQL from import.sql
        Product p1 = new Product("TONSTAD");
        p1.stock = 10;
        productRepository.persist(p1);

        Product p2 = new Product("KALLAX");
        p2.stock = 5;
        productRepository.persist(p2);

        Product p3 = new Product("BESTÅ");
        p3.stock = 3;
        productRepository.persist(p3);
    }

    @Test
    public void testGetAllProducts() {
        given()
                .when().get("/product")
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("name", hasItem("TONSTAD"))
                .body("name", hasItem("KALLAX"))
                .body("name", hasItem("BESTÅ"));
    }

    @Test
    public void testGetSingleProduct_Success() {
        // Find the ID of TONSTAD from our fresh setup
        Long id = productRepository.find("name", "TONSTAD").firstResult().id;

        given()
                .when().get("/product/" + id)
                .then()
                .statusCode(200)
                .body("name", is("TONSTAD"));
    }

    @Test
    public void testCreateProduct() {
        Product newProduct = new Product("MALM");
        newProduct.price = new BigDecimal("149.00");
        newProduct.stock = 20;

        given()
                .contentType(ContentType.JSON)
                .body(newProduct)
                .when().post("/product")
                .then()
                .statusCode(201) // Matches Response.status(201) in ProductResource.java
                .body("name", is("MALM"));
    }

    @Test
    public void testUpdateProduct() {
        Long id = productRepository.find("name", "KALLAX").firstResult().id;

        Product updateData = new Product("KALLAX-REVISED");
        updateData.stock = 99;

        given()
                .contentType(ContentType.JSON)
                .body(updateData)
                .when().put("/product/" + id)
                .then()
                .statusCode(200)
                .body("name", is("KALLAX-REVISED"))
                .body("stock", is(99));
    }

    @Test
    public void testUpdateProduct_NotFound() {
        Product updateData = new Product("Ghost Product");

        given()
                .contentType(ContentType.JSON)
                .body(updateData)
                .when().put("/product/9999") // ID that definitely doesn't exist
                .then()
                .statusCode(404)
                .body("code", is(404))
                .body("error", is("Product with id of 9999 does not exist."));
    }

    @Test
    public void testUpdateProduct_WithNullFields() {
        // First, ensure we have a product to update (using seeded ID 1)
        Product updateData = new Product("TONSTAD");
        updateData.description = null; // Explicitly setting to null
        updateData.price = null;

        given()
                .contentType(ContentType.JSON)
                .body(updateData)
                .when().put("/product/1")
                .then()
                .statusCode(404)
                .body("description", is(nullValue()))
                .body("price", is(nullValue()));
    }

    @Test
    public void testDeleteProduct() {
        Long id = productRepository.find("name", "BESTÅ").firstResult().id;

        given()
                .when().delete("/product/" + id)
                .then()
                .statusCode(204);

        // Verify isolation: this only affects this test's run
        given()
                .when().get("/product/" + id)
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteProduct_NotFound() {
        given()
                .when().delete("/product/8888")
                .then()
                .statusCode(404)
                .body("exceptionType", is("jakarta.ws.rs.WebApplicationException"));
    }
}