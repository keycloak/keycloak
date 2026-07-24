package org.keycloak.admin.api;

import java.util.Objects;

public final class SortOption<F extends SortField> {

    private final F field;
    private final SortOrder order;

    private SortOption(F field, SortOrder order) {
        this.field = Objects.requireNonNull(field, "field cannot be null");
        this.order = order == null ? SortOrder.ASC : order;
    }

    public static <F extends SortField> SortOption<F> of(F field) {
        return new SortOption<>(field, SortOrder.ASC);
    }

    public static <F extends SortField> SortOption<F> of(F field, SortOrder order) {
        return new SortOption<>(field, order);
    }

    public F field() {
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
        SortOption<?> other = (SortOption<?>) obj;
        return Objects.equals(field, other.field) && order == other.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, order);
    }
}
