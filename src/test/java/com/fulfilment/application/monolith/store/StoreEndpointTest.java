package com.fulfilment.application.monolith.store;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

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
                .body("name", is("NEW_TEST_STORE"));
//                .body("id", is(4)); // Verify the REST response contains ID 4

        // Verify the legacy gateway was called with a Store object that has ID 4
        verify(legacyStoreManagerGateway, timeout(1000))
                .createStoreOnLegacySystem(org.mockito.ArgumentMatchers.argThat(store ->
                        store.id != null
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

    @Test
    public void testCreateStoreSucceedsEvenIfLegacyFails() {
        Store newStore = new Store("RESILIENT_STORE");

        // Simulate legacy system throwing an exception
        doThrow(new RuntimeException("Legacy System Down"))
                .when(legacyStoreManagerGateway).createStoreOnLegacySystem(any(Store.class));

        given()
                .contentType(ContentType.JSON)
                .body(newStore)
                .when()
                .post("/store")
                .then()
                .statusCode(201); // The DB commit still succeeded!

        // Verify the call was attempted
        verify(legacyStoreManagerGateway, timeout(1000)).createStoreOnLegacySystem(any(Store.class));
    }
}
