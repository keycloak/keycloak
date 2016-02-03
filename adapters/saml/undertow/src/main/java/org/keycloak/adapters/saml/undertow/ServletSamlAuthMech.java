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

package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.Headers;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.undertow.ServletHttpFacade;
import org.keycloak.adapters.undertow.UndertowHttpFacade;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletSamlAuthMech extends AbstractSamlAuthMech {
    protected SessionIdMapper idMapper = new InMemorySessionIdMapper();
    public ServletSamlAuthMech(SamlDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement, String errorPage) {
        super(deploymentContext, sessionManagement, errorPage);
    }

    @Override
    protected SamlSessionStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, SamlDeployment deployment, SecurityContext securityContext) {
        return new ServletSamlSessionStore(exchange, sessionManagement, securityContext, idMapper, deployment);
    }

    @Override
    protected UndertowHttpFacade createFacade(HttpServerExchange exchange) {
        return new ServletHttpFacade(exchange);
    }

    @Override
    protected void redirectLogout(SamlDeployment deployment, HttpServerExchange exchange) {
       servePage(exchange, deployment.getLogoutPage());
    }

    @Override
    protected Integer servePage(HttpServerExchange exchange, String location) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        ServletRequest req = servletRequestContext.getServletRequest();
        ServletResponse resp = servletRequestContext.getServletResponse();
        RequestDispatcher disp = req.getRequestDispatcher(location);
        //make sure the login page is never cached
        exchange.getResponseHeaders().add(Headers.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().add(Headers.PRAGMA, "no-cache");
        exchange.getResponseHeaders().add(Headers.EXPIRES, "0");


        try {
            disp.forward(req, resp);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


}
