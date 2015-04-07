package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;

/**
 * @author Pedro Igor
 */
public class LessThanCondition implements Condition {

    private final boolean orEqual;

    private final QueryParameter parameter;
    private final Comparable value;

    public LessThanCondition(QueryParameter parameter, Comparable value, boolean orEqual) {
        this.parameter = parameter;
        this.value = value;
        this.orEqual = orEqual;
    }

    @Override
    public QueryParameter getParameter() {
        return this.parameter;
    }

    public Comparable getValue() {
        return this.value;
    }

    public boolean isOrEqual() {
        return this.orEqual;
    }
}
