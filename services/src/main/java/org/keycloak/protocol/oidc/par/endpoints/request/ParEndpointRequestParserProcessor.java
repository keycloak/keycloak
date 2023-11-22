/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.par.endpoints.request;

import java.util.HashSet;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointQueryStringParser;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.AuthorizationContextUtil;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ParEndpointRequestParserProcessor {

    public static AuthorizationEndpointRequest parseRequest(EventBuilder event, KeycloakSession session, ClientModel client, MultivaluedMap<String, String> requestParams) {
        try {
            AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();

            AuthzEndpointQueryStringParser parser = new AuthzEndpointQueryStringParser(session, requestParams, false);
            parser.parseRequest(request);

            if (parser.getInvalidRequestMessage() != null) {
                request.setInvalidRequestMessage(parser.getInvalidRequestMessage());
            }
            if (request.getInvalidRequestMessage() != null) {
                return request;
            }

            String requestParam = requestParams.getFirst(OIDCLoginProtocol.REQUEST_PARAM);
            String requestUriParam = requestParams.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);

            if (requestParam != null && requestUriParam != null) {
                throw new RuntimeException("Illegal to use both 'request' and 'request_uri' parameters together");
            }

            String requestObjectRequired = OIDCAdvancedConfigWrapper.fromClientModel(client).getRequestObjectRequired();

            if (OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_OR_REQUEST_URI.equals(requestObjectRequired)
                    && requestParam == null && requestUriParam == null) {
                throw new RuntimeException("Client is required to use 'request' or 'request_uri' parameter.");
            } else if (OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST.equals(requestObjectRequired)
                    && requestParam == null) {
                throw new RuntimeException("Client is required to use 'request' parameter.");
            } else if (OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST_URI.equals(requestObjectRequired)
                    && requestUriParam == null) {
                throw new RuntimeException("Client is required to use 'request_uri' parameter.");
            }

            if (requestParam != null) {
                new ParEndpointRequestObjectParser(session, requestParam, client).parseRequest(request);
            } else if (requestUriParam != null) {
                // Validate "requestUriParam" with allowed requestUris
                List<String> requestUris = OIDCAdvancedConfigWrapper.fromClientModel(client).getRequestUris();
                String requestUri = RedirectUtils.verifyRedirectUri(session, client.getRootUrl(), requestUriParam, new HashSet<>(requestUris), false);
                if (requestUri == null) {
                    throw new RuntimeException("Specified 'request_uri' not allowed for this client.");
                }
                String retrievedRequest = session.getProvider(HttpClientProvider.class).getString(requestUri);
                new ParEndpointRequestObjectParser(session, retrievedRequest, client).parseRequest(request);
            }

            if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
                request.setAuthorizationRequestContext(AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, request.getScope()));
            }

            return request;

        } catch (Exception e) {
            ServicesLogger.LOGGER.invalidRequest(e);
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }
    }

}
