package com.fulfilment.application.monolith.warehouses.adapters.restapi;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class WarehouseResourceImplTest {
    @Inject
    WarehouseResourceImpl warehouseResource;

    @InjectMock
    WarehouseRepository warehouseRepository;

    @InjectMock
    CreateWarehouseOperation createOperation;

    @InjectMock
    ReplaceWarehouseOperation replaceOperation;

    @InjectMock
    ArchiveWarehouseOperation archiveOperation;

    @Test
    public void testCreateANewWarehouseUnit_Mapping() {
        // Arrange
        com.warehouse.api.beans.Warehouse apiBean = new com.warehouse.api.beans.Warehouse();
        apiBean.setBusinessUnitCode("BU-123");
        apiBean.setLocation("Amsterdam");
        apiBean.setCapacity(1000);
        apiBean.setStock(50);

        // Act
        warehouseResource.createANewWarehouseUnit(apiBean);

        // Assert: Capture the domain model passed to the operation
        ArgumentCaptor<Warehouse> domainCaptor = ArgumentCaptor.forClass(Warehouse.class);
        verify(createOperation).create(domainCaptor.capture());

        Warehouse capturedDomain = domainCaptor.getValue();
        assertEquals("BU-123", capturedDomain.businessUnitCode);
        assertEquals("Amsterdam", capturedDomain.location);
        assertEquals(1000, capturedDomain.capacity);
        assertEquals(50, capturedDomain.stock);
    }

    @Test
    public void testGetAWarehouseUnitByID_Success() {
        // Arrange
        Warehouse domainWarehouse = new Warehouse();
        domainWarehouse.businessUnitCode = "BU-123";
        domainWarehouse.location = "Rotterdam";

        when(warehouseRepository.findByBusinessUnitCode("BU-123")).thenReturn(domainWarehouse);

        // Act
        com.warehouse.api.beans.Warehouse response = warehouseResource.getAWarehouseUnitByID("BU-123");

        // Assert
        assertNotNull(response);
        assertEquals("BU-123", response.getBusinessUnitCode());
        assertEquals("Rotterdam", response.getLocation());
    }

    @Test
    public void testGetAWarehouseUnitByID_NotFound() {
        // Arrange
        when(warehouseRepository.findByBusinessUnitCode("UNKNOWN")).thenReturn(null);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            warehouseResource.getAWarehouseUnitByID("UNKNOWN");
        });
    }
}
