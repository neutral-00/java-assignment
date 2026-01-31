
# 1. Location (Must have)

> this code is located under package `com.fulfilment.application.monolith.location`

On the class `LocationGateway`, implement the method `resolveByIdentifier`.

## Plan

resolveByIdentifier should return the Location model instance for a given identifier (exact match).

### Steps
1. [x] Inspect and implement resolveByIdentifier in src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java
2. [.] Review where LocationResolver is consumed: 
    - [.] CreateWarehouseUseCase.java,
    - [.] ReplaceWarehouseUseCase.java,
    - [.] ArchiveWarehouseUseCase.java and
    - [.] WarehouseResourceImpl.java.
3. Update warehouse validations (in the use-cases) to use the resolved Location:
    - [.] Verify Location exists.
    - [.] Ensure number of warehouses at the location <= maxNumberOfWarehouses.
    - [.] Ensure total capacity at location + new warehouse capacity <= maxCapacity.
    - [.] Ensure stock <= capacity.
    - [.] For replace: ensure new warehouse capacity can accommodate stock from replaced warehouse and stock matches rules.
