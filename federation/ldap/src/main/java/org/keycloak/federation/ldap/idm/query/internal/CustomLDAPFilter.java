package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class CustomLDAPFilter implements Condition {

    private final String customFilter;

    public CustomLDAPFilter(String customFilter) {
        this.customFilter = customFilter;
    }

    @Override
    public String getParameterName() {
        return null;
    }

    @Override
    public void setParameterName(String parameterName) {
    }

    @Override
    public void updateParameterName(String modelParamName, String ldapParamName) {

    }

    @Override
    public void applyCondition(StringBuilder filter) {
        filter.append(customFilter);
    }
}
