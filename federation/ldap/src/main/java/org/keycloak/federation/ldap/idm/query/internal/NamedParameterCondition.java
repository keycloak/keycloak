package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class NamedParameterCondition implements Condition {

    private String parameterName;

    public NamedParameterCondition(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public String getParameterName() {
        return parameterName;
    }

    @Override
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
}
