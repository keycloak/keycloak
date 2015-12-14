package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.models.LDAPConstants;

/**
 * @author Pedro Igor
 */
class InCondition extends NamedParameterCondition {

    private final Object[] valuesToCompare;

    public InCondition(String name, Object[] valuesToCompare) {
        super(name);
        this.valuesToCompare = valuesToCompare;
    }

    @Override
    public void applyCondition(StringBuilder filter) {

        filter.append("(&(");

        for (int i = 0; i< valuesToCompare.length; i++) {
            Object value = valuesToCompare[i];

            filter.append("(").append(getParameterName()).append(LDAPConstants.EQUAL).append(value).append(")");
        }

        filter.append("))");
    }
}

