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
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Per-request object. Storage of tokens in servlet HTTP session.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServletSessionTokenStore implements AdapterTokenStore {

    protected static Logger log = Logger.getLogger(ServletSessionTokenStore.class);

    private final HttpServerExchange exchange;
    private final KeycloakDeployment deployment;
    private final UndertowUserSessionManagement sessionManagement;
    private final SecurityContext securityContext;

    public ServletSessionTokenStore(HttpServerExchange exchange, KeycloakDeployment deployment, UndertowUserSessionManagement sessionManagement,
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
        HttpSession session = getSession(false);
        if (session == null) {
            log.debug("session was null, returning null");
            return false;
        }
        KeycloakUndertowAccount account = null;
        try {
            account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        } catch (IllegalStateException e) {
            log.debug("session was invalidated.  Return false.");
            return false;
        }
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
            restoreRequest();
            return true;
        } else {
            log.debug("Refresh failed. Account was not active. Returning null and invalidating Http session");
            try {
                session.removeAttribute(KeycloakUndertowAccount.class.getName());
                session.removeAttribute(KeycloakSecurityContext.class.getName());
                session.invalidate();
            } catch (Exception e) {
                log.debug("Failed to invalidate session, might already be invalidated");
            }
            return false;
        }
    }

    @Override
    public void saveAccountInfo(OidcKeycloakAccount account) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpSession session = getSession(true);
        session.setAttribute(KeycloakUndertowAccount.class.getName(), account);
        session.setAttribute(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());
        sessionManagement.login(servletRequestContext.getDeployment().getSessionManager());
    }

    @Override
    public void logout() {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.removeAttribute(KeycloakUndertowAccount.class.getName());
        req.removeAttribute(KeycloakSecurityContext.class.getName());
        HttpSession session = req.getSession(false);
        if (session == null) return;
        try {
            KeycloakUndertowAccount account = (KeycloakUndertowAccount) session.getAttribute(KeycloakUndertowAccount.class.getName());
            if (account == null) return;
            session.removeAttribute(KeycloakSecurityContext.class.getName());
            session.removeAttribute(KeycloakUndertowAccount.class.getName());
        } catch (IllegalStateException ise) {
            // Session may be already logged-out in case that app has adminUrl
            log.debugf("Session %s logged-out already", session.getId());
        }
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }

    @Override
    public void saveRequest() {
        SavedRequest.trySaveRequest(exchange);

    }

    @Override
    public boolean restoreRequest() {
        HttpSession session = getSession(false);
        if (session == null) return false;
        SavedRequest.tryRestoreRequest(exchange, session);
        return false;
    }

    protected HttpSession getSession(boolean create) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        return req.getSession(create);
    }

}
