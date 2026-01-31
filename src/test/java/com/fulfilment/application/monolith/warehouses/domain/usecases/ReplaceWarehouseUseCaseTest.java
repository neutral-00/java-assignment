package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ReplaceWarehouseUseCaseTest {
    private WarehouseStore warehouseStore;
    private ReplaceWarehouseUseCase useCase;

    @BeforeEach
    void setUp() {
        warehouseStore = mock(WarehouseStore.class);
        useCase = new ReplaceWarehouseUseCase(warehouseStore);
    }

    @Test
    void testReplace_Success() {
        // Current warehouse in DB
        Warehouse current = new Warehouse();
        current.businessUnitCode = "MWH.001";
        current.stock = 50;
        current.capacity = 100;

        // Replacement: same stock, new capacity (must be >= 50)
        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "MWH.001";
        replacement.stock = 50;
        replacement.capacity = 80;

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);

        assertDoesNotThrow(() -> useCase.replace(replacement));
        verify(warehouseStore, times(1)).update(replacement);
    }

    @Test
    void testReplace_NotFound() {
        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "NON-EXISTENT";

        when(warehouseStore.findByBusinessUnitCode("NON-EXISTENT")).thenReturn(null);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void testReplace_CapacityTooSmallForCurrentStock() {
        Warehouse current = new Warehouse();
        current.businessUnitCode = "MWH.001";
        current.stock = 50; // Existing stock is 50

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "MWH.001";
        replacement.capacity = 40; // Attempting to replace with capacity of 40
        replacement.stock = 50;

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("accommodate"));
    }

    @Test
    void testReplace_StockMismatch() {
        Warehouse current = new Warehouse();
        current.businessUnitCode = "MWH.001";
        current.stock = 50;

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "MWH.001";
        replacement.capacity = 100;
        replacement.stock = 60; // ERROR: Stock must match exactly for a seamless transition

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getMessage().contains("match"));
    }
}
