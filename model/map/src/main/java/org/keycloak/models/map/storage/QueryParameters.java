package org.keycloak.models.map.storage;

import org.keycloak.storage.SearchableModelField;

import java.util.LinkedList;
import java.util.List;

public class QueryParameters<M> {

    protected final Integer offset;
    protected final Integer limit;
    protected final List<OrderingTuple<M>> ordering = new LinkedList<>();

    public QueryParameters(Integer offset, Integer limit, List<OrderingTuple<M>> ordering) {
        this.offset = offset;
        this.limit = limit;
        this.ordering.addAll(ordering);
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public List<OrderingTuple<M>> getOrdering() {
        return ordering;
    }

    enum Order {
        ASCENDING,
        DESCENDING
    }

    public static class OrderingTuple<M> {
        private final SearchableModelField<M> modelField;
        private final Order order;

        public OrderingTuple(SearchableModelField<M> modelField, Order order) {
            this.modelField = modelField;
            this.order = order;
        }

        public SearchableModelField<M> getModelField() {
            return modelField;
        }

        public Order getOrder() {
            return order;
        }
    }

    public static class Builder<M> {

        private Integer offset;
        private Integer limit;
        private final List<OrderingTuple<M>> ordering = new LinkedList<>();

        public static <M> Builder<M> create() {
            return new Builder<>();
        }

        public Builder<M> pagination(Integer offset, Integer limit, SearchableModelField<M> orderByField) {
            return offset(offset)
                    .limit(limit)
                    .orderBy(orderByField);
        }

        public Builder<M> offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Builder<M> limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder<M> orderBy(SearchableModelField<M> modelField) {
            return orderBy(modelField, Order.ASCENDING);
        }

        public Builder<M> orderBy(SearchableModelField<M> modelField, Order order) {
            this.ordering.add(new OrderingTuple<>(modelField, order));
            return this;
        }

        public QueryParameters<M> build() {
            return new QueryParameters<>(offset, limit, ordering);
        }
    }
}
