package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.Sort;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.models.ModelException;

/**
 * @author Pedro Igor
 */
public class IdentityQueryBuilder {

    private final LDAPIdentityStore identityStore;

    public IdentityQueryBuilder(LDAPIdentityStore identityStore) {
        this.identityStore = identityStore;
    }

    public Condition like(QueryParameter parameter, String pattern) {
        return new LikeCondition(parameter, pattern);
    }

    public Condition equal(QueryParameter parameter, Object value) {
        return new EqualCondition(parameter, value);
    }

    public Condition greaterThan(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(parameter, (Comparable) x, false);
    }

    public Condition greaterThanOrEqualTo(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(parameter, (Comparable) x, true);
    }

    public Condition lessThan(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new LessThanCondition(parameter, (Comparable) x, false);
    }

    public Condition lessThanOrEqualTo(QueryParameter parameter, Object x) {
        throwExceptionIfNotComparable(x);
        return new LessThanCondition(parameter, (Comparable) x, true);
    }

    public Condition between(QueryParameter parameter, Object x, Object y) {
        throwExceptionIfNotComparable(x);
        throwExceptionIfNotComparable(y);
        return new BetweenCondition(parameter, (Comparable) x, (Comparable) y);
    }

    public Condition orCondition(Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            throw new ModelException("At least one condition should be provided to OR query");
        }
        return new OrCondition(conditions);
    }

    public Condition in(QueryParameter parameter, Object... x) {
        return new InCondition(parameter, x);
    }

    public Sort asc(QueryParameter parameter) {
        return new Sort(parameter, true);
    }

    public Sort desc(QueryParameter parameter) {
        return new Sort(parameter, false);
    }

    public <T extends IdentityType> IdentityQuery createIdentityQuery(Class<T> identityType) {
        return new IdentityQuery(this, identityType, this.identityStore);
    }

    private void throwExceptionIfNotComparable(Object x) {
        if (!Comparable.class.isInstance(x)) {
            throw new ModelException("Query parameter value [" + x + "] must be " + Comparable.class + ".");
        }
    }
}