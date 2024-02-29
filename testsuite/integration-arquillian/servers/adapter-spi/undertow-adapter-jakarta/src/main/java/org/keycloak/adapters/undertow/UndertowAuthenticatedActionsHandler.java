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
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * Bridge for authenticated Keycloak adapter actions
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 * @version $Revision: 1 $
 */
public class UndertowAuthenticatedActionsHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(UndertowAuthenticatedActionsHandler.class);
    protected AdapterDeploymentContext deploymentContext;
    protected HttpHandler next;

    public static class Wrapper implements HandlerWrapper {
        protected AdapterDeploymentContext deploymentContext;

        public Wrapper(AdapterDeploymentContext deploymentContext) {
            this.deploymentContext = deploymentContext;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new UndertowAuthenticatedActionsHandler(deploymentContext, handler);
        }
    }


    public UndertowAuthenticatedActionsHandler(AdapterDeploymentContext deploymentContext, HttpHandler next) {
        this.deploymentContext = deploymentContext;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        OIDCUndertowHttpFacade facade = new OIDCUndertowHttpFacade(exchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment != null && deployment.isConfigured()) {
            AuthenticatedActionsHandler handler = new AuthenticatedActionsHandler(deployment, facade);
            if (handler.handledRequest()) return;
        }
        next.handleRequest(exchange);
    }
}
