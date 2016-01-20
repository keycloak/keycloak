package org.keycloak.adapters.servlet;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.SessionIdMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCFilterSessionStore extends FilterSessionStore implements AdapterTokenStore {
    protected final KeycloakDeployment deployment;
    private static final Logger log = Logger.getLogger("" + OIDCFilterSessionStore.class);
    protected final SessionIdMapper idMapper;

    public OIDCFilterSessionStore(HttpServletRequest request, HttpFacade facade, int maxBuffer, KeycloakDeployment deployment, SessionIdMapper idMapper) {
        super(request, facade, maxBuffer);
        this.deployment = deployment;
        this.idMapper = idMapper;
    }

    public HttpServletRequestWrapper buildWrapper() {
        HttpSession session = request.getSession();
        KeycloakAccount account = (KeycloakAccount)session.getAttribute((KeycloakAccount.class.getName()));
        return buildWrapper(session, account);
    }

    @Override
    public void checkCurrentToken() {
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) return;
        SerializableKeycloakAccount account = (SerializableKeycloakAccount)httpSession.getAttribute(KeycloakAccount.class.getName());
        if (account == null) {
            return;
        }

        RefreshableKeycloakSecurityContext session = account.getKeycloakSecurityContext();
        if (session == null) return;

        // just in case session got serialized
        if (session.getDeployment() == null) session.setCurrentRequestInfo(deployment, this);

        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        boolean success = session.refreshExpiredToken(false);
        if (success && session.isActive()) return;

        // Refresh failed, so user is already logged out from keycloak. Cleanup and expire our session
        //log.fine("Cleanup and expire session " + httpSession.getId() + " after failed refresh");
        cleanSession(httpSession);
        httpSession.invalidate();
    }

    protected void cleanSession(HttpSession session) {
        session.removeAttribute(KeycloakAccount.class.getName());
        clearSavedRequest(session);
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) return false;
        SerializableKeycloakAccount account = (SerializableKeycloakAccount) httpSession.getAttribute(KeycloakAccount.class.getName());
        if (account == null) {
            return false;
        }

        log.fine("remote logged in already. Establish state from session");

        RefreshableKeycloakSecurityContext securityContext = account.getKeycloakSecurityContext();

        if (!deployment.getRealm().equals(securityContext.getRealm())) {
            log.fine("Account from cookie is from a different realm than for the request.");
            cleanSession(httpSession);
            return false;
        }

        if (idMapper != null && !idMapper.hasSession(httpSession.getId())) {
            cleanSession(httpSession);
            return false;
        }


        securityContext.setCurrentRequestInfo(deployment, this);
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
        needRequestRestore = restoreRequest();
        return true;
    }

    public static class SerializableKeycloakAccount implements OidcKeycloakAccount, Serializable {
        protected Set<String> roles;
        protected Principal principal;
        protected RefreshableKeycloakSecurityContext securityContext;

        public SerializableKeycloakAccount(Set<String> roles, Principal principal, RefreshableKeycloakSecurityContext securityContext) {
            this.roles = roles;
            this.principal = principal;
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

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext) account.getKeycloakSecurityContext();
        Set<String> roles = account.getRoles();

        SerializableKeycloakAccount sAccount = new SerializableKeycloakAccount(roles, account.getPrincipal(), securityContext);
        HttpSession httpSession = request.getSession();
        httpSession.setAttribute(KeycloakAccount.class.getName(), sAccount);
        if (idMapper != null) idMapper.map(account.getKeycloakSecurityContext().getToken().getClientSession(),  account.getPrincipal().getName(), httpSession.getId());
        //String username = securityContext.getToken().getSubject();
        //log.fine("userSessionManagement.login: " + username);
    }

    @Override
    public void logout() {
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            SerializableKeycloakAccount account = (SerializableKeycloakAccount) httpSession.getAttribute(KeycloakAccount.class.getName());
            if (account != null) {
                account.getKeycloakSecurityContext().logout(deployment);
            }
            cleanSession(httpSession);
        }
    }

    @Override
    public void servletRequestLogout() {
        logout();
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }
}
