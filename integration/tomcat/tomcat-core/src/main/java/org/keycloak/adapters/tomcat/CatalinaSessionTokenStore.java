package org.keycloak.adapters.tomcat;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CatalinaSessionTokenStore implements AdapterTokenStore {

    private static final Logger log = Logger.getLogger("" + CatalinaSessionTokenStore.class);

    private Request request;
    private KeycloakDeployment deployment;
    private CatalinaUserSessionManagement sessionManagement;
    protected GenericPrincipalFactory principalFactory;
    protected AbstractKeycloakAuthenticatorValve valve;


    public CatalinaSessionTokenStore(Request request, KeycloakDeployment deployment,
                                     CatalinaUserSessionManagement sessionManagement,
                                     GenericPrincipalFactory principalFactory,
                                     AbstractKeycloakAuthenticatorValve valve) {
        this.request = request;
        this.deployment = deployment;
        this.sessionManagement = sessionManagement;
        this.principalFactory = principalFactory;
        this.valve = valve;
    }

    @Override
    public void checkCurrentToken() {
        Session catalinaSession = request.getSessionInternal(false);
        if (catalinaSession == null) return;
        SerializableKeycloakAccount account = (SerializableKeycloakAccount) catalinaSession.getSession().getAttribute(SerializableKeycloakAccount.class.getName());
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
        log.fine("Cleanup and expire session " + catalinaSession.getId() + " after failed refresh");
        request.setUserPrincipal(null);
        request.setAuthType(null);
        cleanSession(catalinaSession);
        catalinaSession.expire();
    }

    protected void cleanSession(Session catalinaSession) {
        catalinaSession.getSession().removeAttribute(KeycloakAccount.class.getName());
        catalinaSession.setPrincipal(null);
        catalinaSession.setAuthType(null);
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        Session session = request.getSessionInternal(false);
        if (session == null) return false;
        SerializableKeycloakAccount account = (SerializableKeycloakAccount) session.getSession().getAttribute(SerializableKeycloakAccount.class.getName());
        if (account == null) {
            return false;
        }

        log.fine("remote logged in already. Establish state from session");

        RefreshableKeycloakSecurityContext securityContext = account.getKeycloakSecurityContext();

        if (!deployment.getRealm().equals(securityContext.getRealm())) {
            log.fine("Account from cookie is from a different realm than for the request.");
            cleanSession(session);
            return false;
        }

        securityContext.setCurrentRequestInfo(deployment, this);
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
        GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
        // in clustered environment in JBossWeb, principal is not serialized or saved
        if (principal == null) {
            principal = principalFactory.createPrincipal(request.getContext().getRealm(), account.getPrincipal(), account.getRoles(), securityContext);
            session.setPrincipal(principal);
            session.setAuthType("KEYCLOAK");

        }
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK");

        restoreRequest();
        return true;
    }

    public static class SerializableKeycloakAccount implements KeycloakAccount, Serializable {
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
    public void saveAccountInfo(KeycloakAccount account) {
        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext) account.getKeycloakSecurityContext();
        Set<String> roles = account.getRoles();
        GenericPrincipal principal = principalFactory.createPrincipal(request.getContext().getRealm(), account.getPrincipal(), roles, securityContext);

        SerializableKeycloakAccount sAccount = new SerializableKeycloakAccount(roles, account.getPrincipal(), securityContext);
        Session session = request.getSessionInternal(true);
        session.setPrincipal(principal);
        session.setAuthType("KEYCLOAK");
        session.getSession().setAttribute(SerializableKeycloakAccount.class.getName(), sAccount);
        String username = securityContext.getToken().getSubject();
        log.fine("userSessionManagement.login: " + username);
        this.sessionManagement.login(session);
    }

    @Override
    public void logout() {
        Session session = request.getSessionInternal(false);
        if (session != null) {
            cleanSession(session);
        }
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }

    @Override
    public void saveRequest() {
        try {
            valve.keycloakSaveRequest(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean restoreRequest() {
        return valve.keycloakRestoreRequest(request);
    }
}
