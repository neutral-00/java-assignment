# Task  3

#### Implement Warehouse Creation, Replacement and Archive logic (Must have)

Locate and implement the API endpoints handlers and Use Cases for all Warehouse related operations, like creating, retrieveing, replacing and archiving.

The following validations and constraints should be applied and a proper response should be returned to the users:

**Business Unit Code Verification**

Ensure that the specified business unit code for the warehouse doesn't already exists.

**Location Validation**

Confirm that the warehouse location is valid, meaning it must be an existing valid location.

**Warehouse Creation Feasibility**

Check if a new warehouse can be created at the specified location or if the maximum number of warehouses has already been reached.

**Capacity and Stock Validation**

Validate the warehouse capacity, ensuring it does not exceed the maximum capacity associated with the location and that it can handle the stock informed.

#### Additional Validations for Replacing a Warehouse

**Capacity Accommodation**

Ensure the new warehouse's capacity can accommodate the stock from the warehouse being replaced.

**Stock Matching**

Confirm that the stock of the new warehouse matches the stock of the previous warehouse.

## BONUS task (nice to have)

Implement the feature of associating `Warehouses` as fullfilment units of certain `Products` to determined `Stores`.

Constraints:

1. Each `Product` can be fulfilled by a maximum of 2 different `Warehouses` per `Store`

2. Each `Store` can be fulfilled by a maximum of 3 different `Warehouses`

3. Each `Warehouse` can store maximally 5 types of `Products`

## Analysis For Task 3

I see the instructions for **Task 3**, which is the core of your assignment's logistics engine. This task moves away from simple CRUD and introduces complex business rules that link your **Locations**, **Warehouses**, and eventually **Stores**.

### Overview of Task 3: The Warehouse Lifecycle

Your goal is to implement the handlers and use cases for creating, retrieving, replacing, and archiving warehouses. Unlike the `Store` domain, which used the **Active Record** pattern, the interfaces provided (`CreateWarehouseOperation`, `ReplaceWarehouseOperation`, etc.) suggest that for this domain, you should follow a **Clean Architecture/Hexagonal** approach using **Ports and Use Cases**.

### Key Constraints to Implement:

Based on the `08-warehouse-task.md` file, we must enforce several strict validations:

1. **Business Unit Code Uniqueness:** You cannot create a warehouse with a `businessUnitCode` that already exists in the system.
2. **Location Integrity:** Before creating a warehouse, you must use the `LocationResolver` to confirm the `location` string refers to a real, valid location.
3. **Density Check:** Each location has a `maxNumberOfWarehouses` limit. You must verify that adding a new warehouse won't exceed this limit.
4. **Capacity Validation:** A warehouse's `capacity` must not exceed the `maxCapacity` defined for its location.
5. **Stock Safety (Replacement):** When replacing an existing warehouse, the new one must have a capacity high enough to hold the current stock of the old one.

### Current Project State

* **Domain Models:** You have the basic `Warehouse.java` and `Location.java` POJOs.
* **Ports:** You have the interfaces for the operations.
* **Missing Infrastructure:** We still need the **JPA Entities** for Warehouse (since the provided `Warehouse.java` is a plain model), the **Repositories**, and the **REST Resource** to expose these endpoints.

### The "Bonus" Challenge

The instructions also mention a bonus task to associate `Warehouses` as fulfillment units for `Products` at specific `Stores`. This is where the "link everything" part truly happens, creating a many-to-many-to-many relationship with specific constraints (e.g., max 3 warehouses per store).
