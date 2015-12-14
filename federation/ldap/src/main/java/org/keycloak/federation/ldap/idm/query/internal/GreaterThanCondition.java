package org.keycloak.federation.ldap.idm.query.internal;

import java.util.Date;

import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPUtil;

/**
 * @author Pedro Igor
 */
class GreaterThanCondition extends NamedParameterCondition {

    private final boolean orEqual;

    private final Comparable value;

    public GreaterThanCondition(String name, Comparable value, boolean orEqual) {
        super(name);
        this.value = value;
        this.orEqual = orEqual;
    }

    @Override
    public void applyCondition(StringBuilder filter) {
        Comparable parameterValue = value;

        if (Date.class.isInstance(parameterValue)) {
            parameterValue = LDAPUtil.formatDate((Date) parameterValue);
        }

        if (orEqual) {
            filter.append("(").append(getParameterName()).append(">=").append(parameterValue).append(")");
        } else {
            filter.append("(").append(getParameterName()).append(">").append(parameterValue).append(")");
        }
    }
}