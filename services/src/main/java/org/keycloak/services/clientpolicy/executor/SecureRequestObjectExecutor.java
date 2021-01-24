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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Time;
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

public class SecureRequestObjectExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(SecureRequestObjectExecutor.class);
    private static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    private String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode() + " :: EXECUTOR";
    }

    public static final String INVALID_REQUEST_OBJECT = "invalid_request_object";

    private final KeycloakSession session;

    public SecureRequestObjectExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Object config) {
        // no setup configuration item
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration {
    }

    @Override
    public String getProviderId() {
        return SecureRequestObjectExecutorFactory.PROVIDER_ID;
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
        ClientPolicyLogger.logv(logger, "{0} :: Authz Endpoint - authz request", logMsgPrefix());

        if (params == null) {
            ClientPolicyLogger.logv(logger, "{0} :: request parameter not exist.", logMsgPrefix());
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameters");
        }

        String requestParam = params.getFirst(OIDCLoginProtocol.REQUEST_PARAM);
        String requestUriParam = params.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);

        // check whether whether request object exists
        if (requestParam == null && requestUriParam == null) {
            ClientPolicyLogger.logv(logger, "{0} :: request object not exist.", logMsgPrefix());
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter");
        }

        JsonNode requestObject = (JsonNode)session.getAttribute(AuthzEndpointRequestParser.AUTHZ_REQUEST_OBJECT);

        // check whether request object exists
        if (requestObject == null || requestObject.isEmpty()) {
            ClientPolicyLogger.logv(logger, "{0} :: request object not exist.", logMsgPrefix());
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter");
        }

        // check whether scope exists in both query parameter and request object
        if (params.getFirst(OIDCLoginProtocol.SCOPE_PARAM) == null || requestObject.get(OIDCLoginProtocol.SCOPE_PARAM) == null) {
            ClientPolicyLogger.logv(logger, "{0} :: scope does not exists.", logMsgPrefix());
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter : scope");
        }

        // check whether "exp" claim exists
        if (requestObject.get("exp") == null) {
            ClientPolicyLogger.logv(logger, "{0} :: exp claim not incuded.", logMsgPrefix());
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Missing parameter : exp");
        }

        // check whether request object not expired
        long exp = requestObject.get("exp").asLong();
        if (Time.currentTime() > exp) { // TODO: Time.currentTime() is int while exp is long...
            ClientPolicyLogger.logv(logger, "{0} :: request object expired.", logMsgPrefix());
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Request Expired");
        }

        // check whether "aud" claim exists
        List<String> aud = new ArrayList<String>();
        JsonNode audience = requestObject.get("aud");
        if (audience == null) {
            ClientPolicyLogger.logv(logger, "{0} :: aud claim not incuded.", logMsgPrefix());
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Missing parameter : aud");
        }
        if (audience.isArray()) {
            for (JsonNode node : audience) aud.add(node.asText());
        } else {
            aud.add(audience.asText());
        }
        if (aud.isEmpty()) {
            ClientPolicyLogger.logv(logger, "{0} :: aud claim not incuded.", logMsgPrefix());
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Missing parameter : aud");
        }

        // check whether "aud" claim points to this keycloak as authz server
        String iss = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName());
        if (!aud.contains(iss)) {
            ClientPolicyLogger.logv(logger, "{0} :: aud not points to the intended realm.", logMsgPrefix());
            throw new ClientPolicyException(INVALID_REQUEST_OBJECT, "Invalid parameter : aud");
        }

        // confirm whether all parameters in query string are included in the request object, and have the same values
        // argument "request" are parameters overridden by parameters in request object
        if (AuthzEndpointRequestParser.KNOWN_REQ_PARAMS.stream().filter(s->params.containsKey(s)).anyMatch(s->!isSameParameterIncluded(s, params.getFirst(s), requestObject))) {
            ClientPolicyLogger.logv(logger, "{0} :: not all parameters in query string are included in the request object, and have the same values.", logMsgPrefix());
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter");
        }

        ClientPolicyLogger.logv(logger, "{0} :: Passed.", logMsgPrefix());
    }

    private boolean isSameParameterIncluded(String param, String value, JsonNode requestObject) {
        if (param.equals(OIDCLoginProtocol.REQUEST_PARAM) || param.equals(OIDCLoginProtocol.REQUEST_URI_PARAM)) return true;
        if (requestObject.hasNonNull(param)) return requestObject.get(param).asText().equals(value);
        return false;
    }

}