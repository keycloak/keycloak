package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;

/**
 * @author Pedro Igor
 */
public class InCondition implements Condition {

    private final QueryParameter parameter;
    private final Object[] value;

    public InCondition(QueryParameter parameter, Object[] value) {
        this.parameter = parameter;
        this.value = value;
    }

    @Override
    public QueryParameter getParameter() {
        return this.parameter;
    }

    public Object[] getValue() {
        return this.value;
    }
}

