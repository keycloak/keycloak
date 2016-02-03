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

package org.keycloak.proxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;
import org.keycloak.representations.IDToken;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConstraintAuthorizationHandler implements HttpHandler {

    public static final String KEYCLOAK_SUBJECT = "KEYCLOAK_SUBJECT";
    public static final String KEYCLOAK_USERNAME = "KEYCLOAK_USERNAME";
    public static final String KEYCLOAK_EMAIL = "KEYCLOAK_EMAIL";
    public static final String KEYCLOAK_NAME = "KEYCLOAK_NAME";
    public static final String KEYCLOAK_ACCESS_TOKEN = "KEYCLOAK_ACCESS_TOKEN";
    private final Map<String, HttpString> httpHeaderNames;

    protected HttpHandler next;
    protected String errorPage;
    protected boolean sendAccessToken;

    public ConstraintAuthorizationHandler(HttpHandler next, String errorPage, boolean sendAccessToken, Map<String, String> headerNames) {
        this.next = next;
        this.errorPage = errorPage;
        this.sendAccessToken = sendAccessToken;

        this.httpHeaderNames = new HashMap<>();
        this.httpHeaderNames.put(KEYCLOAK_SUBJECT, new HttpString(getOrDefault(headerNames, "keycloak-subject", KEYCLOAK_SUBJECT)));
        this.httpHeaderNames.put(KEYCLOAK_USERNAME, new HttpString(getOrDefault(headerNames, "keycloak-username", KEYCLOAK_USERNAME)));
        this.httpHeaderNames.put(KEYCLOAK_EMAIL, new HttpString(getOrDefault(headerNames, "keycloak-email", KEYCLOAK_EMAIL)));
        this.httpHeaderNames.put(KEYCLOAK_NAME, new HttpString(getOrDefault(headerNames, "keycloak-name", KEYCLOAK_NAME)));
        this.httpHeaderNames.put(KEYCLOAK_ACCESS_TOKEN, new HttpString(getOrDefault(headerNames, "keycloak-access-token", KEYCLOAK_ACCESS_TOKEN)));
    }

    private String getOrDefault(Map<String, String> map, String key, String defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        KeycloakUndertowAccount account = (KeycloakUndertowAccount)exchange.getSecurityContext().getAuthenticatedAccount();

        SingleConstraintMatch match = exchange.getAttachment(ConstraintMatcherHandler.CONSTRAINT_KEY);
        if (match == null || (match.getRequiredRoles().isEmpty() && match.getEmptyRoleSemantic() == SecurityInfo.EmptyRoleSemantic.AUTHENTICATE)) {
            authenticatedRequest(account, exchange);
            return;
        }

        if (match != null) {
            if(SecurityInfo.EmptyRoleSemantic.PERMIT_AND_INJECT_IF_AUTHENTICATED.equals(match.getEmptyRoleSemantic())) {
                authenticatedRequest(account, exchange);
                return;
            } else {
                for (String role : match.getRequiredRoles()) {
                    if (account.getRoles().contains(role)) {
                        authenticatedRequest(account, exchange);
                        return;
                    }
                }
            }
        }

        if (errorPage != null) {
            exchange.setRequestPath(errorPage);
            exchange.setRelativePath(errorPage);
            exchange.setResolvedPath(errorPage);
            next.handleRequest(exchange);
            return;

        }
        exchange.setResponseCode(403);
        exchange.endExchange();

    }

    public void authenticatedRequest(KeycloakUndertowAccount account, HttpServerExchange exchange) throws Exception {
        if (account != null) {
            IDToken idToken = account.getKeycloakSecurityContext().getToken();
            if (idToken == null) return;
            if (idToken.getSubject() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get(KEYCLOAK_SUBJECT), idToken.getSubject());
            }

            if (idToken.getPreferredUsername() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get(KEYCLOAK_USERNAME), idToken.getPreferredUsername());
            }
            if (idToken.getEmail() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get(KEYCLOAK_EMAIL), idToken.getEmail());
            }
            if (idToken.getName() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get(KEYCLOAK_NAME), idToken.getName());
            }
            if (sendAccessToken) {
                exchange.getRequestHeaders().put(httpHeaderNames.get(KEYCLOAK_ACCESS_TOKEN), account.getKeycloakSecurityContext().getTokenString());
            }
        }
        next.handleRequest(exchange);
    }
}
