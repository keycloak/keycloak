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

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.PreAuthActionsHandler;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletPreAuthActionsHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(ServletPreAuthActionsHandler.class);
    protected HttpHandler next;
    protected UndertowUserSessionManagement userSessionManagement;
    protected AdapterDeploymentContext deploymentContext;

    public static class Wrapper implements HandlerWrapper {
        protected AdapterDeploymentContext deploymentContext;
        protected UndertowUserSessionManagement userSessionManagement;


        public Wrapper(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement) {
            this.deploymentContext = deploymentContext;
            this.userSessionManagement = userSessionManagement;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new ServletPreAuthActionsHandler(deploymentContext, userSessionManagement, handler);
        }
    }

    protected ServletPreAuthActionsHandler(AdapterDeploymentContext deploymentContext,
                                           UndertowUserSessionManagement userSessionManagement,
                                           HttpHandler next) {
        this.next = next;
        this.deploymentContext = deploymentContext;
        this.userSessionManagement = userSessionManagement;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        UndertowHttpFacade facade = new OIDCServletUndertowHttpFacade(exchange);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        SessionManagementBridge bridge = new SessionManagementBridge(userSessionManagement, servletRequestContext.getDeployment().getSessionManager());
        PreAuthActionsHandler handler = new PreAuthActionsHandler(bridge, deploymentContext, facade);
        if (handler.handleRequest()) return;
        next.handleRequest(exchange);
    }


}
