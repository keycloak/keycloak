package org.keycloak.federation.ldap.idm.query.internal;

import java.util.Date;

import org.keycloak.federation.ldap.idm.store.ldap.LDAPUtil;

/**
 * @author Pedro Igor
 */
class BetweenCondition extends NamedParameterCondition {

    private final Comparable x;
    private final Comparable y;

    public BetweenCondition(String name, Comparable x, Comparable y) {
        super(name);
        this.x = x;
        this.y = y;
    }

    @Override
    public void applyCondition(StringBuilder filter) {
        Comparable x = this.x;
        Comparable y = this.y;

        if (Date.class.isInstance(x)) {
            x = LDAPUtil.formatDate((Date) x);
        }

        if (Date.class.isInstance(y)) {
            y = LDAPUtil.formatDate((Date) y);
        }

        filter.append("(").append(x).append("<=").append(getParameterName()).append("<=").append(y).append(")");
    }
}
