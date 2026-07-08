package org.keycloak.admin.api;

import java.util.Objects;

public final class SortOption {

    private final ClientField field;
    private final SortOrder order;

    private SortOption(ClientField field, SortOrder order) {
        this.field = Objects.requireNonNull(field, "field cannot be null");
        this.order = order == null ? SortOrder.ASC : order;
    }

    public static SortOption of(ClientField field) {
        return new SortOption(field, SortOrder.ASC);
    }

    public static SortOption of(ClientField field, SortOrder order) {
        return new SortOption(field, order);
    }

    public ClientField field() {
        return field;
    }

    public SortOrder order() {
        return order;
    }

    public boolean isAscending() {
        return order.isAscending();
    }

    public String toQuerySegment() {
        if (order == SortOrder.ASC) {
            return field.toQueryValue();
        }
        return field.toQueryValue() + "|" + order.name().toLowerCase();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SortOption)) {
            return false;
        }
        SortOption other = (SortOption) obj;
        return field == other.field && order == other.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, order);
    }
}
