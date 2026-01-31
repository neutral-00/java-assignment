package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
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
  public void update(Warehouse warehouse) {
    DbWarehouse dbEntity = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbEntity != null) {
      dbEntity.location = warehouse.location;
      dbEntity.capacity = warehouse.capacity;
      dbEntity.stock = warehouse.stock;
      dbEntity.archivedAt = warehouse.archivedAt;
      // Panache manages the entity, so it will auto-update on tx commit
    }
  }

  @Override
  public void remove(Warehouse warehouse) {
    delete("businessUnitCode", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return find("businessUnitCode", buCode)
            .firstResultOptional()
            .map(DbWarehouse::toWarehouse)
            .orElse(null);
  }
}
