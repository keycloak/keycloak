package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.IdentityQuery;
import org.keycloak.federation.ldap.idm.query.IdentityQueryBuilder;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.Sort;
import org.keycloak.federation.ldap.idm.store.IdentityStore;
import org.keycloak.models.ModelException;

/**
 * @author Pedro Igor
 */
public class DefaultQueryBuilder implements IdentityQueryBuilder {

    private final IdentityStore identityStore;

    public DefaultQueryBuilder(IdentityStore identityStore) {
        this.identityStore = identityStore;
    }

    @Override
    public Condition like(QueryParameter parameter, String pattern) {
        return new LikeCondition(parameter, pattern);
    }

    @Override
    public Condition equal(QueryParameter parameter, Object value) {
        return new EqualCondition(parameter, value);
    }

    @Override
    public Condition greaterThan(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(parameter, (Comparable) x, false);
    }

    @Override
    public Condition greaterThanOrEqualTo(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(parameter, (Comparable) x, true);
    }

    @Override
    public Condition lessThan(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new LessThanCondition(parameter, (Comparable) x, false);
    }

    @Override
    public Condition lessThanOrEqualTo(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new LessThanCondition(parameter, (Comparable) x, true);
    }

    @Override
    public Condition between(QueryParameter parameter, Object x, Object y) {
        throwExceptionIfNotComparable(x);
        throwExceptionIfNotComparable(y);
        return new BetweenCondition(parameter, (Comparable) x, (Comparable) y);
    }

    @Override
    public Condition in(QueryParameter parameter, Object... x) {
        return new InCondition(parameter, x);
    }

    @Override
    public Sort asc(QueryParameter parameter) {
        return new Sort(parameter, true);
    }

    @Override
    public Sort desc(QueryParameter parameter) {
        return new Sort(parameter, false);
    }

    @Override
    public <T extends IdentityType> IdentityQuery createIdentityQuery(Class<T> identityType) {
        return new DefaultIdentityQuery(this, identityType, this.identityStore);
    }

    private void throwExceptionIfNotComparable(Object x) {
        if (!Comparable.class.isInstance(x)) {
            throw new ModelException("Query parameter value [" + x + "] must be " + Comparable.class + ".");
        }
    }
}