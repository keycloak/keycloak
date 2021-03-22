/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;

import com.fasterxml.jackson.databind.JsonNode;

public class SecureRequestObjectExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(SecureRequestObjectExecutor.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public static final String INVALID_REQUEST_OBJECT = "invalid_request_object";

    public SecureRequestObjectExecutor(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
                AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext)context;
                executeOnAuthorizationRequest(authorizationRequestContext.getparsedResponseType(),
                    authorizationRequestContext.getAuthorizationEndpointRequest(),
                    authorizationRequestContext.getRedirectUri(),
                    authorizationRequestContext.getRequestParameters());
                break;
            default:
                return;
        }
    }

    private void executeOnAuthorizationRequest(
            OIDCResponseType parsedResponseType,
            AuthorizationEndpointRequest request,
            String redirectUri,
            MultivaluedMap<String, String> params) throws ClientPolicyException {
        ClientPolicyLogger.log(logger, "Authz Endpoint - authz request");

        if (params == null) {
            ClientPolicyLogger.log(logger, "request parameter not exist.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameters");
        }

        String requestParam = params.getFirst(OIDCLoginProtocol.REQUEST_PARAM);
        String requestUriParam = params.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);

        // check whether whether request object exists
        if (requestParam == null && requestUriParam == null) {
            ClientPolicyLogger.log(logger, "request object not exist.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter");
        }

        JsonNode requestObject = (JsonNode)session.getAttribute(AuthzEndpointRequestParser.AUTHZ_REQUEST_OBJECT);

        // check whether request object exists
        if (requestObject == null || requestObject.isEmpty()) {
            ClientPolicyLogger.log(logger, "request object not exist.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter");
        }

        // check whether scope exists in both query parameter and request object
        if (params.getFirst(OIDCLoginProtocol.SCOPE_PARAM) == null || requestObject.get(OIDCLoginProtocol.SCOPE_PARAM) == null) {
            ClientPolicyLogger.log(logger, "scope does not exists.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter : scope");
        }

        // check whether "exp" claim exists
        if (requestObject.get("exp") == null) {
            ClientPolicyLogger.log(logger, "exp claim not incuded.");
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Missing parameter : exp");
        }

        // check whether request object not expired
        long exp = requestObject.get("exp").asLong();
        if (Time.currentTime() > exp) { // TODO: Time.currentTime() is int while exp is long...
            ClientPolicyLogger.log(logger, "request object expired.");
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Request Expired");
        }

        // check whether "aud" claim exists
        List<String> aud = new ArrayList<String>();
        JsonNode audience = requestObject.get("aud");
        if (audience == null) {
            ClientPolicyLogger.log(logger, "aud claim not incuded.");
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Missing parameter : aud");
        }
        if (audience.isArray()) {
            for (JsonNode node : audience) aud.add(node.asText());
        } else {
            aud.add(audience.asText());
        }
        if (aud.isEmpty()) {
            ClientPolicyLogger.log(logger, "aud claim not incuded.");
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Missing parameter : aud");
        }

        // check whether "aud" claim points to this keycloak as authz server
        String iss = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName());
        if (!aud.contains(iss)) {
            ClientPolicyLogger.log(logger, "aud not points to the intended realm.");
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Invalid parameter : aud");
        }

        // confirm whether all parameters in query string are included in the request object, and have the same values
        // argument "request" are parameters overridden by parameters in request object
        if (AuthzEndpointRequestParser.KNOWN_REQ_PARAMS.stream().filter(s->params.containsKey(s)).anyMatch(s->!isSameParameterIncluded(s, params.getFirst(s), requestObject))) {
            ClientPolicyLogger.log(logger, "not all parameters in query string are included in the request object, and have the same values.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter");
        }

        ClientPolicyLogger.log(logger, "Passed.");
    }

    private boolean isSameParameterIncluded(String param, String value, JsonNode requestObject) {
        if (param.equals(OIDCLoginProtocol.REQUEST_PARAM) || param.equals(OIDCLoginProtocol.REQUEST_URI_PARAM)) return true;
        if (requestObject.hasNonNull(param)) return requestObject.get(param).asText().equals(value);
        return false;
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

}