package org.keycloak.adapters.tomcat;

import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.enums.TokenStore;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @version $Revision: 1 $
 */
public class CatalinaRequestAuthenticator extends RequestAuthenticator {
    private static final Logger log = Logger.getLogger(""+CatalinaRequestAuthenticator.class);
    protected AbstractKeycloakAuthenticatorValve valve;
    protected Request request;
    protected GenericPrincipalFactory principalFactory;

    public CatalinaRequestAuthenticator(KeycloakDeployment deployment,
                                        AbstractKeycloakAuthenticatorValve valve, AdapterTokenStore tokenStore,
                                        CatalinaHttpFacade facade,
                                        Request request,
                                        GenericPrincipalFactory principalFactory) {
        super(facade, deployment, tokenStore, request.getConnector().getRedirectPort());
        this.valve = valve;
        this.request = request;
        this.principalFactory = principalFactory;
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(this, facade, deployment, sslRedirectPort) {
            @Override
            protected void saveRequest() {
                try {
                    // Support saving request just for TokenStore.SESSION TODO: Add to tokenStore spi?
                    if (deployment.getTokenStore() == TokenStore.SESSION) {
                        valve.keycloakSaveRequest(request);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    protected void completeOAuthAuthentication(final KeycloakPrincipal<RefreshableKeycloakSecurityContext> skp) {
        final RefreshableKeycloakSecurityContext securityContext = skp.getKeycloakSecurityContext();
        final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
        KeycloakAccount account = new KeycloakAccount() {

            @Override
            public Principal getPrincipal() {
                return skp;
            }

            @Override
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            public KeycloakSecurityContext getKeycloakSecurityContext() {
                return securityContext;
            }

        };

        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
        this.tokenStore.saveAccountInfo(account);
    }

    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, String method) {
        RefreshableKeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();
        Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
        if (log.isLoggable(Level.FINE)) {
            log.fine("Completing bearer authentication. Bearer roles: " + roles);
        }
        Principal generalPrincipal = principalFactory.createPrincipal(request.getContext().getRealm(), principal, roles, securityContext);
        request.setUserPrincipal(generalPrincipal);
        request.setAuthType(method);
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
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

    @Override
    protected String getHttpSessionId(boolean create) {
        HttpSession session = request.getSession(create);
        return session != null ? session.getId() : null;
    }
}
