package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.warehouse.api.beans.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;

//@QuarkusTest
public class WarehouseResourceImplTest {

//    @Test
//    public void testListAllWarehouses() {
//        given()
//                .when().get("/warehouses")
//                .then()
//                .statusCode(200)
//                // Asserting on data seeded in import.sql
//                .body("businessUnitCode", hasItem("MWH.001"));
//    }
//
//    @Test
//    public void testGetWarehouseById_Success() {
//        given()
//                .when().get("/warehouses/MWH.001")
//                .then()
//                .statusCode(200)
//                .body("businessUnitCode", is("MWH.001"))
//                .body("location", is("ZWOLLE-001"));
//    }
//
//    @Test
//    public void testGetWarehouseById_NotFound() {
//        given()
//                .when().get("/warehouses/NON-EXISTENT")
//                .then()
//                .statusCode(404);
//        // This triggers the if (warehouse == null) throw block
//    }
//
//    @Test
//    public void testCreateWarehouse() {
//        Warehouse bean = new Warehouse();
//        bean.setBusinessUnitCode("TEST-API-01");
//        bean.setLocation("Den Haag");
//        bean.setCapacity(500);
//        bean.setStock(0);
//
//        given()
//                .contentType(ContentType.JSON)
//                .body(bean)
//                .when().post("/warehouses")
//                .then()
//                .statusCode(200) // Note: Impl returns data bean directly
//                .body("businessUnitCode", is("TEST-API-01"));
//    }
//
//    @Test
//    public void testArchiveWarehouse_NotFound() {
//        // Testing the null scenario in archiveAWarehouseUnit
//        given()
//                .when().delete("/warehouses/GHOST-99")
//                .then()
//                .statusCode(404);
//    }
}