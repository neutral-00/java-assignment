Testing `WarehouseResourceImpl` is a bit more complex because it acts as an adapter between the generated OpenAPI beans and your domain logic. It injects several "Operation" ports (`CreateWarehouseOperation`, `ReplaceWarehouseOperation`, etc.), so we need to ensure we're testing the mapping logic and error handling.

Here are the scenarios we need to cover for `WarehouseResourceImpl`:

---

## 1. Mapping Integrity (Domain ↔ API)

The resource has two private helper methods: `toDomainModel` and `toWarehouseResponse`. We need to ensure that data isn't lost during the translation between the generated `com.warehouse.api.beans.Warehouse` and your internal `com.fulfilment...models.Warehouse`.

* **Scenario:** Creating a warehouse with location, capacity, and stock.
* **Verification:** Ensure the `createOperation.create()` is called with a domain model containing the same data as the input bean.

## 2. Successful Retrieval (Existing ID)

* **Scenario:** Calling `getAWarehouseUnitByID` with a valid `businessUnitCode` (e.g., "MWH.001" from your `import.sql`).
* **Verification:** Ensure it returns a 200 OK with the correctly mapped fields.

## 3. Resource Not Found (Null Result)

* **Scenario:** Calling `getAWarehouseUnitByID` or `archiveAWarehouseUnit` with an ID that doesn't exist in the repository.
* **Verification:** Verify it throws a `jakarta.ws.rs.NotFoundException`. This is a crucial "null scenario" for your coverage.

## 4. Archive Operation

* **Scenario:** Triggering the `archiveAWarehouseUnit` for an existing warehouse.
* **Verification:** Verify that `archiveOperation.archive()` is called exactly once.

---

### Implementation: WarehouseResourceImplTest.java

We will use **RestAssured** for the endpoint testing to ensure the Jakarta annotations (like `@NotNull`) and the injected operations work together.

```java
package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.warehouse.api.beans.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
public class WarehouseResourceImplTest {

    @Test
    public void testListAllWarehouses() {
        given()
          .when().get("/warehouses")
          .then()
             .statusCode(200)
             // Asserting on data seeded in import.sql
             .body("businessUnitCode", hasItem("MWH.001"));
    }

    @Test
    public void testGetWarehouseById_Success() {
        given()
          .when().get("/warehouses/MWH.001")
          .then()
             .statusCode(200)
             .body("businessUnitCode", is("MWH.001"))
             .body("location", is("ZWOLLE-001"));
    }

    @Test
    public void testGetWarehouseById_NotFound() {
        given()
          .when().get("/warehouses/NON-EXISTENT")
          .then()
             .statusCode(404); 
             // This triggers the if (warehouse == null) throw block
    }

    @Test
    public void testCreateWarehouse() {
        Warehouse bean = new Warehouse();
        bean.setBusinessUnitCode("TEST-API-01");
        bean.setLocation("Den Haag");
        bean.setCapacity(500);
        bean.setStock(0);

        given()
          .contentType(ContentType.JSON)
          .body(bean)
          .when().post("/warehouses")
          .then()
             .statusCode(200) // Note: Impl returns data bean directly
             .body("businessUnitCode", is("TEST-API-01"));
    }

    @Test
    public void testArchiveWarehouse_NotFound() {
        // Testing the null scenario in archiveAWarehouseUnit
        given()
          .when().delete("/warehouses/GHOST-99")
          .then()
             .statusCode(404);
    }
}

```

### Coverage Note

In `WarehouseResourceImpl.java`, your `replaceTheCurrentActiveWarehouse` method uses `@NotNull` on the `data` parameter. Testing this with a null body in a POST/PUT request will help cover the validation interceptor logic provided by Quarkus.
