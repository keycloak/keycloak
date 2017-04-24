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

import java.util.function.Consumer;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.wildfly.security.http.HttpScope;
import org.wildfly.security.http.HttpScopeNotification;
import org.wildfly.security.http.Scope;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ElytronSessionTokenStore implements ElytronTokeStore {

    private static Logger log = Logger.getLogger(ElytronSessionTokenStore.class);

    private final ElytronHttpFacade httpFacade;
    private final CallbackHandler callbackHandler;

    public ElytronSessionTokenStore(ElytronHttpFacade httpFacade, CallbackHandler callbackHandler) {
        this.httpFacade = httpFacade;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void checkCurrentToken() {
        HttpScope session = httpFacade.getScope(Scope.SESSION);
        if (!session.exists()) return;
        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext) session.getAttachment(KeycloakSecurityContext.class.getName());
        if (securityContext == null) return;

        // just in case session got serialized
        if (securityContext.getDeployment() == null) securityContext.setCurrentRequestInfo(httpFacade.getDeployment(), this);

        if (securityContext.isActive() && !securityContext.getDeployment().isAlwaysRefreshToken()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        boolean success = securityContext.refreshExpiredToken(false);
        if (success && securityContext.isActive()) return;

        // Refresh failed, so user is already logged out from keycloak. Cleanup and expire our session
        session.setAttachment(KeycloakSecurityContext.class.getName(), null);
        session.invalidate();
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        HttpScope session = this.httpFacade.getScope(Scope.SESSION);

        if (session == null) {
            log.debug("session was null, returning null");
            return false;
        }

        ElytronAccount account;

        try {
            account = (ElytronAccount) session.getAttachment(ElytronAccount.class.getName());
        } catch (IllegalStateException e) {
            log.debug("session was invalidated.  Return false.");
            return false;
        }
        if (account == null) {
            log.debug("Account was not in session, returning null");
            return false;
        }

        KeycloakDeployment deployment = httpFacade.getDeployment();

        if (!deployment.getRealm().equals(account.getKeycloakSecurityContext().getRealm())) {
            log.debug("Account in session belongs to a different realm than for this request.");
            return false;
        }

        boolean active = account.checkActive();

        if (!active) {
            active = account.tryRefresh(this.callbackHandler);
        }

        if (active) {
            log.debug("Cached account found");
            restoreRequest();
            httpFacade.authenticationComplete(account, true);
            return true;
        } else {
            log.debug("Refresh failed. Account was not active. Returning null and invalidating Http session");
            try {
                session.setAttachment(KeycloakSecurityContext.class.getName(), null);
                session.setAttachment(ElytronAccount.class.getName(), null);
                session.invalidate();
            } catch (Exception e) {
                log.debug("Failed to invalidate session, might already be invalidated");
            }
            return false;
        }
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        HttpScope session = this.httpFacade.getScope(Scope.SESSION);

        if (!session.exists()) {
            session.create();
        }

        session.setAttachment(ElytronAccount.class.getName(), account);
        session.setAttachment(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());

        session.registerForNotification(httpScopeNotification -> {
            if (!httpScopeNotification.isOfType(HttpScopeNotification.SessionNotificationType.UNDEPLOY)) {
                logout();
            }
        });

        HttpScope scope = this.httpFacade.getScope(Scope.EXCHANGE);

        scope.setAttachment(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());
    }

    @Override
    public void logout() {
        logout(false);
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(AdapterUtils.getPrincipalName(this.httpFacade.getDeployment(), securityContext.getToken()), securityContext);
        saveAccountInfo(new ElytronAccount(principal));
    }

    @Override
    public void saveRequest() {
        this.httpFacade.suspendRequest();
    }

    @Override
    public boolean restoreRequest() {
        return this.httpFacade.restoreRequest();
    }

    @Override
    public void logout(boolean glo) {
        HttpScope session = this.httpFacade.getScope(Scope.SESSION);

        if (!session.exists()) {
            return;
        }

        try {
            if (glo) {
                KeycloakSecurityContext ksc = (KeycloakSecurityContext) session.getAttachment(KeycloakSecurityContext.class.getName());

                if (ksc == null) {
                    return;
                }

                KeycloakDeployment deployment = httpFacade.getDeployment();

                if (!deployment.isBearerOnly() && ksc != null && ksc instanceof RefreshableKeycloakSecurityContext) {
                    ((RefreshableKeycloakSecurityContext) ksc).logout(deployment);
                }
            }

            session.setAttachment(KeycloakSecurityContext.class.getName(), null);
            session.setAttachment(ElytronAccount.class.getName(), null);
            session.invalidate();
        } catch (IllegalStateException ise) {
            // Session may be already logged-out in case that app has adminUrl
            log.debugf("Session %s logged-out already", session.getID());
        }
    }
}
