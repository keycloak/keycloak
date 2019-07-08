/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.tomcat;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.http.auth.AuthenticationException;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.AuthOutcome;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @version $Revision: 1 $
 */
public class CatalinaRequestAuthenticator extends RequestAuthenticator {
    private static final Logger log = Logger.getLogger("" + CatalinaRequestAuthenticator.class);
    protected Request request;
    protected GenericPrincipalFactory principalFactory;

    public CatalinaRequestAuthenticator(KeycloakDeployment deployment,
                                        AdapterTokenStore tokenStore,
                                        CatalinaHttpFacade facade,
                                        Request request,
                                        GenericPrincipalFactory principalFactory) {
        super(facade, deployment, tokenStore, request.getConnector().getRedirectPort());
        this.request = request;
        this.principalFactory = principalFactory;
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(this, facade, deployment, sslRedirectPort, tokenStore);
    }

    @Override
    public AuthOutcome authenticate() {
        String sessionNonce = nonce();

        AuthOutcome preCheck = super.authenticate(sessionNonce);
        if (preCheck != AuthOutcome.AUTHENTICATED) {
            return preCheck;
        }

        if (deployment.isUseNonce()) {
            return checkNonce();
        }

        return AuthOutcome.AUTHENTICATED;
    }

    /**
     * The concept of a signed request is a security enhancement to prevent valid requests
     * from being replied.
     * <p>
     * Replay of Authorized Resource Server Requests
     *
     * @return null if deactivated (default), else a secure random nonce string
     * @link https://tools.ietf.org/html/rfc6819#section-4.6.2
     * <p>
     * Signed Requests
     * @link https://tools.ietf.org/html/rfc6819#section-5.4.3
     * <p>
     * The implementation is off by default and can be enabled via configuration.
     * In this case nonce will be generated and stored in the catalina session for
     * later validation.
     */
    private String nonce() {
        if (!this.deployment.isUseNonce()) {
            return null;
        }
        Session catalinaSession = request.getSessionInternal(false);
        if (catalinaSession != null) {
            catalinaSession.access();
            final String sessionNonce = (String) catalinaSession.getSession().getAttribute("nonce");
            if (sessionNonce == null || sessionNonce.isEmpty()) {
                log.finest("generating nonce for auth session");
                String freshNonce = AdapterUtils.generateId();
                catalinaSession.getSession().setAttribute("nonce", freshNonce);
                return freshNonce;
            }
            log.finest("found existing nonce in current catalina session");
            catalinaSession.endAccess();
            return sessionNonce;
        }
        return null;
    }

    private AuthOutcome checkNonce() {
        try {
            CatalinaSessionTokenStore catalinaSessionTokenStore = (CatalinaSessionTokenStore) tokenStore;
            catalinaSessionTokenStore.checkTokenNonce();
            return AuthOutcome.AUTHENTICATED;
        } catch (AuthenticationException aue) {
            log.fine("failed to validate nonce: " + aue.getMessage());
            return AuthOutcome.FAILED;
        } catch (Exception e) {
            log.fine("error occured during nonce validation: " + e.getMessage());
            return AuthOutcome.FAILED;
        }
    }

    @Override
    protected void completeOAuthAuthentication(final KeycloakPrincipal<RefreshableKeycloakSecurityContext> skp) {
        final RefreshableKeycloakSecurityContext securityContext = skp.getKeycloakSecurityContext();
        final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
        OidcKeycloakAccount account = new OidcKeycloakAccount() {

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
        Principal generalPrincipal = principalFactory.createPrincipal(request.getContext().getRealm(), principal, roles);
        request.setUserPrincipal(generalPrincipal);
        request.setAuthType(method);
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
    }

    @Override
    protected String changeHttpSessionId(boolean create) {
        HttpSession session = request.getSession(create);
        return session != null ? session.getId() : null;
    }

}
