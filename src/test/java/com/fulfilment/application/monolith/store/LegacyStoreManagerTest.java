package com.fulfilment.application.monolith.store;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class LegacyStoreManagerTest {

    @Inject
    LegacyStoreManagerGateway gateway;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream standardOut = System.out;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }


    //    1. Successful Store Creation
    @Test
    public void testCreateStoreOnLegacySystem_Success() {
        // Arrange
        Store store = new Store("Warehouse-Alpha");
        store.quantityProductsInStock = 42;

        // Act
        gateway.createStoreOnLegacySystem(store);

        // Assert
        String output = outputStreamCaptor.toString();

        // Verify the specific format required by the legacy gateway
        String expectedContent = "Store created. [ name =Warehouse-Alpha ] [ items on stock =42]";

        assertTrue(output.contains("Data read from temporary file: " + expectedContent),
                "The file content should match the legacy system's expected format.");
    }

    @Test
    public void testUpdateStoreOnLegacySystem_DataIntegrity() {
        // Arrange: Create a store and then simulate an update in its values
        Store store = new Store("Boutique-Central");
        store.quantityProductsInStock = 150;

        // Act: Call the update method
        gateway.updateStoreOnLegacySystem(store);

        // Assert
        String output = outputStreamCaptor.toString();

        // Define the expected string format used in the gateway's writeToFile method
        String expectedContent = "Store created. [ name =Boutique-Central ] [ items on stock =150]";

        assertTrue(output.contains("Data read from temporary file: " + expectedContent),
                "The update method should write the correct store state to the legacy file.");
    }
}
