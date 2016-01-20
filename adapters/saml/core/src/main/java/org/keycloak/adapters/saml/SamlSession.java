package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.KeycloakAccount;

import java.io.Serializable;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlSession implements Serializable, KeycloakAccount {
    private SamlPrincipal principal;
    private Set<String> roles;
    private String sessionIndex;

    public SamlSession() {
    }

    public SamlSession(SamlPrincipal principal, Set<String> roles, String sessionIndex) {
        this.principal = principal;
        this.roles = roles;
        this.sessionIndex = sessionIndex;
    }

    public SamlPrincipal getPrincipal() {
        return principal;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }
}
