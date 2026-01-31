Testing the `ProductResource` involves verifying the REST endpoints for CRUD operations. Since this resource interacts with the database via `ProductRepository`, we will use **RestAssured** for black-box testing. This ensures that the JSON serialization, status codes, and the `ErrorMapper` logic all work together seamlessly.

### ProductResourceTest Scenarios

We will cover the full lifecycle of a product using the data already present in your `import.sql` (like 'TONSTAD') and adding new ones.

* **`testGetProducts`**: Verify the `GET /product` endpoint returns the seeded products.
* **`testCreateProduct`**: Ensure `POST /product` correctly saves a new entity and returns a 200/201 status.
* **`testUpdateProduct`**: Confirm `PUT /product/{id}` updates an existing record.
* **`testDeleteProduct`**: Verify `DELETE /product/{id}` removes the product and returns a 204.
* **`testErrorMapping`**: Trigger a `404` by requesting a non-existent ID to verify the `ErrorMapper` output.

---

### Implementation: ProductResourceTest.java

```java
package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
public class ProductResourceTest {

    @Test
    public void testGetAllProducts() {
        given()
          .when().get("/product")
          .then()
             .statusCode(200)
             .body("name", containsInAnyOrder("TONSTAD", "KALLAX", "BESTÅ"));
    }

    @Test
    public void testGetSingleProduct_Success() {
        // ID 1 is 'TONSTAD' from import.sql
        given()
          .when().get("/product/1")
          .then()
             .statusCode(200)
             .body("name", is("TONSTAD"))
             .body("stock", is(10));
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
             .statusCode(200) // ProductResource returns the entity directly
             .body("name", is("MALM"))
             .body("stock", is(20));
    }

    @Test
    public void testUpdateProduct() {
        Product updateData = new Product("KALLAX-REVISED");
        updateData.stock = 99;

        given()
          .contentType(ContentType.JSON)
          .body(updateData)
          .when().put("/product/2")
          .then()
             .statusCode(200)
             .body("name", is("KALLAX-REVISED"))
             .body("stock", is(99));
    }

    @Test
    public void testDeleteProduct() {
        // Delete 'BESTÅ' (ID 3)
        given()
          .when().delete("/product/3")
          .then()
             .statusCode(204);

        // Verify it is gone
        given()
          .when().get("/product/3")
          .then()
             .statusCode(404);
    }

    @Test
    public void testErrorMapper_NotFound() {
        given()
          .when().get("/product/999")
          .then()
             .statusCode(404)
             .body("code", is(404))
             .body("error", is("Product with id of 999 does not exist."));
    }
}

```

### Why use RestAssured here?

1. **Validates JSON Handling:** It ensures that your `ObjectMapper` correctly handles `BigDecimal` and other fields.
2. **Verifies the Provider:** It tests the `@Provider` logic of your `ErrorMapper` in a real HTTP context, ensuring the `ObjectNode` is constructed as expected.
3. **End-to-End Safety:** Since your properties use `drop-and-create`, these tests run against a fresh schema, making them highly reliable.

This [RestAssured API Testing Tutorial](https://www.youtube.com/watch?v=QDp7EQga3ME) provides a clear walkthrough of performing PUT and DELETE operations, which is directly relevant to testing your `ProductResource` endpoints.

Since we've already handled the "happy path" and basic persistence, focusing on **null scenarios** is the best way to harden the `ProductResource`. Looking at your code, the `put` and `delete` methods are particularly vulnerable if they don't find the requested ID.

Here are the specific null/edge cases we should cover to ensure the `ErrorMapper` and resource logic are fully exercised:

### 1. Update Non-Existent Product (Null Result from Find)

* **Scenario:** Calling `PUT /product/{id}` with an ID that doesn't exist.
* **Goal:** Verify that the `productRepository.findById(id)` returns null, which should trigger the `WebApplicationException` with a 404 status.

### 2. Delete Non-Existent Product

* **Scenario:** Calling `DELETE /product/{id}` for a missing ID.
* **Goal:** Ensure the system doesn't crash on a `NullPointerException` but instead throws the handled 404 exception.

### 3. Update with Null Payload Fields

* **Scenario:** Sending a valid JSON body to `PUT /product/{id}` but with null values for fields like `description` or `price`.
* **Goal:** Verify how the repository handles the assignment of nulls to the entity.

---

### Implementation: ProductResourceNullScenarioTest.java

We can add these to your existing suite or create a specific class for "Defensive Testing":

```java
package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
public class ProductResourceNullScenarioTest {

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
    public void testDeleteProduct_NotFound() {
        given()
          .when().delete("/product/8888")
          .then()
             .statusCode(404)
             .body("exceptionType", is("jakarta.ws.rs.WebApplicationException"));
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
             .statusCode(200)
             .body("description", is(nullValue()))
             .body("price", is(nullValue()));
    }
}

```

### Why these tests matter for your coverage

Looking at the `ProductResource.java` code you provided:

* The `if (entity == null)` blocks in both `update` and `delete` methods are likely marked in **yellow or red** in your JaCoCo report because the "true" branch (where the exception is thrown) hasn't been hit yet.
* By running these tests, you force the execution into those `throw new WebApplicationException(...)` lines, which will significantly increase your **Branch Coverage**.
