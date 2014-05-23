package org.keycloak;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakPrincipal implements Principal, Serializable {
    protected final String name;
    protected final KeycloakSecurityContext context;

    public KeycloakPrincipal(String name, KeycloakSecurityContext context) {
        this.name = name;
        this.context = context;
    }

    public KeycloakSecurityContext getKeycloakSecurityContext() {
        return context;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeycloakPrincipal that = (KeycloakPrincipal) o;

        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
