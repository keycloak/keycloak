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

package org.keycloak.services.clientpolicy.executor;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureResponseTypeExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfiguration> {

    private static final Logger logger = Logger.getLogger(SecureResponseTypeExecutor.class);

    protected final KeycloakSession session;

    public SecureResponseTypeExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return SecureResponseTypeExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
                AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext)context;
                executeOnAuthorizationRequest(authorizationRequestContext.getparsedResponseType(),
                    authorizationRequestContext.getAuthorizationEndpointRequest(),
                    authorizationRequestContext.getRedirectUri());
                break;
            default:
        }
        return;
    }

    // on Authorization Endpoint access for authorization request
    public void executeOnAuthorizationRequest(
            OIDCResponseType parsedResponseType,
            AuthorizationEndpointRequest request,
            String redirectUri) throws ClientPolicyException {
        logger.trace("Authz Endpoint - authz request");

        if (parsedResponseType.hasResponseType(OIDCResponseType.CODE) && parsedResponseType.hasResponseType(OIDCResponseType.ID_TOKEN)) {
            if (parsedResponseType.hasResponseType(OIDCResponseType.TOKEN)) {
                logger.trace("Passed. response_type = code id_token token");
            } else {
                logger.trace("Passed. response_type = code id_token");
            }
            return;
        }

        logger.tracev("invalid response_type = {0}", parsedResponseType);
        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid response_type");
    }

}
