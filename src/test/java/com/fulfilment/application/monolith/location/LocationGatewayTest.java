package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
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
