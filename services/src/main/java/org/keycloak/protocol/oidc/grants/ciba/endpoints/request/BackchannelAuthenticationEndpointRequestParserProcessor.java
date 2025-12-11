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
 *
 */

package org.keycloak.protocol.oidc.grants.ciba.endpoints.request;

import java.util.HashSet;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorResponseException;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class BackchannelAuthenticationEndpointRequestParserProcessor {

    public static BackchannelAuthenticationEndpointRequest parseRequest(EventBuilder event, KeycloakSession session, ClientModel client, MultivaluedMap<String, String> requestParams, CibaConfig config) {
        try {
            BackchannelAuthenticationEndpointRequest request = new BackchannelAuthenticationEndpointRequest();

            BackchannelAuthenticationEndpointRequestBodyParser parser = new BackchannelAuthenticationEndpointRequestBodyParser(requestParams);
            parser.parseRequest(request);

            if (parser.getInvalidRequestMessage() != null) {
                request.invalidRequestMessage = parser.getInvalidRequestMessage();
                return request;
            }

            String requestParam = requestParams.getFirst(OIDCLoginProtocol.REQUEST_PARAM);
            String requestUriParam = requestParams.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);

            if (requestParam != null && requestUriParam != null) {
                throw new RuntimeException("Illegal to use both 'request' and 'request_uri' parameters together");
            }

            if (requestParam != null) {
                new BackchannelAuthenticationEndpointSignedRequestParser(session, requestParam, client, config).parseRequest(request);
            } else if (requestUriParam != null) {
                // Validate "requestUriParam" with allowed requestUris
                List<String> requestUris = OIDCAdvancedConfigWrapper.fromClientModel(client).getRequestUris();
                String requestUri = RedirectUtils.verifyRedirectUri(session, client.getRootUrl(), requestUriParam, new HashSet<>(requestUris), false);
                if (requestUri == null) {
                    throw new RuntimeException("Specified 'request_uri' not allowed for this client.");
                }

                String retrievedRequest = session.getProvider(HttpClientProvider.class).getString(requestUri);
                new BackchannelAuthenticationEndpointSignedRequestParser(session, retrievedRequest, client, config).parseRequest(request);
            }

            return request;

        } catch (Exception e) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }
}
