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
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestObjectParser;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.protocol.oidc.endpoints.request.RequestUriType;
import org.keycloak.protocol.oidc.par.clientpolicy.context.PushedAuthorizationRequestContext;
import org.keycloak.protocol.oidc.par.endpoints.request.AuthzEndpointParParser;
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
    private static final Logger log = Logger.getLogger(SecureParContentsExecutor.class);

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
            case PUSHED_AUTHORIZATION_REQUEST -> {
                PushedAuthorizationRequestContext parAuthorizationRequestContext = (PushedAuthorizationRequestContext)context;
                checkParAuthorizationRequest(parAuthorizationRequestContext);
            }
            case PRE_AUTHORIZATION_REQUEST -> {
                PreAuthorizationRequestContext preAuthorizationRequestContext = (PreAuthorizationRequestContext)context;
                checkPreAuthorizationRequest(preAuthorizationRequestContext);
            }
        }
    }

    private void checkParAuthorizationRequest(PushedAuthorizationRequestContext context) throws ClientPolicyException {
        // FAPI 2.0: For authorization-endpoint flows, ASs “shall require the redirect_uri parameter in pushed authorization requests”
        // and clients must send only client_id and request_uri to the authorization endpoint afterward.
        AuthorizationEndpointRequest request = context.getRequest();
        if (request.getRedirectUri() == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR is required to have a 'redirect_uri' parameter");
        }
    }

    private void checkPreAuthorizationRequest(PreAuthorizationRequestContext context) throws ClientPolicyException {
        MultivaluedMap<String, String> requestParametersFromQuery = context.getRequestParameters();
        String requestUri = requestParametersFromQuery.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);
        if (requestUri == null || AuthorizationEndpointRequestParserProcessor.getRequestUriType(requestUri) != RequestUriType.PAR) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "PAR request_uri not included.");
        }

        Map<String, String> requestParametersFromPAR = AuthzEndpointParParser.getRequestObject(session, requestUri);
        if (requestParametersFromPAR == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST_URI, "PAR not found, not issued or used multiple times.");
        }

        Set<String> requestParameterKeysFromPAR;
        if (requestParametersFromPAR.containsKey(OIDCLoginProtocol.REQUEST_PARAM)) {
            // if PAR request includes request object (JAR), parsing the request is needed.
            requestParameterKeysFromPAR = getParRetrievedRequestParameters(requestParametersFromPAR, context.getClientId());
        } else {
            requestParameterKeysFromPAR = requestParametersFromPAR.keySet();
        }

        List<String> requestParameterKeysFromQuery = requestParametersFromQuery.keySet().stream()
                .filter(it -> !it.equals(OIDCLoginProtocol.REQUEST_URI_PARAM))
                .toList();

        // FAPI says only parameters inside the request object should be used
        //
        for (String queryParam : requestParameterKeysFromQuery) {
            if (!requestParameterKeysFromPAR.contains(queryParam)) {
                AuthzEndpointParParser.removeRequestObject(session, requestUri);
                log.warnf("PAR request did not include query parameter: %s", queryParam);
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST_OBJECT, "PAR request did not include query parameter");
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
        if (request.getRedirectUri() != null) parRetrievedRequest.add(OIDCLoginProtocol.REDIRECT_URI_PARAM);
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
