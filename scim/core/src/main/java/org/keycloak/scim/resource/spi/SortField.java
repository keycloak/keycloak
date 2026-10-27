package org.keycloak.scim.resource.spi;

import java.util.Objects;

/**
 * A sort field along with all options
 */
public final class SortField {
    
    public enum SortOrder {
        ASC,
        DESC;

        public boolean isAscending() {
            return this == ASC;
        }
    }

    private final String fieldName;
    private final SortOrder order;

    public SortField(String fieldName, SortOrder order) {
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
        if (!(obj instanceof SortField)) {
            return false;
        }
        SortField other = (SortField) obj;
        return fieldName.equals(other.fieldName) && order == other.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, order);
    }
}
