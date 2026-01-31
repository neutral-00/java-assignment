To test your `LocationGateway`, you should add two specific test cases: one for the **success scenario** (finding an existing location) and one for the **failure scenario** (where an invalid ID is provided, triggering your new `orElseThrow` logic).

Here is the updated test class using **JUnit 5** best practices:

```java
package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LocationGatewayTest {

    private final LocationGateway locationGateway = new LocationGateway();

    @Test
    public void testWhenResolveExistingLocationShouldReturn() {
        // given
        String identifier = "ZWOLLE-001";

        // when
        Location location = locationGateway.resolveByIdentifier(identifier);

        // then
        // then
        assertAll("Verify Location fields",
                () -> assertEquals(identifier, location.identification, "ID should match"),
                () -> assertEquals(1, location.maxNumberOfWarehouses, "Max warehouses should match"),
                () -> assertEquals(40, location.maxCapacity, "Max capacity should match")
        );
    }

    @Test
    public void testWhenResolveNonExistingLocationShouldThrowException() {
        // given
        String invalidIdentifier = "NON-EXISTENT-999";

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            locationGateway.resolveByIdentifier(invalidIdentifier);
        });

        assertEquals("Location not found: NON-EXISTENT-999", exception.getMessage());
    }
}


```

### Key Improvements:

1. **Static Imports:** I added `import static` for `assertEquals` and `assertThrows`. This is the standard JUnit 5 style and makes the code much cleaner to read.
2. **The Failure Case:** Testing the `Exception` is just as important as testing the success. `assertThrows` ensures that your `orElseThrow` logic actually executes when it should.
3. **Assertion Messages:** I added optional descriptions (the third argument in `assertEquals`). This helps you debug much faster if a test fails in a large CI/CD pipeline.
4. **Scope:** Notice that I moved `locationGateway` to a class-level variable. Since the gateway is stateless (using a static list), we can reuse one instance for all tests.

### How to run this?

Since you are using Quarkus, you can run this test directly from your terminal while dev mode is running:

* Press **`r`** in your terminal where `quarkus:dev` is running to re-run all tests.
* Alternatively, run `./mvnw test` to run them in a standard build cycle.

[Exception testing in JUnit 5](https://www.google.com/search?q=https://www.youtube.com/watch%3Fv%3Desmr-A_fv_k)

This video demonstrates the use of the `assertThrows` method, which is the exact tool used in the code above to verify that your gateway correctly handles missing identifiers.