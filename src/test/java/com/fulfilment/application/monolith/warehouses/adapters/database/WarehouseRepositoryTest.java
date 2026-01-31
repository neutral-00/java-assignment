package com.fulfilment.application.monolith.warehouses.adapters.database;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class WarehouseRepositoryTest {
    @Inject
    WarehouseRepository repository;

    @Test
    public void testFindByBusinessUnitCode_ExistingSeedData() {
        // We use the data defined in import.sql
        // INSERT INTO warehouse(...) VALUES (1, 'MWH.001', 'ZWOLLE-001', 100, 10, ...);
        Warehouse found = repository.findByBusinessUnitCode("MWH.001");

        assertNotNull(found, "Should find the warehouse seeded by import.sql");
        assertEquals("ZWOLLE-001", found.location);
        assertEquals(100, found.capacity);
    }

    @Test
    @Transactional
    public void testCreateAndRetrieve_NewData() {
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "NEW-001";
        warehouse.location = "Eindhoven";
        warehouse.capacity = 250;
        warehouse.stock = 50;

        repository.create(warehouse);

        Warehouse found = repository.findByBusinessUnitCode("NEW-001");
        assertNotNull(found);
        assertEquals("Eindhoven", found.location);
    }

    @Test
    @Transactional
    public void testCreateAndFindByCode() {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "BU-001";
        warehouse.location = "Amsterdam";
        warehouse.capacity = 500;
        warehouse.stock = 100;

        // Act
        repository.create(warehouse);
        Warehouse found = repository.findByBusinessUnitCode("BU-001");

        // Assert
        assertNotNull(found);
        assertEquals("Amsterdam", found.location);
        assertEquals(500, found.capacity);
    }

    @Test
    @Transactional
    public void testUpdate_UsingSeedData() {
        // Arrange: Fetch existing record from import.sql
        Warehouse existing = repository.findByBusinessUnitCode("MWH.012");
        assertNotNull(existing);

        // Act: Modify the domain object
        existing.location = "NEW-AMSTERDAM";
        existing.capacity = 999;
        repository.update(existing);

        // Assert: Verify changes persisted
        Warehouse updated = repository.findByBusinessUnitCode("MWH.012");
        assertEquals("NEW-AMSTERDAM", updated.location);
        assertEquals(999, updated.capacity);
    }

    @Test
    @Transactional
    public void testUpdate() {
        // Arrange
        Warehouse initial = new Warehouse();
        initial.businessUnitCode = "BU-002";
        initial.location = "Rotterdam";
        repository.create(initial);

        // Act
        Warehouse updateData = new Warehouse();
        updateData.businessUnitCode = "BU-002";
        updateData.location = "Utrecht"; // Changed location
        updateData.capacity = 1000;
        repository.update(updateData);

        // Assert
        Warehouse updated = repository.findByBusinessUnitCode("BU-002");
        assertEquals("Utrecht", updated.location);
        assertEquals(1000, updated.capacity);
    }

    @Test
    @Transactional
    public void testRemoveSeeded() {
        // Arrange: Use a specific seeded record
        Warehouse toRemove = repository.findByBusinessUnitCode("MWH.023");
        assertNotNull(toRemove);

        // Act
        repository.remove(toRemove);

        // Assert
        Warehouse found = repository.findByBusinessUnitCode("MWH.023");
        assertNull(found, "Warehouse should be deleted from the database");
    }

    @Test
    public void testGetAll_IncludesSeedData() {
        List<Warehouse> all = repository.getAll();

        // import.sql adds 3 warehouses
        assertTrue(all.size() >= 3, "Should contain at least the 3 seeded warehouses");
    }

    @Test
    @Transactional
    public void testRemove() {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "BU-003";
        repository.create(warehouse);

        // Act
        repository.remove(warehouse);

        // Assert
        Warehouse found = repository.findByBusinessUnitCode("BU-003");
        assertNull(found);
    }

}
