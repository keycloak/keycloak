package org.keycloak.adapters.as7;

import java.util.Set;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * Handle storage of token info in HTTP Session. Per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CatalinaSessionTokenStore implements AdapterTokenStore {

    private static final Logger log = Logger.getLogger(CatalinaSessionTokenStore.class);

    private Request request;
    private KeycloakDeployment deployment;
    private CatalinaUserSessionManagement sessionManagement;

    public CatalinaSessionTokenStore(Request request, KeycloakDeployment deployment, CatalinaUserSessionManagement sessionManagement) {
        this.request = request;
        this.deployment = deployment;
        this.sessionManagement = sessionManagement;
    }

    @Override
    public void checkCurrentToken() {
        if (request.getSessionInternal(false) == null || request.getSessionInternal().getPrincipal() == null) return;
        RefreshableKeycloakSecurityContext session = (RefreshableKeycloakSecurityContext) request.getSessionInternal().getNote(KeycloakSecurityContext.class.getName());
        if (session == null) return;

        // just in case session got serialized
        if (session.getDeployment() == null) session.setCurrentRequestInfo(deployment, this);

        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        boolean success = session.refreshExpiredToken(false);
        if (success && session.isActive()) return;

        // Refresh failed, so user is already logged out from keycloak. Cleanup and expire our session
        Session catalinaSession = request.getSessionInternal();
        log.debugf("Cleanup and expire session %s after failed refresh", catalinaSession.getId());
        catalinaSession.removeNote(KeycloakSecurityContext.class.getName());
        request.setUserPrincipal(null);
        request.setAuthType(null);
        catalinaSession.setPrincipal(null);
        catalinaSession.setAuthType(null);
        catalinaSession.expire();
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        if (request.getSessionInternal(false) == null || request.getSessionInternal().getPrincipal() == null)
            return false;
        log.debug("remote logged in already. Establish state from session");

        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext) request.getSessionInternal().getNote(KeycloakSecurityContext.class.getName());
        if (securityContext != null) {

            if (!deployment.getRealm().equals(securityContext.getRealm())) {
                log.debug("Account from cookie is from a different realm than for the request.");
                return false;
            }

            securityContext.setCurrentRequestInfo(deployment, this);
            request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
        }

        GenericPrincipal principal = (GenericPrincipal) request.getSessionInternal().getPrincipal();
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK");

        ((CatalinaRequestAuthenticator)authenticator).restoreRequest();
        return true;
    }

    @Override
    public void saveAccountInfo(KeycloakAccount account) {
        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext)account.getKeycloakSecurityContext();
        Set<String> roles = account.getRoles();
        GenericPrincipal principal = new CatalinaSecurityContextHelper().createPrincipal(request.getContext().getRealm(), account.getPrincipal(), roles, securityContext);

        Session session = request.getSessionInternal(true);
        session.setPrincipal(principal);
        session.setAuthType("OAUTH");
        session.setNote(KeycloakSecurityContext.class.getName(), securityContext);
        String username = securityContext.getToken().getSubject();
        log.debug("userSessionManagement.login: " + username);
        this.sessionManagement.login(session);
    }

    @Override
    public void logout() {
        Session session = request.getSessionInternal(false);
        if (session != null) {
            session.removeNote(KeycloakSecurityContext.class.getName());
        }
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }
}
