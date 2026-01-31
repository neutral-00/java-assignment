package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CreateWarehouseUseCaseTest {
    private WarehouseStore warehouseStore;
    private LocationResolver locationResolver;
    private CreateWarehouseUseCase useCase;

    @BeforeEach
    void setUp() {
        warehouseStore = mock(WarehouseStore.class);
        locationResolver = mock(LocationResolver.class);
        useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
    }

    @Test
    void testCreateWarehouse_Success() {
        Warehouse w = new Warehouse();
        w.businessUnitCode = "NEW.001";
        w.location = "ZWOLLE-001";
        w.capacity = 30;
        w.stock = 10;

        // Mocking a valid location that has space for 1 warehouse and 40 units
        when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
                .thenReturn(new Location("ZWOLLE-001", 1, 40));
        when(warehouseStore.getAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> useCase.create(w));
        verify(warehouseStore, times(1)).create(w);
    }

    @Test
    void testCreateWarehouse_DuplicateBusinessUnitCode() {
        Warehouse w = new Warehouse();
        w.businessUnitCode = "EXISTING.001";

        // Mock that a warehouse with this code already exists
        when(warehouseStore.findByBusinessUnitCode("EXISTING.001")).thenReturn(new Warehouse());

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> useCase.create(w));
        assertEquals(409, ex.getResponse().getStatus());
    }

    @Test
    void testCreateWarehouse_MaxWarehousesAtLocationReached() {
        Warehouse w = new Warehouse();
        w.location = "ZWOLLE-001";

        // Location only allows 1 warehouse
        when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
                .thenReturn(new Location("ZWOLLE-001", 1, 100));

        // Simulate one already exists at that location
        Warehouse existing = new Warehouse();
        existing.location = "ZWOLLE-001";
        when(warehouseStore.getAll()).thenReturn(List.of(existing));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> useCase.create(w));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
    }

    @Test
    void testCreateWarehouse_CapacityExceedsLocationLimit() {
        Warehouse w = new Warehouse();
        w.location = "ZWOLLE-001";
        w.capacity = 100; // Too high

        // Location only allows max 40
        when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
                .thenReturn(new Location("ZWOLLE-001", 1, 40));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> useCase.create(w));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("capacity"));
    }
}
