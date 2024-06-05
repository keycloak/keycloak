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
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 * @version $Revision: 1 $
 */
public abstract class AbstractUndertowRequestAuthenticator extends RequestAuthenticator {
    protected SecurityContext securityContext;
    protected HttpServerExchange exchange;


    public AbstractUndertowRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort,
                                                SecurityContext securityContext, HttpServerExchange exchange,
                                                AdapterTokenStore tokenStore) {
        super(facade, deployment, tokenStore, sslRedirectPort);
        this.securityContext = securityContext;
        this.exchange = exchange;
    }

    protected void propagateKeycloakContext(KeycloakUndertowAccount account) {
        exchange.putAttachment(OIDCUndertowHttpFacade.KEYCLOAK_SECURITY_CONTEXT_KEY, account.getKeycloakSecurityContext());
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(this, facade, deployment, sslRedirectPort, tokenStore);
    }

    @Override
    protected void completeOAuthAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        KeycloakUndertowAccount account = createAccount(principal);
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
        propagateKeycloakContext(account);
        tokenStore.saveAccountInfo(account);
    }

    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, String method) {
        KeycloakUndertowAccount account = createAccount(principal);
        securityContext.authenticationComplete(account, method, false);
        propagateKeycloakContext(account);
    }

    @Override
    protected String changeHttpSessionId(boolean create) {
        if (create) {
            Session session = Sessions.getOrCreateSession(exchange);
            return session.getId();
        } else {
            Session session = Sessions.getSession(exchange);
            return session != null ? session.getId() : null;
        }
    }

    /**
     * Subclasses need to be able to create their own version of the KeycloakUndertowAccount
     * @return The account
     */
    protected abstract KeycloakUndertowAccount createAccount(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal);

}
