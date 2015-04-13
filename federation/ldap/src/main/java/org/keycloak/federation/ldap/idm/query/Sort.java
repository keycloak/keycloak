package org.keycloak.federation.ldap.idm.query;

/**
 * @author Pedro Igor
 */
public class Sort {

    private final QueryParameter parameter;
    private final boolean asc;

    public Sort(QueryParameter parameter, boolean asc) {
        this.parameter = parameter;
        this.asc = asc;
    }

    public QueryParameter getParameter() {
        return this.parameter;
    }

    public boolean isAscending() {
        return asc;
    }
}
