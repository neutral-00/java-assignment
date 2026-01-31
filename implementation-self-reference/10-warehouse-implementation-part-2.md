# Task 3 Implementation

Follow the **Hexagonal Architecture** pattern, treating the domain logic as the core and using ports to interact with the database and location data.

### 1. Enable Injection for the Location Adapter

First, ensure Quarkus manages your `LocationGateway`. Add the `@ApplicationScoped` annotation so it can fulfill the `LocationResolver` port.

```java
package com.fulfilment.application.monolith.location;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped // Add this
public class LocationGateway implements LocationResolver {
    // ... existing list and logic
}

```

### 2. Implement the Repository (Database Adapter)

We need to implement the mapping between the Domain Model (`Warehouse`) and the JPA Entity (`DbWarehouse`) in `WarehouseRepository.java`.

```java
@Override
@Transactional
public void create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
    this.persist(entity);
}

@Override
public Warehouse findByBusinessUnitCode(String buCode) {
    return find("businessUnitCode", buCode)
            .firstResultOptional()
            .map(DbWarehouse::toWarehouse)
            .orElse(null);
}

@Override
@Transactional
public void update(Warehouse warehouse) {
    DbWarehouse entity = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (entity != null) {
        entity.capacity = warehouse.capacity;
        entity.stock = warehouse.stock;
        entity.archivedAt = warehouse.archivedAt;
        this.persist(entity);
    }
}

```

### 3. Implement CreateWarehouseUseCase

Now we wire the `LocationResolver` and `WarehouseStore` into the creation logic to enforce the business rules from your task description.

```java
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // 1. BU Code Verification
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException("Business Unit Code already exists", 409);
    }

    // 2. Location Validation (throws IllegalArgumentException if invalid)
    Location location = locationResolver.resolveByIdentifier(warehouse.location);

    // 3. Density Check
    long activeCount = warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location) && w.archivedAt == null)
            .count();
    if (activeCount >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException("Max warehouses reached for this location", 400);
    }

    // 4. Capacity and Stock Validation
    if (warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException("Capacity exceeds location maximum", 400);
    }
    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException("Stock exceeds warehouse capacity", 400);
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }
}

```

### 4. Implement ReplaceWarehouseUseCase

This use case ensures that a replacement doesn't break inventory integrity.

```java
@Override
public void replace(Warehouse newWarehouse) {
    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
        throw new WebApplicationException("Warehouse not found", 404);
    }

    // Capacity Accommodation: New capacity must fit current stock
    if (newWarehouse.capacity < current.stock) {
        throw new WebApplicationException("New capacity cannot accommodate current stock", 400);
    }

    // Stock Matching: Replacement must have identical stock levels
    if (!newWarehouse.stock.equals(current.stock)) {
      throw new WebApplicationException("Replacement stock must match current stock", 400);
    }

    warehouseStore.update(newWarehouse);
}

```

### 5. Final Wiring in the Resource

Finally, update `WarehouseResourceImpl.java` to use the operations:

```java
@Inject CreateWarehouseOperation createOperation;
@Inject ReplaceWarehouseOperation replaceOperation;

@Override
public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var domainModel = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainModel.businessUnitCode = data.getBusinessUnitCode();
    domainModel.location = data.getLocation();
    domainModel.capacity = data.getCapacity();
    domainModel.stock = data.getStock();
    
    createOperation.create(domainModel);
    return data;
}

```

**Would you like to move on to the "Bonus Task" involving the association of Warehouses to specific Products and Stores?**