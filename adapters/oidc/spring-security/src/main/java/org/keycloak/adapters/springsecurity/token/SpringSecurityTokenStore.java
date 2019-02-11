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

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Simple Spring {@link SecurityContext security context} aware {@link AdapterTokenStore adapter token store}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class SpringSecurityTokenStore implements AdapterTokenStore {

    private final Logger logger = LoggerFactory.getLogger(SpringSecurityTokenStore.class);

    private final KeycloakDeployment deployment;
    private final HttpServletRequest request;

    public SpringSecurityTokenStore(KeycloakDeployment deployment, HttpServletRequest request) {
        Assert.notNull(deployment, "KeycloakDeployment is required");
        Assert.notNull(request, "HttpServletRequest is required");
        this.deployment = deployment;
        this.request = request;
    }

    @Override
    public void checkCurrentToken() {
        // no-op
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {

        logger.debug("Checking if {} is cached", authenticator);
        SecurityContext context = SecurityContextHolder.getContext();
        KeycloakAuthenticationToken token;
        KeycloakSecurityContext keycloakSecurityContext;

        if (context == null || context.getAuthentication() == null) {
            return false;
        }

        if (!KeycloakAuthenticationToken.class.isAssignableFrom(context.getAuthentication().getClass())) {
            logger.warn("Expected a KeycloakAuthenticationToken, but found {}", context.getAuthentication());
            return false;
        }

        logger.debug("Remote logged in already. Establishing state from security context.");
        token = (KeycloakAuthenticationToken) context.getAuthentication();
        keycloakSecurityContext = token.getAccount().getKeycloakSecurityContext();

        if (!deployment.getRealm().equals(keycloakSecurityContext.getRealm())) {
            logger.debug("Account from security context is from a different realm than for the request.");
            logout();
            return false;
        }

        if (keycloakSecurityContext.getToken().isExpired()) {
            logger.warn("Security token expired ... not returning from cache");
            return false;
        }

        request.setAttribute(KeycloakSecurityContext.class.getName(), keycloakSecurityContext);

        return true;
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            throw new IllegalStateException(String.format("Went to save Keycloak account %s, but already have %s", account, authentication));
        }

        logger.debug("Saving account info {}", account);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new KeycloakAuthenticationToken(account, true));
        SecurityContextHolder.setContext(context);
    }

    @Override
    public void logout() {

        logger.debug("Handling logout request");
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.setAttribute(KeycloakSecurityContext.class.getName(), null);
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }

    @Override
    public void saveRequest() {
        // no-op, Spring Security will handle this
    }

    @Override
    public boolean restoreRequest() {
        // no-op, Spring Security will handle this
        return false;
    }
}
