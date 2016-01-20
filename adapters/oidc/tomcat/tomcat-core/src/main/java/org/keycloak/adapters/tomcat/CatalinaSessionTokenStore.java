package org.keycloak.adapters.tomcat;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CatalinaSessionTokenStore extends CatalinaAdapterSessionStore implements AdapterTokenStore {

    private static final Logger log = Logger.getLogger("" + CatalinaSessionTokenStore.class);

    private KeycloakDeployment deployment;
    private CatalinaUserSessionManagement sessionManagement;
    protected GenericPrincipalFactory principalFactory;


    public CatalinaSessionTokenStore(Request request, KeycloakDeployment deployment,
                                     CatalinaUserSessionManagement sessionManagement,
                                     GenericPrincipalFactory principalFactory,
                                     AbstractKeycloakAuthenticatorValve valve) {
        super(request, valve);
        this.deployment = deployment;
        this.sessionManagement = sessionManagement;
        this.principalFactory = principalFactory;
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
        catalinaSession.getSession().removeAttribute(OidcKeycloakAccount.class.getName());
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
            principal = principalFactory.createPrincipal(request.getContext().getRealm(), account.getPrincipal(), account.getRoles());
            session.setPrincipal(principal);
            session.setAuthType("KEYCLOAK");

        }
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK");

        restoreRequest();
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
        GenericPrincipal principal = principalFactory.createPrincipal(request.getContext().getRealm(), account.getPrincipal(), roles);

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

}
