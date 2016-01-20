package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.CookieTokenStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * Per-request object. Storage of tokens in cookie
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UndertowCookieTokenStore implements AdapterTokenStore {

    protected static Logger log = Logger.getLogger(UndertowCookieTokenStore.class);

    private final HttpFacade facade;
    private final KeycloakDeployment deployment;
    private final SecurityContext securityContext;

    public UndertowCookieTokenStore(HttpFacade facade, KeycloakDeployment deployment,
                                    SecurityContext securityContext) {
        this.facade = facade;
        this.deployment = deployment;
        this.securityContext = securityContext;
    }

    @Override
    public void checkCurrentToken() {
        // no-op on undertow
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, facade, this);
        if (principal == null) {
            log.debug("Account was not in cookie or was invalid, returning null");
            return false;
        }
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal);

        if (!deployment.getRealm().equals(account.getKeycloakSecurityContext().getRealm())) {
            log.debug("Account in session belongs to a different realm than for this request.");
            return false;
        }

        if (account.checkActive()) {
            log.debug("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            ((AbstractUndertowRequestAuthenticator)authenticator).propagateKeycloakContext(account);
            return true;
        } else {
            log.debug("Account was not active, removing cookie and returning false");
            CookieTokenStore.removeCookie(facade);
            return false;
        }
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        RefreshableKeycloakSecurityContext secContext = (RefreshableKeycloakSecurityContext)account.getKeycloakSecurityContext();
        CookieTokenStore.setTokenCookie(deployment, facade, secContext);
    }

    @Override
    public void logout() {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, facade, this);
        if (principal == null) return;

        CookieTokenStore.removeCookie(facade);
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        CookieTokenStore.setTokenCookie(deployment, facade, securityContext);
    }

    @Override
    public void saveRequest() {

    }

    @Override
    public boolean restoreRequest() {
        return false;
    }
}
