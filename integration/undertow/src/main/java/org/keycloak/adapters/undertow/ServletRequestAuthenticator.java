/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 * @version $Revision: 1 $
 */
public class ServletRequestAuthenticator extends UndertowRequestAuthenticator {


    public ServletRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort,
                                       SecurityContext securityContext, HttpServerExchange exchange,
                                       UndertowUserSessionManagement userSessionManagement) {
        super(facade, deployment, sslRedirectPort, securityContext, exchange, userSessionManagement);
    }

    @Override
    protected boolean isCached() {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        HttpSession session = req.getSession(false);
        if (session == null) {
            log.info("session was null, returning null");
            return false;
        }
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) {
            log.info("Account was not in session, returning null");
            return false;
        }
        account.setDeployment(deployment);
        if (account.isActive()) {
            log.info("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            propagateKeycloakContext( account);
            return true;
        }
        log.info("Account was not active, returning null");
        session.setAttribute(KeycloakUndertowAccount.class.getName(), null);
        return false;
    }

    @Override
    protected void propagateKeycloakContext(KeycloakUndertowAccount account) {
        super.propagateKeycloakContext(account);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());
    }

    @Override
    protected void login(KeycloakAccount account) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        HttpSession session = req.getSession(true);
        session.setAttribute(KeycloakUndertowAccount.class.getName(), account);
        userSessionManagement.login(servletRequestContext.getDeployment().getSessionManager(), session.getId(), account.getPrincipal().getName(), account.getKeycloakSecurityContext().getToken().getSessionState());

    }

    @Override
    protected KeycloakUndertowAccount createAccount(KeycloakPrincipal principal, RefreshableKeycloakSecurityContext session) {
        return new KeycloakUndertowAccount(principal, session, deployment);
    }
}
