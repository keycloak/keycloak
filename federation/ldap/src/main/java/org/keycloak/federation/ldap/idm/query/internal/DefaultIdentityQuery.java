package org.keycloak.federation.ldap.idm.query.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.IdentityQuery;
import org.keycloak.federation.ldap.idm.query.IdentityQueryBuilder;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.Sort;
import org.keycloak.federation.ldap.idm.store.IdentityStore;
import org.keycloak.models.ModelException;

import static java.util.Collections.unmodifiableSet;

/**
 * Default IdentityQuery implementation.
 *
 * @param <T>
 *
 * @author Shane Bryzak
 */
public class DefaultIdentityQuery<T extends IdentityType> implements IdentityQuery<T> {

    private final Map<QueryParameter, Object[]> parameters = new LinkedHashMap<QueryParameter, Object[]>();
    private final Class<T> identityType;
    private final IdentityStore identityStore;
    private final IdentityQueryBuilder queryBuilder;
    private int offset;
    private int limit;
    private Object paginationContext;
    private QueryParameter[] sortParameters;
    private boolean sortAscending = true;
    private final Set<Condition> conditions = new LinkedHashSet<Condition>();
    private final Set<Sort> ordering = new LinkedHashSet<Sort>();

    public DefaultIdentityQuery(IdentityQueryBuilder queryBuilder, Class<T> identityType, IdentityStore identityStore) {
        this.queryBuilder = queryBuilder;
        this.identityStore = identityStore;
        this.identityType = identityType;
    }

    @Override
    public IdentityQuery<T> setParameter(QueryParameter queryParameter, Object... value) {
        if (value == null || value.length == 0) {
            throw new ModelException("Query Parameter values null or empty");
        }

        parameters.put(queryParameter, value);

        if (IdentityType.CREATED_AFTER.equals(queryParameter) || IdentityType.EXPIRY_AFTER.equals(queryParameter)) {
            this.conditions.add(queryBuilder.greaterThanOrEqualTo(queryParameter, value[0]));
        } else if (IdentityType.CREATED_BEFORE.equals(queryParameter) || IdentityType.EXPIRY_BEFORE.equals(queryParameter)) {
            this.conditions.add(queryBuilder.lessThanOrEqualTo(queryParameter, value[0]));
        } else {
            this.conditions.add(queryBuilder.equal(queryParameter, value[0]));
        }

        return this;
    }

    @Override
    public IdentityQuery<T> where(Condition... condition) {
        this.conditions.addAll(Arrays.asList(condition));
        return this;
    }

    @Override
    public IdentityQuery<T> sortBy(Sort... sorts) {
        this.ordering.addAll(Arrays.asList(sorts));
        return this;
    }

    @Override
    public Set<Sort> getSorting() {
        return unmodifiableSet(this.ordering);
    }

    @Override
    public Class<T> getIdentityType() {
        return identityType;
    }

    @Override
    public Map<QueryParameter, Object[]> getParameters() {
        return parameters;
    }

    @Override
    public Object[] getParameter(QueryParameter queryParameter) {
        return this.parameters.get(queryParameter);
    }

    @Override
    public Map<QueryParameter, Object[]> getParameters(Class<?> type) {
        Map<QueryParameter, Object[]> typedParameters = new HashMap<QueryParameter, Object[]>();

        Set<Map.Entry<QueryParameter, Object[]>> entrySet = this.parameters.entrySet();

        for (Map.Entry<QueryParameter, Object[]> entry : entrySet) {
            if (type.isInstance(entry.getKey())) {
                typedParameters.put(entry.getKey(), entry.getValue());
            }
        }

        return typedParameters;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public Object getPaginationContext() {
        return paginationContext;
    }

    @Override
    public QueryParameter[] getSortParameters() {
        return sortParameters;
    }

    @Override
    public boolean isSortAscending() {
        return sortAscending;
    }

    @Override
    public List<T> getResultList() {

        // remove this statement once deprecated methods on IdentityQuery are removed
        if (this.sortParameters != null) {
            for (QueryParameter parameter : this.sortParameters) {
                if (isSortAscending()) {
                    sortBy(this.queryBuilder.asc(parameter));
                } else {
                    sortBy(this.queryBuilder.desc(parameter));
                }
            }
        }

        List<T> result = new ArrayList<T>();

        try {
            for (T identityType : identityStore.fetchQueryResults(this)) {
                result.add(identityType);
            }
        } catch (Exception e) {
            throw new ModelException("LDAP Query failed", e);
        }

        return result;
    }

    @Override
    public int getResultCount() {
        return identityStore.countQueryResults(this);
    }

    @Override
    public IdentityQuery<T> setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public IdentityQuery<T> setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public IdentityQuery<T> setSortParameters(QueryParameter... sortParameters) {
        this.sortParameters = sortParameters;
        return this;
    }

    @Override
    public IdentityQuery<T> setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
        return this;
    }

    @Override
    public IdentityQuery<T> setPaginationContext(Object object) {
        this.paginationContext = object;
        return this;
    }

    @Override
    public Set<Condition> getConditions() {
        return unmodifiableSet(this.conditions);
    }
}
