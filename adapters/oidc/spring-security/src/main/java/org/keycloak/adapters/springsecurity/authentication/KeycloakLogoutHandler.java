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

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.AdapterTokenStoreFactory;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.adapters.springsecurity.token.SpringSecurityAdapterTokenStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Logs the current user out of Keycloak.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(KeycloakLogoutHandler.class);

    private AdapterDeploymentContext adapterDeploymentContext;
    private AdapterTokenStoreFactory adapterTokenStoreFactory = new SpringSecurityAdapterTokenStoreFactory();

    public KeycloakLogoutHandler(AdapterDeploymentContext adapterDeploymentContext) {
        Assert.notNull(adapterDeploymentContext);
        this.adapterDeploymentContext = adapterDeploymentContext;
    }

    public void setAdapterTokenStoreFactory(AdapterTokenStoreFactory adapterTokenStoreFactory) {
        this.adapterTokenStoreFactory = adapterTokenStoreFactory;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null) {
            log.warn("Cannot log out without authentication");
            return;
        }
        else if (!KeycloakAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            log.warn("Cannot log out a non-Keycloak authentication: {}", authentication);
            return;
        }

        handleSingleSignOut(request, response, (KeycloakAuthenticationToken) authentication);
    }

    protected void handleSingleSignOut(HttpServletRequest request, HttpServletResponse response, KeycloakAuthenticationToken authenticationToken) {
        HttpFacade facade = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = adapterDeploymentContext.resolveDeployment(facade);
        adapterTokenStoreFactory.createAdapterTokenStore(deployment, request, response).logout();
        RefreshableKeycloakSecurityContext session = (RefreshableKeycloakSecurityContext) authenticationToken.getAccount().getKeycloakSecurityContext();
        session.logout(deployment);
    }
}
