package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ArchiveWarehouseUseCaseTest {
    private WarehouseStore warehouseStore;
    private ArchiveWarehouseUseCase useCase;

    @BeforeEach
    void setUp() {
        warehouseStore = mock(WarehouseStore.class);
        useCase = new ArchiveWarehouseUseCase(warehouseStore);
    }

    @Test
    void testArchive_Success() {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "MWH.001";
        warehouse.archivedAt = null; // Initially not archived

        // Act
        useCase.archive(warehouse);

        // Assert
        // 1. Verify the timestamp was set
        assertNotNull(warehouse.archivedAt, "ArchivedAt timestamp should be set");
        assertTrue(warehouse.archivedAt.isBefore(LocalDateTime.now().plusSeconds(1)));

        // 2. Verify the changes were persisted to the store
        verify(warehouseStore, times(1)).update(warehouse);
    }
}
