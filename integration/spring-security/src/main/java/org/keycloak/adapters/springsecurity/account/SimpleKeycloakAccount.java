package org.keycloak.adapters.springsecurity.account;

import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

/**
 * Concrete, serializable {@link KeycloakAccount} implementation.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class SimpleKeycloakAccount implements KeycloakAccount, Serializable {

    private Set<String> roles;
    private Principal principal;
    private RefreshableKeycloakSecurityContext securityContext;

    public SimpleKeycloakAccount(Principal principal, Set<String> roles,  RefreshableKeycloakSecurityContext securityContext) {
        this.principal = principal;
        this.roles = roles;
        this.securityContext = securityContext;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public RefreshableKeycloakSecurityContext getKeycloakSecurityContext() {
        return securityContext;
    }
}
