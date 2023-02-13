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

package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.CookieTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * Per-request object. Storage of tokens in cookie
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UndertowCookieTokenStore implements AdapterTokenStore {

    protected static Logger log = Logger.getLogger(UndertowCookieTokenStore.class);

    private final HttpFacade facade;
    private final KeycloakDeployment deployment;
    private final SecurityContext securityContext;

    public UndertowCookieTokenStore(HttpFacade facade, KeycloakDeployment deployment,
                                    SecurityContext securityContext) {
        this.facade = facade;
        this.deployment = deployment;
        this.securityContext = securityContext;
    }

    @Override
    public void checkCurrentToken() {
        // no-op on undertow
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, facade, this);
        if (principal == null) {
            log.debug("Account was not in cookie or was invalid, returning null");
            return false;
        }
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal);

        if (!deployment.getRealm().equals(account.getKeycloakSecurityContext().getRealm())) {
            log.debug("Account in session belongs to a different realm than for this request.");
            return false;
        }

        if (account.checkActive()) {
            log.debug("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            ((AbstractUndertowRequestAuthenticator)authenticator).propagateKeycloakContext(account);
            return true;
        } else {
            log.debug("Account was not active, removing cookie and returning false");
            CookieTokenStore.removeCookie(deployment, facade);
            return false;
        }
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        RefreshableKeycloakSecurityContext secContext = (RefreshableKeycloakSecurityContext)account.getKeycloakSecurityContext();
        CookieTokenStore.setTokenCookie(deployment, facade, secContext);
    }

    @Override
    public void logout() {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, facade, this);
        if (principal == null) return;

        CookieTokenStore.removeCookie(deployment, facade);
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        CookieTokenStore.setTokenCookie(deployment, facade, securityContext);
    }

    @Override
    public void saveRequest() {

    }

    @Override
    public boolean restoreRequest() {
        return false;
    }
}
