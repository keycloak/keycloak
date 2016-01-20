package org.keycloak.adapters.springsecurity.token;

import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.security.Principal;
import java.util.Collection;

/**
 * Represents the token for a Keycloak authentication request or for an authenticated principal once the request has been
 * processed by the {@link AuthenticationManager#authenticate(Authentication)}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationToken extends AbstractAuthenticationToken implements Authentication {

    private Principal principal;

    /**
     * Creates a new, unauthenticated Keycloak security token for the given account.
     */
    public KeycloakAuthenticationToken(KeycloakAccount account) {
        super(null);
        Assert.notNull(account, "KeycloakAccount cannot be null");
        Assert.notNull(account.getPrincipal(), "KeycloakAccount.getPrincipal() cannot be null");
        this.principal = account.getPrincipal();
        this.setDetails(account);
    }

    public KeycloakAuthenticationToken(KeycloakAccount account, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        Assert.notNull(account, "KeycloakAccount cannot be null");
        Assert.notNull(account.getPrincipal(), "KeycloakAccount.getPrincipal() cannot be null");
        this.principal = account.getPrincipal();
        this.setDetails(account);
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.getAccount().getKeycloakSecurityContext();
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public OidcKeycloakAccount getAccount() {
        return (OidcKeycloakAccount) this.getDetails();
    }
}
