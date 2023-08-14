/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.resteasy;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.vertx.VertxResteasyReactiveRequestContext;
import org.keycloak.http.HttpCookie;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;

public final class QuarkusHttpResponse implements HttpResponse, KeycloakTransaction {

    private final ResteasyReactiveRequestContext requestContext;

    private Set<HttpCookie> cookies;
    private boolean transactionActive;
    private boolean writeCookiesOnTransactionComplete;

    public QuarkusHttpResponse(KeycloakSession session, ResteasyReactiveRequestContext requestContext) {
        this.requestContext = requestContext;
        session.getTransactionManager().enlistAfterCompletion(this);
    }

    @Override
    public int getStatus() {
        VertxResteasyReactiveRequestContext serverHttpResponse = (VertxResteasyReactiveRequestContext) requestContext.serverResponse();
        return serverHttpResponse.vertxServerResponse().getStatusCode();
    }

    @Override
    public void setStatus(int statusCode) {
        requestContext.serverResponse().setStatusCode(statusCode);
    }

    @Override
    public void addHeader(String name, String value) {
        requestContext.serverResponse().addResponseHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) {
        requestContext.serverResponse().setResponseHeader(name, value);
    }

    @Override
    public void setCookieIfAbsent(HttpCookie cookie) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie is null");
        }

        if (cookies == null) {
            cookies = new HashSet<>();
        }

        if (cookies.add(cookie)) {
            if (writeCookiesOnTransactionComplete) {
                // cookies are written after transaction completes
                return;
            }

            addHeader(HttpHeaders.SET_COOKIE, cookie.toHeaderValue());
        }
    }

    @Override
    public void setWriteCookiesOnTransactionComplete() {
        this.writeCookiesOnTransactionComplete = true;
    }

    @Override
    public void begin() {
        transactionActive = true;
    }

    @Override
    public void commit() {
        if (!transactionActive) {
            throw new IllegalStateException("Transaction not active. Response already committed or rolled back");
        }

        try {
            addCookiesAfterTransaction();
        } finally {
            close();
        }
    }

    @Override
    public void rollback() {
        close();
    }

    @Override
    public void setRollbackOnly() {

    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }

    @Override
    public boolean isActive() {
        return transactionActive;
    }

    private void close() {
        transactionActive = false;
        cookies = null;
    }

    private void addCookiesAfterTransaction() {
        if (cookies == null || !writeCookiesOnTransactionComplete) {
            return;
        }

        // Ensure that cookies are only added when the transaction is complete, as otherwise cookies will be set for
        // error pages, or will be added twice when running retries.
        for (HttpCookie cookie : cookies) {
            addHeader(HttpHeaders.SET_COOKIE, cookie.toHeaderValue());
        }
    }
}
