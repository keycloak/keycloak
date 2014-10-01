package org.keycloak;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakPrincipal<T extends KeycloakSecurityContext> implements Principal, Serializable {
    protected final String name;
    protected final T context;

    public KeycloakPrincipal(String name, T context) {
        this.name = name;
        this.context = context;
    }

    public T getKeycloakSecurityContext() {
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
