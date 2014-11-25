package org.keycloak.adapters.as7;

import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.jboss.logging.Logger;
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

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaRequestAuthenticator extends RequestAuthenticator {

    private static final Logger log = Logger.getLogger(CatalinaRequestAuthenticator.class);
    protected KeycloakAuthenticatorValve valve;
    protected Request request;

    public CatalinaRequestAuthenticator(KeycloakDeployment deployment,
                                        KeycloakAuthenticatorValve valve, AdapterTokenStore tokenStore,
                                        CatalinaHttpFacade facade,
                                        Request request) {
        super(facade, deployment, tokenStore, request.getConnector().getRedirectPort());
        this.valve = valve;
        this.request = request;
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
        if (log.isDebugEnabled()) {
            log.debug("Completing bearer authentication. Bearer roles: " + roles);
        }
        Principal generalPrincipal = new CatalinaSecurityContextHelper().createPrincipal(request.getContext().getRealm(), principal, roles, securityContext);
        request.setUserPrincipal(generalPrincipal);
        request.setAuthType(method);
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
    }

    protected void restoreRequest() {
        if (request.getSessionInternal().getNote(Constants.FORM_REQUEST_NOTE) != null) {
            if (valve.keycloakRestoreRequest(request)) {
                log.debug("restoreRequest");
            } else {
                log.debug("Restore of original request failed");
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
