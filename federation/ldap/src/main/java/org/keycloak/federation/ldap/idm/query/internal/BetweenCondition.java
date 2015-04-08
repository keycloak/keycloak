package org.keycloak.federation.ldap.idm.query.internal;

import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;

/**
 * @author Pedro Igor
 */
public class BetweenCondition implements Condition {

    private final Comparable x;
    private final Comparable y;
    private final QueryParameter parameter;

    public BetweenCondition(QueryParameter parameter, Comparable x, Comparable y) {
        this.parameter = parameter;
        this.x = x;
        this.y = y;
    }

    @Override
    public QueryParameter getParameter() {
        return this.parameter;
    }

    public Comparable getX() {
        return this.x;
    }

    public Comparable getY() {
        return this.y;
    }
}
