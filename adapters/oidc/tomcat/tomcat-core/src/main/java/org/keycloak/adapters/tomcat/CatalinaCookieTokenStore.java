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

import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.CookieTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

import java.util.Set;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CatalinaCookieTokenStore implements AdapterTokenStore {

    private static final Logger log = Logger.getLogger(""+CatalinaCookieTokenStore.class);

    private Request request;
    private HttpFacade facade;
    private KeycloakDeployment deployment;
    private GenericPrincipalFactory principalFactory;

    private KeycloakPrincipal<RefreshableKeycloakSecurityContext> authenticatedPrincipal;

    public CatalinaCookieTokenStore(Request request, HttpFacade facade, KeycloakDeployment deployment, GenericPrincipalFactory principalFactory) {
        this.request = request;
        this.facade = facade;
        this.deployment = deployment;
        this.principalFactory = principalFactory;
    }


    @Override
    public void checkCurrentToken() {
        this.authenticatedPrincipal = checkPrincipalFromCookie();
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        // Assuming authenticatedPrincipal set by previous call of checkCurrentToken() during this request
        if (authenticatedPrincipal != null) {
            log.fine("remote logged in already. Establish state from cookie");
            RefreshableKeycloakSecurityContext securityContext = authenticatedPrincipal.getKeycloakSecurityContext();

            if (!securityContext.getRealm().equals(deployment.getRealm())) {
                log.fine("Account from cookie is from a different realm than for the request.");
                return false;
            }

            securityContext.setCurrentRequestInfo(deployment, this);
            Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
            GenericPrincipal principal = principalFactory.createPrincipal(request.getContext().getRealm(), authenticatedPrincipal, roles);

            request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
            request.setUserPrincipal(principal);
            request.setAuthType("KEYCLOAK");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext)account.getKeycloakSecurityContext();
        CookieTokenStore.setTokenCookie(deployment, facade, securityContext);
    }

    @Override
    public void logout() {
        CookieTokenStore.removeCookie(deployment, facade);
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext secContext) {
        CookieTokenStore.setTokenCookie(deployment, facade, secContext);
    }

    @Override
    public void saveRequest() {

    }

    @Override
    public boolean restoreRequest() {
        return false;
    }

    /**
     * Verify if we already have authenticated and active principal in cookie. Perform refresh if it's not active
     *
     * @return valid principal
     */
    protected KeycloakPrincipal<RefreshableKeycloakSecurityContext> checkPrincipalFromCookie() {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, facade, this);
        if (principal == null) {
            log.fine("Account was not in cookie or was invalid");
            return null;
        }

        RefreshableKeycloakSecurityContext session = principal.getKeycloakSecurityContext();

        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) return principal;
        boolean success = session.refreshExpiredToken(false);
        if (success && session.isActive()) return principal;

        log.fine("Cleanup and expire cookie for user " + principal.getName() + " after failed refresh");
        request.setUserPrincipal(null);
        request.setAuthType(null);
        CookieTokenStore.removeCookie(deployment, facade);
        return null;
    }
}
