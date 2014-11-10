package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.Request;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.CookieTokenStore;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * Handle storage of token info in cookie. Per-request object.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JettyCookieTokenStore implements AdapterTokenStore {

    private static final Logger log = Logger.getLogger(JettyCookieTokenStore.class);

    private Request request;
    private HttpFacade facade;
    private KeycloakDeployment deployment;

    private KeycloakPrincipal<RefreshableKeycloakSecurityContext> authenticatedPrincipal;

    public JettyCookieTokenStore(Request request, HttpFacade facade, KeycloakDeployment deployment) {
        this.request = request;
        this.facade = facade;
        this.deployment = deployment;
    }


    @Override
    public void checkCurrentToken() {
       this.authenticatedPrincipal = checkPrincipalFromCookie();
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        // Assuming authenticatedPrincipal set by previous call of checkCurrentToken() during this request
        if (authenticatedPrincipal != null) {
            log.debug("remote logged in already. Establish state from cookie");
            RefreshableKeycloakSecurityContext securityContext = authenticatedPrincipal.getKeycloakSecurityContext();

            if (!securityContext.getRealm().equals(deployment.getRealm())) {
                log.debug("Account from cookie is from a different realm than for the request.");
                return false;
            }

            securityContext.setCurrentRequestInfo(deployment, this);

            request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void saveAccountInfo(KeycloakAccount account) {
        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext)account.getKeycloakSecurityContext();
        CookieTokenStore.setTokenCookie(deployment, facade, securityContext);
    }

    @Override
    public void logout() {
        CookieTokenStore.removeCookie(facade);

    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext secContext) {
        CookieTokenStore.setTokenCookie(deployment, facade, secContext);
    }

    /**
     * Verify if we already have authenticated and active principal in cookie. Perform refresh if it's not active
     *
     * @return valid principal
     */
    protected KeycloakPrincipal<RefreshableKeycloakSecurityContext> checkPrincipalFromCookie() {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, facade, this);
        if (principal == null) {
            log.debug("Account was not in cookie or was invalid");
            return null;
        }

        RefreshableKeycloakSecurityContext session = principal.getKeycloakSecurityContext();

        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) return principal;
        boolean success = session.refreshExpiredToken(false);
        if (success && session.isActive()) return principal;

        log.debugf("Cleanup and expire cookie for user %s after failed refresh", principal.getName());
        CookieTokenStore.removeCookie(facade);
        return null;
    }
}
