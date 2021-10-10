package org.keycloak.models.map.storage;

import org.keycloak.storage.SearchableModelField;

import java.util.LinkedList;
import java.util.List;

import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;

/**
 * Wraps together parameters for querying storage e.g. number of results to return, requested order or filtering criteria
 *
 * @param <M> Provide entity specific type checking, for example, when we create {@code QueryParameters}
 *           instance for Users, M is equal to UserModel, hence we are not able, for example, to order result by a
 *           {@link SearchableModelField} defined for clients in {@link org.keycloak.models.ClientModel}.
 */
public class QueryParameters<M> {

    private Integer offset;
    private Integer limit;
    private final List<OrderBy<M>> orderBy = new LinkedList<>();
    private ModelCriteriaBuilder<M> mcb;

    public QueryParameters() {
    }

    public QueryParameters(ModelCriteriaBuilder<M> mcb) {
        this.mcb = mcb;
    }

    /**
     * Creates a new {@code QueryParameters} instance initialized with {@link ModelCriteriaBuilder}
     *
     * @param mcb filtering criteria
     * @param <M> model type
     * @return a new {@code QueryParameters} instance
     */
    public static <M> QueryParameters<M> withCriteria(ModelCriteriaBuilder<M> mcb) {
        return new QueryParameters<>(mcb);
    }

    /**
     * Sets pagination (offset, limit and orderBy) parameters to {@code QueryParameters}
     *
     * @param offset
     * @param limit
     * @param orderByAscField
     * @return this object
     */
    public QueryParameters<M> pagination(Integer offset, Integer limit, SearchableModelField<M> orderByAscField) {
        this.offset = offset;
        this.limit = limit;
        this.orderBy.add(new OrderBy<>(orderByAscField, ASCENDING));

        return this;
    }

    /**
     * Sets orderBy parameter; can be called repeatedly; fields are stored in a list where the first field has highest
     * priority when determining order; e.g. the second field is compared only when values for the first field are equal
     *
     * @param searchableModelField
     * @return this object
     */
    public QueryParameters<M> orderBy(SearchableModelField<M> searchableModelField, Order order) {
        orderBy.add(new OrderBy<>(searchableModelField, order));

        return this;
    }

    /**
     * Sets offset parameter
     *
     * @param offset
     * @return
     */
    public QueryParameters<M> offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Sets limit parameter
     *
     * @param limit
     * @return
     */
    public QueryParameters<M> limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public ModelCriteriaBuilder<M> getModelCriteriaBuilder() {
        return mcb;
    }

    public List<OrderBy<M>> getOrderBy() {
        return orderBy;
    }

    /**
     * Enum for ascending or descending ordering
     */
    public enum Order {
        ASCENDING,
        DESCENDING
    }

    /**
     * Wrapper class for a field with its {@code Order}, ascending or descending
     *
     * @param <M>
     */
    public static class OrderBy<M> {
        private final SearchableModelField<M> modelField;
        private final Order order;

        public OrderBy(SearchableModelField<M> modelField, Order order) {
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
}
