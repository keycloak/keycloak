package org.keycloak.admin.api;

import java.util.Locale;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(enumeration = {"asc", "desc"})
public enum SortOrder {
    ASC,
    DESC;

    public boolean isAscending() {
        return this == ASC;
    }

    public static Optional<SortOrder> fromQueryValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(ASC);
        }
        for (SortOrder order : values()) {
            if (order.name().equalsIgnoreCase(value)) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    public String toQueryValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
