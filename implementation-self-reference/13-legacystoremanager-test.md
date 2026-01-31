# Unit Test - LegacyStoreManager

## Test Scenarios

Since the gateway's primary job is to "emulate" a legacy system integration by creating, writing to, and deleting a temporary file, our unit tests should focus on ensuring this process happens without errors and handles the data correctly.

Here are the scenarios we should cover for `LegacyStoreManagerTest`:

---

## 1. Successful Store Creation

This is the "happy path." We want to ensure that when `createStoreOnLegacySystem` is called, the logic executes from start to finish.

* **Verification:** Ensure no exceptions are thrown during the file creation, writing, or deletion cycle.
* **Data Integrity:** Verify that a `Store` object with a specific name and stock quantity is processed correctly by the internal `writeToFile` logic.

## 2. Successful Store Update

Similar to creation, but ensuring the gateway handles an update request.

* **Verification:** Confirm that calling `updateStoreOnLegacySystem` triggers the file emulation process successfully.

## 3. Handling Special Characters in Store Names

Since the `store.name` is used as a prefix for `Files.createTempFile`, we should test how the gateway handles names that might be problematic for file systems.

* **Scenario:** A store name like `"Store/North-Main"`.
* **Verification:** Ensure the `Exception` catch block in `writeToFile` handles potential `IOException` gracefully (or identify if we need to sanitize the name).

## 4. Resource Cleanup (File Deletion)

Since this gateway explicitly deletes the file at the end of the method, we want to ensure that no "ghost" files are left behind in the temp directory.

* **Verification:** While difficult to test after the fact since it's an internal private method, we can mock the `Files` behavior or simply verify that the log "Temporary file deleted" would be reached.

## 5. Null or Empty State Handling

What happens if the `Store` object or its fields are incomplete?

* **Scenario:** Passing a `Store` with a null `name`.
* **Verification:** Confirm the gateway handles the null pointer or logs the stack trace as defined in the `catch` block without crashing the entire application.

---

### Proposed Test Structure

Given that this is a `QuarkusTest`, we can inject the gateway directly:

To test the data integrity of the `writeToFile` logic, we face a slight challenge: the method is `private` and it deletes the file immediately after writing it.

To verify the content without changing the production code to be less secure (like leaving files behind), we can use a **system output capture** strategy. Since the `LegacyStoreManagerGateway` prints the file content to the console via `System.out.println` before deletion, we can intercept that stream to validate the data integrity.

### Refined Test Scenarios for Data Integrity

Here is how we will structure the test to ensure the `Store` name and quantity are processed into the correct string format:

* **String Formatting Validation:** Verify the output matches the required template: `"Store created. [ name =" + name + " ] [ items on stock =" + quantity + "]"`.
* **Parameter Mapping:** Ensure the `store.name` and `store.quantityProductsInStock` are injected into the correct placeholders.
* **Lifecycle Verification:** Confirm the "Data read from temporary file" log contains the exact expected content.

---

### Implementation Strategy

We can use a standard JUnit 5 extension or a simple manual stream redirect to capture the console output.

```java
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

    @Test
    public void testDataIntegrityInLegacyFile() {
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
}

```

### Why this works

1. **Captures Volatile Data:** Since the file is deleted at the end of the method, we catch the data while it is still "alive" in the memory buffer of the print stream.
2. **Validates the "Contract":** It confirms that `store.name` and `store.quantityProductsInStock` from the `Store` entity are being concatenated exactly as the legacy system expects.

To test the `updateStoreOnLegacySystem` method, we will follow a similar pattern of capturing the system output. Since both the create and update methods in `LegacyStoreManagerGateway` currently call the same private `writeToFile` logic, the data integrity requirements remain identical.

### Updated Test Scenarios

* **Update Verification:** Confirm that calling the update method triggers the file emulation logic.
* **State Accuracy:** Ensure that updated values (e.g., a change in stock quantity) are correctly reflected in the "legacy" output.

---

### Implementation for Update Test

You can add this method to your existing `LegacyStoreManagerTest` class. It reuses the `outputStreamCaptor` logic to verify that the update operation processes the new state of the `Store` entity.

```java
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

```

### Key Differences in Logic

While the underlying implementation in the `LegacyStoreManagerGateway` is currently the same for both methods, having distinct tests is a best practice. If the legacy system requirements change—for example, if updates require a different file prefix or a "Store updated" string instead of "Store created"—your tests will immediately catch the discrepancy.
