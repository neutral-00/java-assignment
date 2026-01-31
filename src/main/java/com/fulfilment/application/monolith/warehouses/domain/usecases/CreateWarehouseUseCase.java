package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;

  private final LocationResolver locationResolver; // Port injected here

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // 1. BU Code Uniqueness
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException("Business Unit Code already exists", 409);
    }

    // 2. Location Validation
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    // LocationGateway throws IllegalArgumentException if not found

    /// 3. Warehouse Creation Feasibility (Density Check)
    long currentActiveCount = warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location) && w.archivedAt == null)
            .count();
    if (currentActiveCount >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException("Maximum number of warehouses reached for this location", 400);
    }

    // 4. Capacity and Stock Validation
    if (warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException("Warehouse capacity exceeds the location's maximum capacity", 400);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException("Stock informed exceeds the warehouse capacity", 400);
    }

    // if all went well, create the warehouse
    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }
}
