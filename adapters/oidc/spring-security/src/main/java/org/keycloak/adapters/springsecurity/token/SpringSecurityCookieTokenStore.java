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

package org.keycloak.adapters.springsecurity.token;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.CookieTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

/**
 * Extension of {@link SpringSecurityTokenStore} that stores the obtains tokens in a cookie.
 *
 * @author <a href="mailto:scranen@gmail.com">Sjoerd Cranen</a>
 */
public class SpringSecurityCookieTokenStore extends SpringSecurityTokenStore {

    private final Logger logger = LoggerFactory.getLogger(SpringSecurityCookieTokenStore.class);

    private final KeycloakDeployment deployment;
    private final HttpFacade facade;
    private volatile boolean cookieChecked = false;

    public SpringSecurityCookieTokenStore(
            KeycloakDeployment deployment,
            HttpServletRequest request,
            HttpServletResponse response) {
        super(deployment, request);
        Assert.notNull(response, "HttpServletResponse is required");
        this.deployment = deployment;
        this.facade = new SimpleHttpFacade(request, response);
    }

    @Override
    public void checkCurrentToken() {
        final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal =
                checkPrincipalFromCookie();
        if (principal != null) {
            final RefreshableKeycloakSecurityContext securityContext =
                    principal.getKeycloakSecurityContext();
            KeycloakSecurityContext current = ((OIDCHttpFacade) facade).getSecurityContext();
            if (current != null) {
                securityContext.setAuthorizationContext(current.getAuthorizationContext());
            }
            final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
            final OidcKeycloakAccount account =
                    new SimpleKeycloakAccount(principal, roles, securityContext);
            SecurityContextHolder.getContext()
                    .setAuthentication(new KeycloakAuthenticationToken(account, false));
        } else {
            super.checkCurrentToken();
        }
        cookieChecked = true;
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        if (!cookieChecked) {
            checkCurrentToken();
        }
        return super.isCached(authenticator);
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        super.refreshCallback(securityContext);
        CookieTokenStore.setTokenCookie(deployment, facade, securityContext);
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        super.saveAccountInfo(account);
        RefreshableKeycloakSecurityContext securityContext =
                (RefreshableKeycloakSecurityContext) account.getKeycloakSecurityContext();
        CookieTokenStore.setTokenCookie(deployment, facade, securityContext);
    }

    @Override
    public void logout() {
        CookieTokenStore.removeCookie(deployment, facade);
        super.logout();
    }

    /**
     * Verify if we already have authenticated and active principal in cookie. Perform refresh if
     * it's not active
     *
     * @return valid principal
     */
    private KeycloakPrincipal<RefreshableKeycloakSecurityContext> checkPrincipalFromCookie() {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal =
                CookieTokenStore.getPrincipalFromCookie(deployment, facade, this);
        if (principal == null) {
            logger.debug("Account was not in cookie or was invalid");
            return null;
        }

        RefreshableKeycloakSecurityContext session = principal.getKeycloakSecurityContext();

        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) return principal;
        boolean success = session.refreshExpiredToken(false);
        if (success && session.isActive()) {
            refreshCallback(session);
            return principal;
        }

        logger.debug(
                "Cleanup and expire cookie for user {} after failed refresh", principal.getName());
        CookieTokenStore.removeCookie(deployment, facade);
        return null;
    }
}
