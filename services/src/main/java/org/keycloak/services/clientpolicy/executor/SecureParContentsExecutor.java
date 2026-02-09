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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestObjectParser;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.protocol.oidc.endpoints.request.RequestUriType;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;

import org.jboss.logging.Logger;

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
        MultivaluedMap<String, String> requestParametersFromQuery = preAuthorizationRequestContext.getRequestParameters();
        String requestUri = requestParametersFromQuery.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);
        if (requestUri == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "request_uri not included.");
        }
        if (requestUri != null && AuthorizationEndpointRequestParserProcessor.getRequestUriType(requestUri) != RequestUriType.PAR) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR request_uri not included.");
        }

        String key = requestUri.substring(ParEndpoint.REQUEST_URI_PREFIX_LENGTH);
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        Map<String, String> requestParametersFromPAR = singleUseStore.get(key);
        if (requestParametersFromPAR == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR not found. not issued or used multiple times.");
        }

        Set<String> requestParametersNameFromPAR = new HashSet<>();
        if (requestParametersFromPAR.containsKey(OIDCLoginProtocol.REQUEST_PARAM)) {
            // if PAR request includes request object (JAR), parsing the request is needed.
            requestParametersNameFromPAR = getParRetrievedRequestParameters(requestParametersFromPAR, preAuthorizationRequestContext.getClientId());
        } else {
            requestParametersNameFromPAR = requestParametersFromPAR.keySet();
        }

        for (String queryParamName : requestParametersFromQuery.keySet()) {
            if (!requestParametersNameFromPAR.contains(queryParamName) && !OIDCLoginProtocol.REQUEST_URI_PARAM.equals(queryParamName)) {
                singleUseStore.remove(key);
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR request did not include necessary parameters");
            }
        }
    }

    private Set<String> getParRetrievedRequestParameters(Map<String, String> requestParametersFromPAR, String clientId) {
        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest();
        Set<String> parRetrievedRequest = new HashSet<>();
        String requestObjectString  = requestParametersFromPAR.get(OIDCLoginProtocol.REQUEST_PARAM);
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = realm.getClientByClientId(clientId);

        new AuthzEndpointRequestObjectParser(session, requestObjectString, client).parseRequest(request);

        // from PAR request parameters other than ones included in a request object
        for (String param : requestParametersFromPAR.keySet()) {
            if (OIDCLoginProtocol.REQUEST_PARAM.equals(param)) continue;
            parRetrievedRequest.add(param);
        }
        // from parsed PAR request parameters
        AuthorizationEndpoint.performActionOnParameters(request, (paramName, paramValue) -> {if (paramValue != null) parRetrievedRequest.add(paramName);});
        if (request.getClientId() != null) parRetrievedRequest.add(OIDCLoginProtocol.CLIENT_ID_PARAM);
        if (request.getResponseType() != null) parRetrievedRequest.add(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        if (request.getRedirectUriParam() != null) parRetrievedRequest.add(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        if (request.getMaxAge() != null) parRetrievedRequest.add(OIDCLoginProtocol.MAX_AGE_PARAM);
        if (request.getUiLocales() != null) parRetrievedRequest.add(OAuth2Constants.UI_LOCALES_PARAM);
        for (String additionalParam : request.getAdditionalReqParams().keySet()) {
            if (!AuthzEndpointRequestParser.KNOWN_REQ_PARAMS.contains(additionalParam)) {
                parRetrievedRequest.add(additionalParam);
            }
        }

        return parRetrievedRequest;
    }
}
