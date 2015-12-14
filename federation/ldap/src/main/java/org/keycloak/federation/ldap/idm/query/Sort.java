package org.keycloak.federation.ldap.idm.query;

/**
 * @author Pedro Igor
 */
public class Sort {

    private final String paramName;
    private final boolean asc;

    public Sort(String paramName, boolean asc) {
        this.paramName = paramName;
        this.asc = asc;
    }

    public String getParameter() {
        return this.paramName;
    }

    public boolean isAscending() {
        return asc;
    }
}
