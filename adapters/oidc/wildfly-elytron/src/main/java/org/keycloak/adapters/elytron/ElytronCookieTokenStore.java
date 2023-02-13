/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.elytron;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.CookieTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.wildfly.security.http.HttpScope;
import org.wildfly.security.http.Scope;

import javax.security.auth.callback.CallbackHandler;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ElytronCookieTokenStore implements ElytronTokeStore, UserSessionManagement {

    protected static Logger log = Logger.getLogger(ElytronCookieTokenStore.class);

    private final ElytronHttpFacade httpFacade;
    private final CallbackHandler callbackHandler;

    public ElytronCookieTokenStore(ElytronHttpFacade httpFacade, CallbackHandler callbackHandler) {
        this.httpFacade = httpFacade;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void checkCurrentToken() {
        KeycloakDeployment deployment = httpFacade.getDeployment();
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, httpFacade, this);

        if (principal == null) {
            return;
        }

        RefreshableKeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();

        if (securityContext.isActive() && !securityContext.getDeployment().isAlwaysRefreshToken()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        boolean success = securityContext.refreshExpiredToken(false);
        if (success && securityContext.isActive()) return;

        saveAccountInfo(new ElytronAccount(principal));
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        KeycloakDeployment deployment = httpFacade.getDeployment();
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(deployment, httpFacade, this);
        if (principal == null) {
            log.debug("Account was not in cookie or was invalid, returning null");
            return false;
        }
        ElytronAccount account = new ElytronAccount(principal);

        if (!deployment.getRealm().equals(account.getKeycloakSecurityContext().getRealm())) {
            log.debug("Account in session belongs to a different realm than for this request.");
            return false;
        }

        boolean active = account.checkActive();

        if (!active) {
            active = account.tryRefresh();
        }

        if (active) {
            log.debug("Cached account found");
            restoreRequest();
            httpFacade.authenticationComplete(account, true);
            return true;
        } else {
            log.debug("Account was not active, removing cookie and returning false");
            CookieTokenStore.removeCookie(deployment, httpFacade);
            return false;
        }
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        RefreshableKeycloakSecurityContext secContext = (RefreshableKeycloakSecurityContext)account.getKeycloakSecurityContext();
        CookieTokenStore.setTokenCookie(this.httpFacade.getDeployment(), this.httpFacade, secContext);
        HttpScope exchange = this.httpFacade.getScope(Scope.EXCHANGE);

        exchange.registerForNotification(httpServerScopes -> logout());

        exchange.setAttachment(ElytronAccount.class.getName(), account);
        exchange.setAttachment(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());

        restoreRequest();
    }

    @Override
    public void logout() {
        logout(false);
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        CookieTokenStore.setTokenCookie(this.httpFacade.getDeployment(), httpFacade, securityContext);
    }

    @Override
    public void saveRequest() {

    }

    @Override
    public boolean restoreRequest() {
        return false;
    }

    @Override
    public void logout(boolean glo) {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = CookieTokenStore.getPrincipalFromCookie(this.httpFacade.getDeployment(), this.httpFacade, this);

        if (principal == null) {
            return;
        }

        CookieTokenStore.removeCookie(this.httpFacade.getDeployment(), this.httpFacade);

        if (glo) {
            KeycloakSecurityContext ksc = (KeycloakSecurityContext) principal.getKeycloakSecurityContext();

            if (ksc == null) {
                return;
            }

            KeycloakDeployment deployment = httpFacade.getDeployment();

            if (!deployment.isBearerOnly() && ksc != null && ksc instanceof RefreshableKeycloakSecurityContext) {
                ((RefreshableKeycloakSecurityContext) ksc).logout(deployment);
            }
        }
    }

    @Override
    public void logoutAll() {
        //no-op
    }

    @Override
    public void logoutHttpSessions(List<String> ids) {
        //no-op
    }
}
