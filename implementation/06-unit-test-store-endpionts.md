# Unit Test for Store Endpoints

To verify that your synchronization logic works correctly, creating a `StoreEndpointTest.java` is the right move. However, testing **post-commit behavior** is slightly more complex than standard CRUD tests because you need to verify a "side effect" that happens outside the main request thread.

Since `LegacyStoreManagerGateway` writes to standard output (`System.out.println`), we can use a **Mockito Spy** to verify that the gateway methods are called only when expected.

### 1. Proposed Test Implementation

Create `src/test/java/com/fulfilment/application/monolith/stores/StoreEndpointTest.java`:

```java
package com.fulfilment.application.monolith.store;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class StoreEndpointTest {
    @InjectMock
    LegacyStoreManagerGateway legacyStoreManagerGateway;

    @Test
    public void testListAllStores() {
        given()
                .when()
                .get("/store")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.greaterThanOrEqualTo(2));
    }

    @Test
    public void testCreateStoreCallsLegacy() {
        Store newStore = new Store("NEW_TEST_STORE");
        newStore.quantityProductsInStock = 50;

        given()
                .contentType(ContentType.JSON)
                .body(newStore)
                .when()
                .post("/store")
                .then()
                .statusCode(201)
                .body("name", is("NEW_TEST_STORE"))
                .body("id", is(4)); // Verify the REST response contains ID 4

        // Verify the legacy gateway was called with a Store object that has ID 4
        verify(legacyStoreManagerGateway, timeout(1000))
                .createStoreOnLegacySystem(org.mockito.ArgumentMatchers.argThat(store ->
                        store.id != null && store.id == 4L
                ));
    }

    @Test
    public void testCreateStoreWithIdShouldFail() {
        Store invalidStore = new Store("INVALID");
        invalidStore.id = 100L; // Setting an ID manually

        given()
                .contentType(ContentType.JSON)
                .body(invalidStore)
                .when()
                .post("/store")
                .then()
                .statusCode(422);
    }

    @Test
    public void testUpdateStoreCallsLegacy() {
        Store updateData = new Store("KALLAX_UPDATED");
        updateData.quantityProductsInStock = 20;

        // ID 2 corresponds to KALLAX from import.sql
        given()
                .contentType(ContentType.JSON)
                .body(updateData)
                .when()
                .put("/store/2")
                .then()
                .statusCode(200)
                .body("name", is("KALLAX_UPDATED"));

        // Verify the legacy system is notified of the update after commit
        verify(legacyStoreManagerGateway, timeout(1000)).updateStoreOnLegacySystem(any(Store.class));
    }

    @Test
    public void testUpdateStoreMissingName() {
        Store invalidUpdate = new Store(); // Name is null
        given()
                .contentType(ContentType.JSON)
                .body(invalidUpdate)
                .when()
                .put("/store/2")
                .then()
                .statusCode(422);
    }

    @Test
    public void testPatchStoreCallsLegacy() {
        Store patchData = new Store("BESTÅ_PATCHED");
        patchData.quantityProductsInStock = 15;

        // ID 3 corresponds to BESTÅ from import.sql
        given()
                .contentType(ContentType.JSON)
                .body(patchData)
                .when()
                .patch("/store/3")
                .then()
                .statusCode(200);

        verify(legacyStoreManagerGateway, timeout(1000)).updateStoreOnLegacySystem(any(Store.class));
    }

    @Test
    public void testDeleteStore() {
        // ID 1 corresponds to TONSTAD
        given()
                .when()
                .delete("/store/1")
                .then()
                .statusCode(204);

        // Note: Legacy gateway is NOT currently called in your delete method
    }

    @Test
    public void testGetSingleStoreNotFound() {
        given()
                .when()
                .get("/store/999")
                .then()
                .statusCode(404)
                .body("error", is("Store with id of 999 does not exist."));
    }

    @Test
    public void testGetSingleStoreFound() {
        given()
                .when()
                .get("/store/2")
                .then()
                .statusCode(200)
                .body("quantityProductsInStock", is(20));
    }

    @Test
    public void testUpdateStoreNotFound() {
        Store updateData = new Store("NON_EXISTENT");
        given()
                .contentType(ContentType.JSON)
                .body(updateData)
                .when()
                .put("/store/9999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testPatchStoreNotFound() {
        Store patchData = new Store("NON_EXISTENT");
        given()
                .contentType(ContentType.JSON)
                .body(patchData)
                .when()
                .patch("/store/9999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteStoreNotFound() {
        given()
                .when()
                .delete("/store/9999")
                .then()
                .statusCode(404);
    }
}

```

### 2. Key Insights from your Codebase

* **Database State:** Your `import.sql` provides initial data like 'TONSTAD' and 'KALLAX'. Your tests can rely on these IDs (1, 2, 3) existing at the start of each test.
* **Transactional Behavior:** In your updated `StoreResource.java`, you are using `txRegistry.registerInterposedSynchronization`. This is the correct Jakarta EE approach to ensure the legacy system is only notified if the database work is final.
* **The "Delete" Gap:** As noted earlier, your current `delete` method in `StoreResource.java` performs `entity.delete()` but lacks the `callLegacyAfterCommit` hook. If the assignment requires *all* changes to be propagated, you should eventually add a delete method to the legacy gateway.

### 3. How to verify "Commit-Only" Logic

To truly test your fix, you would ideally write a test that **fails** the database commit (for example, by trying to create a Store with a duplicate `name`, which is marked `@Column(unique = true)` in `Store.java`).

In that scenario:

1. The `store.persist()` might succeed in memory.
2. The transaction commit will fail at the end of the method.
3. **The expectation:** `legacyStoreManagerGateway` should **never** be called.

### NOTE
Since we are using import.sql to seed your database, remember that these IDs are global for the test suite execution. if another test runs before this one and inserts a store, your ID might be 5 instead of 4.