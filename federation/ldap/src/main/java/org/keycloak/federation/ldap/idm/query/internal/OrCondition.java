package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class OrCondition implements Condition {

    private final Condition[] innerConditions;

    public OrCondition(Condition... innerConditions) {
        this.innerConditions = innerConditions;
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
        for (Condition innerCondition : innerConditions) {
            innerCondition.updateParameterName(modelParamName, ldapParamName);
        }
    }

    @Override
    public void applyCondition(StringBuilder filter) {
        filter.append("(|");

        for (Condition innerCondition : innerConditions) {
            innerCondition.applyCondition(filter);
        }

        filter.append(")");
    }
}
