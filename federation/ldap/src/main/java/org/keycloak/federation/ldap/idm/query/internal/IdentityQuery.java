package org.keycloak.federation.ldap.idm.query.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.Sort;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.models.ModelException;

import static java.util.Collections.unmodifiableSet;

/**
 * Default IdentityQuery implementation.
 *
 * @param <T>
 *
 * @author Shane Bryzak
 */
public class IdentityQuery<T extends IdentityType> {

    private final Map<QueryParameter, Object[]> parameters = new LinkedHashMap<QueryParameter, Object[]>();
    private final Class<T> identityType;
    private final LDAPIdentityStore identityStore;
    private final IdentityQueryBuilder queryBuilder;
    private int offset;
    private int limit;
    private Object paginationContext;
    private QueryParameter[] sortParameters;
    private boolean sortAscending = true;
    private final Set<Condition> conditions = new LinkedHashSet<Condition>();
    private final Set<Sort> ordering = new LinkedHashSet<Sort>();

    public IdentityQuery(IdentityQueryBuilder queryBuilder, Class<T> identityType, LDAPIdentityStore identityStore) {
        this.queryBuilder = queryBuilder;
        this.identityStore = identityStore;
        this.identityType = identityType;
    }

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

    public IdentityQuery<T> where(Condition... condition) {
        this.conditions.addAll(Arrays.asList(condition));
        return this;
    }

    public IdentityQuery<T> sortBy(Sort... sorts) {
        this.ordering.addAll(Arrays.asList(sorts));
        return this;
    }

    public Set<Sort> getSorting() {
        return unmodifiableSet(this.ordering);
    }

    public Class<T> getIdentityType() {
        return identityType;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public Object getPaginationContext() {
        return paginationContext;
    }

    public QueryParameter[] getSortParameters() {
        return sortParameters;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

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

    public int getResultCount() {
        return identityStore.countQueryResults(this);
    }

    public IdentityQuery<T> setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public IdentityQuery<T> setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public IdentityQuery<T> setSortParameters(QueryParameter... sortParameters) {
        this.sortParameters = sortParameters;
        return this;
    }

    public IdentityQuery<T> setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
        return this;
    }

    public IdentityQuery<T> setPaginationContext(Object object) {
        this.paginationContext = object;
        return this;
    }

    public Set<Condition> getConditions() {
        return unmodifiableSet(this.conditions);
    }
}
