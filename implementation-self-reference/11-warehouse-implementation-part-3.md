# Use Cases (Replace and Archive)

Following the Clean Architecture pattern, the Resource (Adapter) should convert API beans into Domain models, call the relevant Use Case (Operation), and return the result.

### Updated WarehouseResourceImpl.java

Here is the complete implementation for the missing methods:

```java
package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject WarehouseRepository warehouseRepository;
  @Inject CreateWarehouseOperation createOperation;
  @Inject ReplaceWarehouseOperation replaceOperation;
  @Inject ArchiveWarehouseOperation archiveOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var domainModel = toDomainModel(data);
    createOperation.create(domainModel);
    return data;
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new NotFoundException("Warehouse not found: " + id);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    var warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new NotFoundException("Warehouse not found: " + id);
    }
    archiveOperation.archive(warehouse);
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    // Ensure the data bean matches the URL path businessUnitCode
    data.setBusinessUnitCode(businessUnitCode);
    
    var domainModel = toDomainModel(data);
    replaceOperation.replace(domainModel);
    
    return data;
  }

  // Helper to keep logic clean
  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainModel(Warehouse data) {
    var domain = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domain.businessUnitCode = data.getBusinessUnitCode();
    domain.location = data.getLocation();
    domain.capacity = data.getCapacity();
    domain.stock = data.getStock();
    return domain;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}

```

### Key Implementation Details:

1. **`getAWarehouseUnitByID`**: This directly queries the `WarehouseRepository` using the `businessUnitCode`. If the result is null, it throws a `NotFoundException` which Quarkus maps to a 404 response.
2. **`archiveAWarehouseUnitByID`**: It first retrieves the domain object and then passes it to the `ArchiveWarehouseUseCase`. This use case should handle setting the `archivedAt` timestamp.
3. **`replaceTheCurrentActiveWarehouse`**: This maps the incoming JSON payload to the domain model and triggers the `ReplaceWarehouseUseCase`. As we discussed, this use case will contain the logic to validate that the new capacity fits the old stock.
4. **Data Mapping**: I added a `toDomainModel` helper method. In a production app, you might use **MapStruct**, but for this assignment, manual mapping is clearer and avoids extra dependencies.

### Final Check on Archive Logic

In your `ArchiveWarehouseUseCase.java`, make sure the `archive` method is actually setting the timestamp before saving:

```java
@Override
public void archive(Warehouse warehouse) {
    warehouse.archivedAt = java.time.LocalDateTime.now();
    warehouseStore.update(warehouse);
}

```

**With this Resource implemented, your Warehouse API is now fully functional. Would you like to try running a test to see if creating a warehouse correctly enforces the location capacity limits?**