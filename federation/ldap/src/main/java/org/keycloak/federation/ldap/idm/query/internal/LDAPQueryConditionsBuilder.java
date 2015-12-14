package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.Sort;
import org.keycloak.models.ModelException;

/**
 * @author Pedro Igor
 */
public class LDAPQueryConditionsBuilder {

    public Condition equal(String parameter, Object value) {
        return new EqualCondition(parameter, value);
    }

    public Condition greaterThan(String paramName, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(paramName, (Comparable) x, false);
    }

    public Condition greaterThanOrEqualTo(String paramName, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(paramName, (Comparable) x, true);
    }

    public Condition lessThan(String paramName, Comparable x) {
        return new LessThanCondition(paramName, x, false);
    }

    public Condition lessThanOrEqualTo(String paramName, Comparable x) {
        return new LessThanCondition(paramName, x, true);
    }

    public Condition between(String paramName, Comparable x, Comparable y) {
        return new BetweenCondition(paramName, x, y);
    }

    public Condition orCondition(Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            throw new ModelException("At least one condition should be provided to OR query");
        }
        return new OrCondition(conditions);
    }

    public Condition addCustomLDAPFilter(String filter) {
        filter = filter.trim();
        if (!filter.startsWith("(") || !filter.endsWith(")")) {
            throw new ModelException("Custom filter doesn't start with ( or doesn't end with ). ");
        }
        return new CustomLDAPFilter(filter);
    }

    public Condition in(String paramName, Object... x) {
        return new InCondition(paramName, x);
    }

    public Sort asc(String paramName) {
        return new Sort(paramName, true);
    }

    public Sort desc(String paramName) {
        return new Sort(paramName, false);
    }

    private void throwExceptionIfNotComparable(Object x) {
        if (!Comparable.class.isInstance(x)) {
            throw new ModelException("Query parameter value [" + x + "] must be " + Comparable.class + ".");
        }
    }
}