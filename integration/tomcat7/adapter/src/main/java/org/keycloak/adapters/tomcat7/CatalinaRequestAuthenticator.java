package org.keycloak.adapters.tomcat7;

import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.representations.AccessToken;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @version $Revision: 1 $
 */
public class CatalinaRequestAuthenticator extends RequestAuthenticator {
    private static final Logger log = Logger.getLogger(""+CatalinaRequestAuthenticator.class);
    protected KeycloakAuthenticatorValve valve;
    protected CatalinaUserSessionManagement userSessionManagement;
    protected Request request;

    public CatalinaRequestAuthenticator(KeycloakDeployment deployment,
                                        KeycloakAuthenticatorValve valve, CatalinaUserSessionManagement userSessionManagement,
                                        CatalinaHttpFacade facade,
                                        Request request) {
        super(facade, deployment, request.getConnector().getRedirectPort());
        this.valve = valve;
        this.userSessionManagement = userSessionManagement;
        this.request = request;
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(facade, deployment, sslRedirectPort) {
            @Override
            protected void saveRequest() {
                try {
                    valve.keycloakSaveRequest(request);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    protected void completeOAuthAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> skp) {
        RefreshableKeycloakSecurityContext securityContext = skp.getKeycloakSecurityContext();
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
    	Set<String> roles = getRolesFromToken(securityContext);
        GenericPrincipal principal = new CatalinaSecurityContextHelper().createPrincipal(request.getContext().getRealm(), skp, roles, securityContext);
        Session session = request.getSessionInternal(true);
        session.setPrincipal(principal);
        session.setAuthType("OAUTH");
        session.setNote(KeycloakSecurityContext.class.getName(), securityContext);
        String username = securityContext.getToken().getSubject();
        log.finer("userSessionManage.login: " + username);
        userSessionManagement.login(session, username, securityContext.getToken().getSessionState());
    }

    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        RefreshableKeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();
        Set<String> roles = getRolesFromToken(securityContext);
        for (String role : roles) {
            log.info("Bearer role: " + role);
        }
        Principal generalPrincipal = new CatalinaSecurityContextHelper().createPrincipal(request.getContext().getRealm(), principal, roles, securityContext);
        request.setUserPrincipal(generalPrincipal);
        request.setAuthType("KEYCLOAK");
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
    }

    protected Set<String> getRolesFromToken(RefreshableKeycloakSecurityContext session) {
        Set<String> roles = null;
        if (deployment.isUseResourceRoleMappings()) {
            AccessToken.Access access = session.getToken().getResourceAccess(deployment.getResourceName());
            if (access != null) roles = access.getRoles();
        } else {
            AccessToken.Access access =  session.getToken().getRealmAccess();
            if (access != null) roles = access.getRoles();
        }
        if (roles == null) roles = Collections.emptySet();
        return roles;
    }

    @Override
    protected boolean isCached() {
        if (request.getSessionInternal(false) == null || request.getSessionInternal().getPrincipal() == null)
            return false;
        log.finer("remote logged in already");
        GenericPrincipal principal = (GenericPrincipal) request.getSessionInternal().getPrincipal();
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK");
        Session session = request.getSessionInternal();
        if (session != null) {
            RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext) session.getNote(KeycloakSecurityContext.class.getName());
            if (securityContext != null) {
                securityContext.setDeployment(deployment);
                request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
            }
        }
        restoreRequest();
        return true;
    }

    protected void restoreRequest() {
        if (request.getSessionInternal().getNote(Constants.FORM_REQUEST_NOTE) != null) {
            if (valve.keycloakRestoreRequest(request)) {
                log.finer("restoreRequest");
            } else {
                log.finer("Restore of original request failed");
                throw new RuntimeException("Restore of original request failed");
            }
        }
    }
}
