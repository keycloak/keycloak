/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.elytron;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.handlers.ServletRequestContext;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakServletExtension implements ServletExtension {

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addOuterHandlerChainWrapper(handler -> (HttpHandler) exchange -> {
            ServletRequest servletRequest = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY).getServletRequest();

            servletRequest.setAttribute(ElytronHttpFacade.UNDERTOW_EXCHANGE, new ProtectedHttpServerExchange(exchange));

            handler.handleRequest(exchange);
        });
    }
}
