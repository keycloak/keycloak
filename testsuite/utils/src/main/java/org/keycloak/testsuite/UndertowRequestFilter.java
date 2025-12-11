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

package org.keycloak.testsuite;

import java.io.UnsupportedEncodingException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.resteasy.core.ResteasyContext;

public class UndertowRequestFilter implements Filter {

    private final KeycloakSessionFactory factory;

    public UndertowRequestFilter(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws UnsupportedEncodingException {
        servletRequest.setCharacterEncoding("UTF-8");
        final HttpServletRequest request = (HttpServletRequest) servletRequest;

        ClientConnection connection = createClientConnection(request);
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            try {
                ResteasyContext.pushContext(KeycloakSession.class, session);
                session.getContext().setConnection(connection);
                filterChain.doFilter(servletRequest, servletResponse);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ClientConnection createClientConnection(HttpServletRequest request) {
        return new ClientConnection() {
            @Override
            public String getRemoteAddr() {
                String forwardedFor = request.getHeader("X-Forwarded-For");

                if (forwardedFor != null) {
                    return forwardedFor;
                }

                return request.getRemoteAddr();
            }

            @Override
            public String getRemoteHost() {
                return request.getRemoteHost();
            }

            @Override
            public int getRemotePort() {
                return request.getRemotePort();
            }

            @Override
            public String getLocalAddr() {
                return request.getLocalAddr();
            }

            @Override
            public int getLocalPort() {
                return request.getLocalPort();
            }
        };
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
