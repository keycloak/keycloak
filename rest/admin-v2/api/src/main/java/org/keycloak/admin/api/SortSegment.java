package org.keycloak.admin.api;

import java.util.Objects;

/**
 * A single parsed clause of a {@code sort} query expression, before the field name has
 * been resolved to a resource-specific {@link SortField}.
 */
public final class SortSegment {

    private final String fieldName;
    private final SortOrder order;

    public SortSegment(String fieldName, SortOrder order) {
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName cannot be null");
        this.order = order == null ? SortOrder.ASC : order;
    }

    public String fieldName() {
        return fieldName;
    }

    public SortOrder order() {
        return order;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SortSegment)) {
            return false;
        }
        SortSegment other = (SortSegment) obj;
        return fieldName.equals(other.fieldName) && order == other.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, order);
    }
}
