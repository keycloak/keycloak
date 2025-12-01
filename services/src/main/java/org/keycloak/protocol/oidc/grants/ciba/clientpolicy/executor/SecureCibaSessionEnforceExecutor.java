/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelAuthenticationRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequest;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureCibaSessionEnforceExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    private static final Logger logger = Logger.getLogger(SecureCibaSessionEnforceExecutor.class);

    private final KeycloakSession session;

    public SecureCibaSessionEnforceExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return SecureCibaSessionEnforceExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case BACKCHANNEL_AUTHENTICATION_REQUEST:
                BackchannelAuthenticationRequestContext backchannelAuthenticationRequestContext = (BackchannelAuthenticationRequestContext)context;
                executeOnBackchannelAuthenticationRequest(backchannelAuthenticationRequestContext.getRequest(),
                    backchannelAuthenticationRequestContext.getRequestParameters());
                return;
            default:
                return;
        }
    }

    private void executeOnBackchannelAuthenticationRequest(
            BackchannelAuthenticationEndpointRequest request,
            MultivaluedMap<String, String> requestParameters) throws ClientPolicyException {
        logger.trace("Backchannel Authentication Endpoint - authn request");
        if (request.getBindingMessage() == null) {
            logger.trace("Missing parameter: binding_message");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter: binding_message");
        }
        logger.trace("Passed.");
    }
}
