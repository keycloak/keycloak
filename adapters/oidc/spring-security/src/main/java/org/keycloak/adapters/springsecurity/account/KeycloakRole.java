package org.keycloak.adapters.springsecurity.account;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 * Represents an authority granted to an {@link Authentication} by the Keycloak server.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakRole implements GrantedAuthority {

    private String role;

    /**
     * Creates a new granted authority from the given Keycloak role.
     *
     * @param role the name of this granted authority
     */
    public KeycloakRole(String role) {
        Assert.notNull(role, "role cannot be null");
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeycloakRole)) {
            return false;
        }

        KeycloakRole that = (KeycloakRole) o;

        if (!role.equals(that.role)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 3 * role.hashCode();
    }

    @Override
    public String toString() {
        return "KeycloakRole{" +
                "role='" + role + '\'' +
                '}';
    }

}
