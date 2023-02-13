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

package org.keycloak.adapters.springsecurity.authentication;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Set;

/**
 * Request authenticator adapter for Spring Security.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class SpringSecurityRequestAuthenticator extends RequestAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(SpringSecurityRequestAuthenticator.class);
    private final HttpServletRequest request;

    /**
     * Creates a new Spring Security request authenticator.
     *
     * @param facade the current <code>HttpFacade</code> (required)
     * @param request the current <code>HttpServletRequest</code> (required)
     * @param deployment the <code>KeycloakDeployment</code> (required)
     * @param tokenStore the <cdoe>AdapterTokenStore</cdoe> (required)
     * @param sslRedirectPort the SSL redirect port (required)
     */
    public SpringSecurityRequestAuthenticator(
            HttpFacade facade,
            HttpServletRequest request,
            KeycloakDeployment deployment,
            AdapterTokenStore tokenStore,
            int sslRedirectPort) {

        super(facade, deployment, tokenStore, sslRedirectPort);
        this.request = request;
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(this, facade, deployment, sslRedirectPort, tokenStore);
    }

    @Override
    protected void completeOAuthAuthentication(final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {

        final RefreshableKeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();
        final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
        final OidcKeycloakAccount account = new SimpleKeycloakAccount(principal, roles, securityContext);

        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
        this.tokenStore.saveAccountInfo(account);
    }

    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, String method) {

        RefreshableKeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();
        Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
        final KeycloakAccount account = new SimpleKeycloakAccount(principal, roles, securityContext);

        logger.debug("Completing bearer authentication. Bearer roles: {} ",roles);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new KeycloakAuthenticationToken(account, false));
        SecurityContextHolder.setContext(context);

        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
    }

    @Override
    protected String changeHttpSessionId(boolean create) {
        HttpSession session = request.getSession(create);
        return session != null ? session.getId() : null;
    }
}
