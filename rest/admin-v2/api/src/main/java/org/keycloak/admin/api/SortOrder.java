package org.keycloak.admin.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(enumeration = {"asc", "desc"})
public enum SortOrder {
    ASC,
    DESC;

    public boolean isAscending() {
        return this == ASC;
    }

    public static SortOrder fromString(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        for (SortOrder order : values()) {
            if (order.name().equalsIgnoreCase(value)) {
                return order;
            }
        }
        throw new WebApplicationException("sort direction must be asc or desc", Response.Status.BAD_REQUEST);
    }
}
