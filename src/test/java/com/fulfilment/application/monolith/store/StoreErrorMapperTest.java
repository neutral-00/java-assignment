package com.fulfilment.application.monolith.store;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.stores.StoreResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
