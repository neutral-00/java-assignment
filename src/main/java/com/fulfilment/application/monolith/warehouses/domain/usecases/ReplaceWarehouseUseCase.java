package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

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
}
