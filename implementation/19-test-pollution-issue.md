# Test Pollution Issue

I was getting some test pollution issue as shown below:

```text
[ERROR] Failures: 
[ERROR]   ProductResourceTest.testGetAllProducts:22 1 expectation failed.
JSON path name doesn't match.                                                                                                                                   
Expected: iterable with items ["TONSTAD", "KALLAX", "BESTÅ"] in any order                                                                                       
  Actual: <[KALLAX-REVISED, MALM]>                                                                                                                              
                                                                                                                                                                
[ERROR]   ProductResourceTest.testGetSingleProduct_Success:31 1 expectation failed.
Expected status code <200> but was <404>.                                                                                                                       
                                                                                                                                                                
[INFO] 
[ERROR] Tests run: 45, Failures: 2, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

It looks like your `ProductResourceTest` is running into a classic "test pollution" issue. The actual results show `[KALLAX-REVISED, MALM]`, which indicates that your `testUpdateProduct` and `testCreateProduct` methods are modifying the same database state used by `testGetAllProducts`.

Since your `application.properties` uses `drop-and-create` and loads `import.sql` once at startup, the tests are currently sharing a single database instance.

### Why the Tests are Failing

* **Order of Execution:** JUnit does not guarantee that `testGetAllProducts` runs first. If the update test runs before it, the name "KALLAX" changes to "KALLAX-REVISED".
* **Persistent State:** Data created in one `@Test` persists for the next because Quarkus tests do not automatically rollback transactions unless configured specifically.

---

### Refined Test Strategy

To fix this, we should make each test **independent** by creating unique data or cleaning up.

#### 1. Use Unique Values for Validations

Instead of checking for the entire list (which is fragile), check for a specific seeded item that you haven't modified.

#### 2. Reset Database State (Recommended)

You can use a `@BeforeEach` method to ensure a known state, or simply refine the assertions to be less sensitive to other tests.

```java
@QuarkusTest
public class ProductResourceTest {

    @Test
    public void testGetAllProducts() {
        given()
          .when().get("/product")
          .then()
             .statusCode(200)
             // Use "hasItem" instead of "containsInAnyOrder" to be 
             // resilient to extra products created by other tests
             .body("name", hasItem("TONSTAD")); 
    }

    @Test
    public void testCreateProduct() {
        // Use a unique name to avoid conflicts
        Product newProduct = new Product("UNIQUE-PROD-99");
        newProduct.price = new BigDecimal("10.00");
        newProduct.stock = 5;

        given()
          .contentType(ContentType.JSON)
          .body(newProduct)
          .when().post("/product")
          .then()
             .statusCode(201) // Corrected from 200 to match ProductResource.java logic
             .body("name", is("UNIQUE-PROD-99"));
    }

    @Test
    public void testGetSingleProduct_Success() {
        // Use an ID that is likely to exist from import.sql
        // If ID 1 was deleted by another test, this will fail with 404
        given()
          .when().get("/product/1")
          .then()
             .statusCode(200)
             .body("name", is("TONSTAD"));
    }
}

```

### Addressing the "404" in your Screenshot

Your screenshot shows `testGetSingleProduct_Success` failed with a `404`. This happens if your `testDeleteProduct` ran first and deleted the item with ID 1.


To make your `ProductResourceTest` suite independent and reliable, you can use a **cleanup strategy** to reset the database state before each test. In Quarkus, the most efficient way to do this without a complete container restart is to use a `@BeforeEach` method that clears the tables.

Since your `application.properties` is set to `drop-and-create` and uses `import.sql`, clearing the tables and re-executing the inserts will give you a "Clean Slate" for every single test method.

### 1. The Strategy: Database Reset

By injecting the `ProductRepository` and using a `@BeforeEach` block, we ensure that:

* `testGetAllProducts` always sees exactly the 3 items from `import.sql`.
* `testGetSingleProduct_Success` always finds ID 1 because it was just re-inserted.
* The `404` error you saw in your screenshot disappears because no previous test's `DELETE` operation persists.

### 2. Implementation: Refined ProductResourceTest.java

```java
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
}

```

### Why this fixes your issue:

1. **Dynamic IDs:** Instead of hardcoding `1`, `2`, or `3`, we fetch the ID from the database using the repository. This is safer because Hibernate sequences might increment across tests even if you delete the records.
2. **Explicit Status Codes:** I updated the `POST` assertion to `201` to match your `ProductResource.create` implementation.
3. **Isolation:** Even if `testDeleteProduct` runs before `testGetAllProducts`, the `setup()` method will recreate the deleted items.
