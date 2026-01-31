
Implement the missing logic for `update` and `remove` so we aren't just testing `UnsupportedOperationException`.

### 1. Implementing the Missing Methods

Based on the `WarehouseStore` interface and the existing `PanacheRepository` setup, here is how we should fill those gaps:

* **`update(Warehouse warehouse)`**: We need to find the existing `DbWarehouse` by its unique business unit code, update its fields, and persist the changes.
* **`remove(Warehouse warehouse)`**: We should locate the record and delete it from the database.

---

### 2. Test Scenarios for `WarehouseRepository`

Once implemented, these are the scenarios we need to test to clear that red bar in your coverage report:

| Scenario | Goal |
| --- | --- |
| **Create & Persist** | Verify `create()` correctly maps domain fields to `DbWarehouse` and sets `createdAt`. |
| **Find by BU Code** | Ensure `findByBusinessUnitCode()` returns a domain `Warehouse` when it exists and `null` when it doesn't. |
| **Update Existing** | Confirm that changing capacity or location in the domain model reflects in the database. |
| **Remove Warehouse** | Verify the record is actually gone after calling `remove()`. |
| **Get All Mapping** | Ensure the stream mapping from `DbWarehouse` to `Warehouse` works for multiple records. |

---

### 3. Implementation of `WarehouseRepository.java`

Here is the completed code for the missing methods to make them testable:

```java
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

```

### Next Step

Since `WarehouseRepository` uses Hibernate Panache, we should use a `@QuarkusTest` with a H2 or dev-services database to verify the actual SQL interactions.

**Would you like me to generate the `WarehouseRepositoryTest` class including the setup for a clean test database state?**