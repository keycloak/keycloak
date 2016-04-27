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

package org.keycloak.services.filters;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSessionServletFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        servletRequest.setCharacterEncoding("UTF-8");

        final HttpServletRequest request = (HttpServletRequest)servletRequest;

        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) servletRequest.getServletContext().getAttribute(KeycloakSessionFactory.class.getName());
        KeycloakSession session = sessionFactory.create();
        ResteasyProviderFactory.pushContext(KeycloakSession.class, session);
        ClientConnection connection = new ClientConnection() {
            @Override
            public String getRemoteAddr() {
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
        session.getContext().setConnection(connection);
        ResteasyProviderFactory.pushContext(ClientConnection.class, connection);

        KeycloakTransaction tx = session.getTransaction();
        ResteasyProviderFactory.pushContext(KeycloakTransaction.class, tx);
        tx.begin();

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // KeycloakTransactionCommitter is responsible for committing the transaction, but if an exception is thrown it's not invoked and transaction
            // should be rolled back
            if (session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }

            session.close();
            ResteasyProviderFactory.clearContextData();
        }
    }

    @Override
    public void destroy() {
    }
}
