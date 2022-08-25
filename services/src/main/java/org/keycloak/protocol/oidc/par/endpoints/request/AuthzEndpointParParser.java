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

package org.keycloak.protocol.oidc.par.endpoints.request;

import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;

import static org.keycloak.protocol.oidc.par.endpoints.ParEndpoint.PAR_CREATED_TIME;

/**
 * Parse the parameters from PAR
 *
 */
public class AuthzEndpointParParser extends AuthzEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(AuthzEndpointParParser.class);

    private final KeycloakSession session;
    private final ClientModel client;
    private Map<String, String> requestParams;
    private String invalidRequestMessage = null;

    public AuthzEndpointParParser(KeycloakSession session, ClientModel client, String requestUri) {
        this.session = session;
        this.client = client;
        SingleUseObjectProvider singleUseStore = session.getProvider(SingleUseObjectProvider.class);
        String key;
        try {
            key = requestUri.substring(ParEndpoint.REQUEST_URI_PREFIX_LENGTH);
        } catch (RuntimeException re) {
            logger.warnf(re,"Unable to parse request_uri: %s", requestUri);
            throw new RuntimeException("Unable to parse request_uri");
        }
        Map<String, String> retrievedRequest = singleUseStore.remove(key);
        if (retrievedRequest == null) {
            throw new RuntimeException("PAR not found. not issued or used multiple times.");
        }

        RealmModel realm = session.getContext().getRealm();
        int expiresIn = realm.getParPolicy().getRequestUriLifespan();
        long created = Long.parseLong(retrievedRequest.get(PAR_CREATED_TIME));
        if (System.currentTimeMillis() - created < (expiresIn * 1000)) {
            requestParams = retrievedRequest;
        } else {
            throw new RuntimeException("PAR expired.");
        }
    }

    @Override
    public void parseRequest(AuthorizationEndpointRequest request) {
        String requestParam = requestParams.get(OIDCLoginProtocol.REQUEST_PARAM);

        if (requestParam != null) {
            // parses the request object if PAR was registered using JAR
            // parameters from requets object have precedence over those sent directly in the request
            new ParEndpointRequestObjectParser(session, requestParam, client).parseRequest(request);
        } else {
            super.parseRequest(request);
        }
    }

    @Override
    protected String getParameter(String paramName) {
        return requestParams.get(paramName);
    }

    @Override
    protected Integer getIntParameter(String paramName) {
        String paramVal = requestParams.get(paramName);
        return paramVal == null ? null : Integer.valueOf(paramVal);
    }

    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }

    @Override
    protected Set<String> keySet() {
        return requestParams.keySet();
    }

}
