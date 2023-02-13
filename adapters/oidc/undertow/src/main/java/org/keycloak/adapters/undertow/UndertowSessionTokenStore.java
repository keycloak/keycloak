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
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.util.Sessions;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * Per-request object. Storage of tokens in undertow session.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UndertowSessionTokenStore implements AdapterTokenStore {

    protected static Logger log = Logger.getLogger(UndertowSessionTokenStore.class);

    private final HttpServerExchange exchange;
    private final KeycloakDeployment deployment;
    private final UndertowUserSessionManagement sessionManagement;
    private final SecurityContext securityContext;

    public UndertowSessionTokenStore(HttpServerExchange exchange, KeycloakDeployment deployment, UndertowUserSessionManagement sessionManagement,
                                     SecurityContext securityContext) {
        this.exchange = exchange;
        this.deployment = deployment;
        this.sessionManagement = sessionManagement;
        this.securityContext = securityContext;
    }

    @Override
    public void checkCurrentToken() {
        // no-op on undertow
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        Session session = Sessions.getSession(exchange);
        if (session == null) {
            log.debug("session was null, returning null");
            return false;
        }
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) {
            log.debug("Account was not in session, returning null");
            return false;
        }

        if (!deployment.getRealm().equals(account.getKeycloakSecurityContext().getRealm())) {
            log.debug("Account in session belongs to a different realm than for this request.");
            return false;
        }

        account.setCurrentRequestInfo(deployment, this);
        if (account.checkActive()) {
            log.debug("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            ((AbstractUndertowRequestAuthenticator)authenticator).propagateKeycloakContext(account);
            return true;
        } else {
            log.debug("Account was not active, returning false");
            session.removeAttribute(KeycloakUndertowAccount.class.getName());
            session.removeAttribute(KeycloakSecurityContext.class.getName());
            session.invalidate(exchange);
            return false;
        }
    }

    @Override
    public void saveRequest() {

    }

    @Override
    public boolean restoreRequest() {
        return false;
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        Session session = Sessions.getOrCreateSession(exchange);
        session.setAttribute(KeycloakUndertowAccount.class.getName(), account);
        session.setAttribute(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());
        sessionManagement.login(session.getSessionManager());
    }

    @Override
    public void logout() {
        Session session = Sessions.getSession(exchange);
        if (session == null) return;
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) return;
        session.removeAttribute(KeycloakUndertowAccount.class.getName());
        session.removeAttribute(KeycloakSecurityContext.class.getName());
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }
}
