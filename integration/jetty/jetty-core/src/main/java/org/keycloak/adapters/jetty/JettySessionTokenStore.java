package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.Request;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

import javax.servlet.http.HttpSession;

/**
 * Handle storage of token info in HTTP Session. Per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JettySessionTokenStore implements AdapterTokenStore {

    private static final Logger log = Logger.getLogger(JettySessionTokenStore.class);

    private Request request;
    private KeycloakDeployment deployment;

    public JettySessionTokenStore(Request request, KeycloakDeployment deployment) {
        this.request = request;
        this.deployment = deployment;
    }

    @Override
    public void checkCurrentToken() {
        if (request.getSession(false) == null) return;
        RefreshableKeycloakSecurityContext session = (RefreshableKeycloakSecurityContext) request.getSession().getAttribute(KeycloakSecurityContext.class.getName());
        if (session == null) return;

        // just in case session got serialized
        if (session.getDeployment() == null) session.setCurrentRequestInfo(deployment, this);

        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        boolean success = session.refreshExpiredToken(false);
        if (success && session.isActive()) return;

        // Refresh failed, so user is already logged out from keycloak. Cleanup and expire our session
        request.getSession().removeAttribute(KeycloakSecurityContext.class.getName());
        request.getSession().invalidate();
     }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        if (request.getSession(false) == null || request.getSession().getAttribute(KeycloakSecurityContext.class.getName()) == null)
            return false;
        log.debug("remote logged in already. Establish state from session");

        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext) request.getSession().getAttribute(KeycloakSecurityContext.class.getName());
        if (!deployment.getRealm().equals(securityContext.getRealm())) {
            log.debug("Account from cookie is from a different realm than for the request.");
            return false;
        }

        securityContext.setCurrentRequestInfo(deployment, this);
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);

        AbstractJettyRequestAuthenticator jettyAuthenticator = (AbstractJettyRequestAuthenticator) authenticator;
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = AdapterUtils.createPrincipal(deployment, securityContext);
        jettyAuthenticator.principal = principal;
        jettyAuthenticator.restoreRequest();
        return true;
    }

    @Override
    public void saveAccountInfo(KeycloakAccount account) {
        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext)account.getKeycloakSecurityContext();
        request.getSession().setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
    }

    @Override
    public void logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(KeycloakSecurityContext.class.getName());
        }
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }
}
