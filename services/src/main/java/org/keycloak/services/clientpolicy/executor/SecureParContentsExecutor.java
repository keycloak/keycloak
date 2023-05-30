/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.endpoints.request.RequestUriType;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;

import jakarta.ws.rs.core.MultivaluedMap;

/** 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureParContentsExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    protected final KeycloakSession session;
    private static final Logger logger = Logger.getLogger(SecureParContentsExecutor.class);

    public SecureParContentsExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return SecureParContentsExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case PRE_AUTHORIZATION_REQUEST:
                PreAuthorizationRequestContext preAuthorizationRequestContext = (PreAuthorizationRequestContext)context;
                checkValidParContents(preAuthorizationRequestContext);
                break;
            default:
                return;
        }
    }

    private void checkValidParContents(PreAuthorizationRequestContext preAuthorizationRequestContext) throws ClientPolicyException {
        MultivaluedMap<String, String> requestParameters = preAuthorizationRequestContext.getRequestParameters();
        String requestUri = requestParameters.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);
        if (requestUri == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "request_uri not included.");
        }
        if (requestUri != null && AuthorizationEndpointRequestParserProcessor.getRequestUriType(requestUri) != RequestUriType.PAR) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR request_uri not included.");
        }

        String key = requestUri.substring(ParEndpoint.REQUEST_URI_PREFIX_LENGTH);
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        Map<String, String> retrievedRequest = singleUseStore.get(key);
        if (retrievedRequest == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR not found. not issued or used multiple times.");
        }

        Set<String> queryParameterNames = requestParameters.keySet();
        for (String queryParamName : queryParameterNames) {
            if (!retrievedRequest.keySet().contains(queryParamName) && !OIDCLoginProtocol.REQUEST_URI_PARAM.equals(queryParamName)) {
                singleUseStore.remove(key);
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR request did not include necessary parameters");
            }
        }
    }
}
