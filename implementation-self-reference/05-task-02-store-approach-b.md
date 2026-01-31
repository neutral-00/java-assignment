# Task 2 - Store

Adjust the StoreResource operations to make sure that the LegacyStoreManagerGateway calls that happens there take place after the changes are commited to the database, to guarantee that the downstream legacy system is receiving a confirmed data from us.

If you check the StoreResource class and methods, you'll notice that it uses LegacyStoreManagerGateway instance. This is simulating an integration with a legacy system with which we are syncing the Stores register handled by our system. Although the initial implementation is working properly, we need an implementation that guarantees that the change to the Store entity is propagated only after it is effectively stored in our database.

## Solution

To achieve the requirement that calls to the `LegacyStoreManagerGateway` only occur **after** the database transaction is successfully committed, you should use the **Jakarta Transactions (JTA) Synchronization** mechanism.

Currently, your `StoreResource` methods (like `create`, `update`, and `patch`) are annotated with `@Transactional`. This means the database commit happens at the very end of the method execution. Because `legacyStoreManagerGateway` is called *inside* that method, it is called **before** the commit. If the database commit fails (e.g., due to a unique constraint violation on the store name), your legacy system will have received data that doesn't actually exist in your database.

### The Solution: `TransactionSynchronizationRegistry`

You can inject the `TransactionSynchronizationRegistry` to register a "post-commit" task. This task will only execute if the transaction status is `STATUS_COMMITTED`.

#### 1. Implementation Strategy

Modify `StoreResource.java` to include a helper method that schedules the legacy call after the transaction finishes.

```java
// Add these imports to StoreResource.java
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

// ... inside StoreResource class ...

@Inject
TransactionSynchronizationRegistry txRegistry;

private void callLegacyAfterCommit(Store store, boolean isUpdate) {
    txRegistry.registerInterposedSynchronization(new jakarta.transaction.Synchronization() {
        @Override
        public void beforeCompletion() {}

        @Override
        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
                if (isUpdate) {
                    legacyStoreManagerGateway.updateStoreOnLegacySystem(store);
                } else {
                    legacyStoreManagerGateway.createStoreOnLegacySystem(store);
                }
            }
        }
    });
}

```

#### 2. Update the Resource Methods

Replace the direct calls to `legacyStoreManagerGateway` with this new helper.

**For the `create` method:**

```java
@POST
@Transactional
public Response create(Store store) {
    if (store.id != null) {
        throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    store.persist();
    
    // Schedule legacy call for after the transaction successfully commits
    callLegacyAfterCommit(store, false); 

    return Response.ok(store).status(201).build();
}

```

**For the `update` and `patch` methods:**
Apply the same pattern, passing `true` for the `isUpdate` flag:

```java
// Inside update/patch methods:
// Replace legacyStoreManagerGateway.updateStoreOnLegacySystem(updatedStore);
callLegacyAfterCommit(entity, true); 

```

### Why this works

* **Guaranteed Data Integrity:** The `afterCompletion` method is called by the transaction manager only after the database has finalized the `COMMIT`.
* **Rollback Safety:** If `store.persist()` fails or any other exception occurs before the method ends, the transaction status will be `STATUS_ROLLEDBACK`, and the legacy call will never be triggered.
* **Clean Design:** It preserves the use of `@Transactional` for your database logic while cleanly separating the "side effect" (the legacy system call) into the post-commit phase.

### Note on `LegacyStoreManagerGateway`

In the provided `LegacyStoreManagerGateway.java`, the code writes to a temporary file. By moving this logic to the post-commit phase, you ensure that these files are only generated for stores that actually exist in your primary PostgreSQL database.

## 💡 Open Question
1. shouldn't we have a `deleteStoreOnLegacySystem(Store store)` method in LegacyStoreManagerGateway.java, which can be called from the delete method in StoreResource.java to use the same TransactionSynchronizationRegistry pattern.