package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OrCondition implements Condition {

    private final Condition[] innerConditions;

    public OrCondition(Condition... innerConditions) {
        this.innerConditions = innerConditions;
    }

    public Condition[] getInnerConditions() {
        return innerConditions;
    }

    @Override
    public QueryParameter getParameter() {
        return null;
    }
}
