
# Warehouse Implementation - phase 1

Here are the steps to complete **Task 3** using the Hexagonal architecture.

### 1. Register the LocationGateway

Ensure `LocationGateway` is managed by Quarkus so it can be injected into your Use Cases. Add the `@ApplicationScoped` annotation to the class:

```java
@ApplicationScoped // Add this so Quarkus can inject it
public class LocationGateway implements LocationResolver { 
    // ... existing list and logic
}

```

### 2. Implement the Warehouse Repository

You must bridge the gap between the domain `Warehouse` model and the `DbWarehouse` entity. Update `WarehouseRepository.java` to handle persistence:

```java
@Override
@Transactional
public void create(Warehouse warehouse) {
    DbWarehouse dbEntity = new DbWarehouse();
    // Map fields from domain model to DB entity
    dbEntity.businessUnitCode = warehouse.businessUnitCode;
    dbEntity.location = warehouse.location;
    dbEntity.capacity = warehouse.capacity;
    dbEntity.stock = warehouse.stock;
    dbEntity.createdAt = LocalDateTime.now();
    this.persist(dbEntity);
}

@Override
public Warehouse findByBusinessUnitCode(String buCode) {
    return find("businessUnitCode", buCode)
            .firstResultOptional()
            .map(DbWarehouse::toWarehouse)
            .orElse(null);
}

```

### 3. Implement the Create Use Case (The Core Logic)

This is where you enforce the business rules from `08-warehouse-task.md`. Update `CreateWarehouseUseCase.java`:

To follow the **Hexagonal Architecture** pattern, your `CreateWarehouseUseCase` must interact with the `LocationResolver` port to validate the business rules.

Since `LocationGateway` implements `LocationResolver`, you should inject the **Interface (Port)** into your Use Case. This keeps your domain logic decoupled from the specific implementation details of how locations are resolved.

### Updated `CreateWarehouseUseCase.java`

Here is how the class should look with the proper injections and business logic:

```java
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
    // 1. Business Unit Code Verification
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException("Business Unit Code already exists", 409);
    }

    // 2. Location Validation
    // The LocationGateway throws an exception if not found
    Location location = locationResolver.resolveByIdentifier(warehouse.location);

    // 3. Warehouse Creation Feasibility (Density Check)
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

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }
}

```

### Why this is the right approach:

* **Decoupling:** By injecting `LocationResolver` (the port) instead of `LocationGateway` (the adapter), your domain logic doesn't care if the locations come from a hardcoded list, a database, or an external microservice.
* **Testability:** In your Unit Tests, you can now easily mock the `LocationResolver` to return different `maxCapacity` values to test your validation logic without needing the actual `LocationGateway` class.
* **Inversion of Control:** Quarkus handles the wiring. As long as `LocationGateway` is marked with `@ApplicationScoped`, it will be automatically provided whenever a `LocationResolver` is requested.

### Important: Update `LocationGateway.java`

Make sure you add the `@ApplicationScoped` annotation to your `LocationGateway.java` file so that Quarkus recognizes it as a bean that can be injected.

---

### 4. Implement the Replace Use Case

This task has specific "Stock Matching" and "Capacity Accommodation" rules. Update `ReplaceWarehouseUseCase.java`:

```java
@Override
public void replace(Warehouse newWarehouse) {
    Warehouse oldWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (oldWarehouse == null) {
        throw new WebApplicationException("Warehouse to replace not found", 404);
    }

    // Validation: New capacity must fit OLD stock
    if (newWarehouse.capacity < oldWarehouse.stock) {
        throw new WebApplicationException("New capacity cannot accommodate existing stock", 400);
    }

    // Validation: Stock must match for seamless replacement
    if (!newWarehouse.stock.equals(oldWarehouse.stock)) {
        throw new WebApplicationException("Replacement stock must match current stock", 400);
    }

    warehouseStore.update(newWarehouse);
}

```

### 5. Wiring the REST API

Finally, update `WarehouseResourceImpl.java` to call these use cases:

```java
@Inject CreateWarehouseUseCase createUseCase;

@Override
public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    // Convert API bean to Domain model
    var domainModel = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainModel.businessUnitCode = data.getBusinessUnitCode();
    domainModel.location = data.getLocation();
    domainModel.capacity = data.getCapacity();
    domainModel.stock = data.getStock();

    createUseCase.create(domainModel);
    return data;
}

```

### Summary of what's next:

1. **Map the `update` and `remove` methods** in your Repository.
2. **Handle exceptions** in the Resource to ensure `WebApplicationException` returns the correct JSON status codes.

**Would you like me to provide the full implementation for the Repository `update` method, as it requires matching the `DbWarehouse` record by its unique Business Unit Code?**