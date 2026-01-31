# Task 2 - Store

Adjust the StoreResource operations to make sure that the LegacyStoreManagerGateway calls that happens there take place after the changes are commited to the database, to guarantee that the downstream legacy system is receiving a confirmed data from us.

If you check the StoreResource class and methods, you'll notice that it uses LegacyStoreManagerGateway instance. This is simulating an integration with a legacy system with which we are syncing the Stores register handled by our system. Although the initial implementation is working properly, we need an implementation that guarantees that the change to the Store entity is propagated only after it is effectively stored in our database.

## Solution
Use a CDI event fired inside the transaction and observe it with `TransactionPhase.AFTER_SUCCESS` so the legacy gateway call runs only after the DB transaction commits.

Brief explanation:
- Add a `StoreChangeEvent` that carries the store info (id / DTO / operation).
- Fire the event from `StoreResource` immediately after persisting the change.
- Implement a CDI observer `LegacyStoreSyncObserver` with `@Observes(during = TransactionPhase.AFTER_SUCCESS)` that calls `LegacyStoreManagerGateway`. This guarantees the legacy call happens only after a successful commit.

`src/main/java/com/fulfilment/application/monolith/stores/events/StoreChangeEvent.java`
```java
// language: java
package com.fulfilment.application.monolith.stores.events;

public class StoreChangeEvent {

  public enum Type { CREATED, UPDATED, DELETED }

  private final String storeId;
  private final Type type;
  private final Object payload; // replace Object with actual Store DTO class if available

  public StoreChangeEvent(String storeId, Type type, Object payload) {
    this.storeId = storeId;
    this.type = type;
    this.payload = payload;
  }

  public String getStoreId() { return storeId; }
  public Type getType() { return type; }
  public Object getPayload() { return payload; }
}
```

`src/main/java/com/fulfilment/application/monolith/stores/observers/LegacyStoreSyncObserver.java`
```java
// language: java
package com.fulfilment.application.monolith.stores.observers;

import com.fulfilment.application.monolith.stores.events.StoreChangeEvent;
import com.fulfilment.application.monolith.stores.legacy.LegacyStoreManagerGateway;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LegacyStoreSyncObserver {

  @Inject
  LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreChange(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangeEvent event) {
    // safe to call the legacy system now — transaction was committed successfully
    // adapt call to actual legacy gateway API
    legacyStoreManagerGateway.sync(event.getStoreId(), event.getPayload(), event.getType().name());
  }
}
```

`src/main/java/com/fulfilment/application/monolith/stores/adapters/restapi/StoreResource.java` (snippet showing change)
```java
// language: java
package com.fulfilment.application.monolith.stores.adapters.restapi;

import com.fulfilment.application.monolith.stores.events.StoreChangeEvent;
import com.fulfilment.application.monolith.stores.events.StoreChangeEvent.Type;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;

// ... other imports

public class StoreResource {

  @Inject
  Event<StoreChangeEvent> storeChangeEvent;

  // repository/service injection omitted for brevity

  @Transactional
  public Response createStore(StoreDto dto) {
    // persist the store (within transaction)
    Store saved = repository.save(dto.toModel());

    // fire event while still in the transaction; observer runs AFTER_SUCCESS (commit)
    storeChangeEvent.fire(new StoreChangeEvent(saved.getId(), Type.CREATED, dto));

    return Response.status(Response.Status.CREATED).entity(saved).build();
  }

  @Transactional
  public Response updateStore(String id, StoreDto dto) {
    Store updated = repository.update(id, dto.toModel());
    storeChangeEvent.fire(new StoreChangeEvent(updated.getId(), Type.UPDATED, dto));
    return Response.ok(updated).build();
  }

  @Transactional
  public Response deleteStore(String id) {
    repository.delete(id);
    storeChangeEvent.fire(new StoreChangeEvent(id, Type.DELETED, null));
    return Response.noContent().build();
  }
}
```

Notes:
- Replace `Object` / `StoreDto` / `legacyStoreManagerGateway.sync(...)` signatures with the actual types/methods in your codebase.
- This approach works on Quarkus / CDI: the observer will only run after the transaction commits successfully.