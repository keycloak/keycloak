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

package org.keycloak.adapters.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Abstract base for pre-installed actions that must be authenticated
 * <p/>
 * Actions include:
 * <p/>
 * CORS Origin Check and Response headers
 * k_query_bearer_token: Get bearer token from server for Javascripts CORS requests
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractAuthenticatedActionsValve extends ValveBase {
    private static final Logger log = Logger.getLogger(AbstractAuthenticatedActionsValve.class);
    protected AdapterDeploymentContext deploymentContext;

    public AbstractAuthenticatedActionsValve(AdapterDeploymentContext deploymentContext, Valve next, Container container) {
        this.deploymentContext = deploymentContext;
        if (next == null) throw new RuntimeException("Next valve is null!!!");
        setNext(next);
        setContainer(container);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.debugv("AuthenticatedActionsValve.invoke {0}", request.getRequestURI());
        CatalinaHttpFacade facade = new OIDCCatalinaHttpFacade(request, response);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment != null && deployment.isConfigured()) {
            AuthenticatedActionsHandler handler = new AuthenticatedActionsHandler(deployment, new OIDCCatalinaHttpFacade(request, response));
            if (handler.handledRequest()) {
                return;
            }

        }
        getNext().invoke(request, response);
    }
}
