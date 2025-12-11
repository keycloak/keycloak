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
package org.keycloak.models.utils;

import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ModelException;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlowResolver {

    private static final Logger logger = Logger.getLogger(AuthenticationFlowResolver.class);

    public static AuthenticationFlowModel resolveBrowserFlow(AuthenticationSessionModel authSession) {
        AuthenticationFlowModel flow = null;
        ClientModel client = authSession.getClient();

        // check if specific flow has been requested
        String requestedFlowAlias = authSession.getAuthNote(Constants.REQUESTED_AUTHENTICATION_FLOW);
        if (requestedFlowAlias != null){
            flow = authSession.getRealm().getFlowByAlias(requestedFlowAlias);
            // validate flow exists
            if (flow == null){
                throw new ModelException("Client " + client.getClientId() + " has requested browser flow " + requestedFlowAlias + ", but this flow does not exist.");
            } else {
                return flow;
            }
        }

        String clientFlow = client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.BROWSER_BINDING);
        if (clientFlow != null) {
            flow = authSession.getRealm().getAuthenticationFlowById(clientFlow);
            if (flow != null) {
                return flow;
            }
            logger.warnf("Client %s has browser flow override, but this flow '%s' does not exist, " +
                    "fallback to browser flow", client.getClientId(), clientFlow);
        }
        return authSession.getRealm().getBrowserFlow();
    }
    public static AuthenticationFlowModel resolveDirectGrantFlow(AuthenticationSessionModel authSession) {
        AuthenticationFlowModel flow = null;
        ClientModel client = authSession.getClient();

        // check if specific flow has been requested
        String requestedFlowAlias = authSession.getAuthNote(Constants.REQUESTED_AUTHENTICATION_FLOW);
        if (requestedFlowAlias != null){
            flow = authSession.getRealm().getFlowByAlias(requestedFlowAlias);
            // validate flow exists
            if (flow == null){
                throw new ModelException("Client " + client.getClientId() + " has requested browser flow " + requestedFlowAlias + ", but this flow does not exist.");
            } else {
                return flow;
            }
        }

        String clientFlow = client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.DIRECT_GRANT_BINDING);
        if (clientFlow != null) {
            flow = authSession.getRealm().getAuthenticationFlowById(clientFlow);
            if (flow != null) {
                return flow;
            }
            logger.warnf("Client %s has direct grant flow override, but this flow '%s' does not exist, " +
                    "fallback to direct grant flow", client.getClientId(), clientFlow);
        }
        return authSession.getRealm().getDirectGrantFlow();
    }
}
