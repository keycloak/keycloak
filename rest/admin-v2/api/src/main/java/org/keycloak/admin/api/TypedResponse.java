package org.keycloak.admin.api;

import java.util.Objects;

import jakarta.ws.rs.core.Response;

/**
 * Convenience wrapper around a raw {@link Response} that can deserialize a typed payload while
 * still exposing access to the full response.
 */
public class TypedResponse<T> implements AutoCloseable {

    private final Response response;
    private final Class<T> entityType;

    public TypedResponse(Response response, Class<T> entityType) {
        this.response = Objects.requireNonNull(response, "response cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
    }

    public Response getResponse() {
        return response;
    }

    public T readEntity() {
        return response.readEntity(entityType);
    }

    @Override
    public void close() {
        response.close();
    }
}
