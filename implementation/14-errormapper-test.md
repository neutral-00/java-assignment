Based on the JaCoCo coverage report you shared, the **`StoreResource.ErrorMapper`** currently has **0% coverage**. This is a critical area to test because it ensures that when the application fails, it provides a consistent, JSON-formatted error response to the client.

Here are the scenarios we should write for the `ErrorMapper` within a new test class (or added to your resource tests):

---

## `ErrorMapper` Test Scenarios

### 1. WebApplicationException Handling (e.g., 404 Not Found)

* **Scenario:** A `WebApplicationException` is thrown (for example, when a user requests a store ID that doesn't exist).
* **Goal:** Verify that the mapper extracts the status code from the exception and returns it in the JSON body.
* **Expected Output:** A `404` status code and a JSON body containing `"code": 404`.

### 2. General Exception Handling (500 Internal Server Error)

* **Scenario:** An unexpected error occurs (like a `RuntimeException` or `NullPointerException`).
* **Goal:** Ensure the mapper defaults to a `500` status code and captures the class name of the exception.
* **Expected Output:** A `500` status code and a JSON body containing `"exceptionType": "java.lang.RuntimeException"`.

### 3. Missing Exception Message

* **Scenario:** An exception is thrown without a message.
* **Goal:** Verify the mapper doesn't crash when `exception.getMessage()` is null and simply omits the "error" field or handles it gracefully.

---

## Recommended Test Implementation

Since the `ErrorMapper` is an inner static class annotated with `@Provider`, we can test it by triggering errors through the REST API or by injecting it directly into a unit test.

```java
@QuarkusTest
public class StoreErrorMapperTest {

    @Inject
    StoreResource.ErrorMapper errorMapper;

    @Test
    public void testToResponse_WebApplicationException() {
        // Arrange
        String errorMessage = "Store with id of 999 does not exist.";
        WebApplicationException ex = new WebApplicationException(errorMessage, 404);

        // Act
        Response response = errorMapper.toResponse(ex);

        // Assert
        assertEquals(404, response.getStatus());
        ObjectNode json = (ObjectNode) response.getEntity();
        assertEquals(404, json.get("code").asInt());
        assertEquals(errorMessage, json.get("error").asText());
    }

    @Test
    public void testToResponse_GeneralException() {
        // Arrange
        RuntimeException ex = new RuntimeException("Unexpected Database Failure");

        // Act
        Response response = errorMapper.toResponse(ex);

        // Assert
        assertEquals(500, response.getStatus());
        ObjectNode json = (ObjectNode) response.getEntity();
        assertEquals("java.lang.RuntimeException", json.get("exceptionType").asText());
    }
}

```

**Would you like me to also provide the REST-assured versions of these tests to verify that the `StoreResource` endpoints actually trigger this mapper correctly?**