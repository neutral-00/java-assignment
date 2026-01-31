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
import org.jspecify.annotations.NonNull;

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
    // createdAt and archivedAt are managed by Use Cases/Persistence,
    // so we don't map them from the incoming request body.
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
