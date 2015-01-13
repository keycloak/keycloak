/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.oidc;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.AuthenticationResponse;
import org.keycloak.broker.provider.FederatedIdentity;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Pedro Igor
 */
public abstract class AbstractOAuth2IdentityProvider<C extends OAuth2IdentityProviderConfig> extends AbstractIdentityProvider<C> {

    public static final String OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    protected static ObjectMapper mapper = new ObjectMapper();

    public static final String OAUTH2_PARAMETER_ACCESS_TOKEN = "access_token";
    public static final String OAUTH2_PARAMETER_SCOPE = "scope";
    public static final String OAUTH2_PARAMETER_STATE = "state";
    public static final String OAUTH2_PARAMETER_RESPONSE_TYPE = "response_type";
    public static final String OAUTH2_PARAMETER_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH2_PARAMETER_CODE = "code";
    public static final String OAUTH2_PARAMETER_CLIENT_ID = "client_id";
    public static final String OAUTH2_PARAMETER_CLIENT_SECRET = "client_secret";
    public static final String OAUTH2_PARAMETER_GRANT_TYPE = "grant_type";

    public AbstractOAuth2IdentityProvider(C config) {
        super(config);
    }

    @Override
    public AuthenticationResponse handleRequest(AuthenticationRequest request) {
        try {
            URI authorizationUrl = createAuthorizationUrl(request).build();

            return AuthenticationResponse.temporaryRedirect(authorizationUrl);
        } catch (Exception e) {
            throw new RuntimeException("Could not create authentication request.", e);
        }
    }

    @Override
    public String getRelayState(AuthenticationRequest request) {
        UriInfo uriInfo = request.getUriInfo();
        return uriInfo.getQueryParameters().getFirst(OAUTH2_PARAMETER_STATE);
    }

    @Override
    public AuthenticationResponse handleResponse(AuthenticationRequest request) {
        UriInfo uriInfo = request.getUriInfo();
        String error = uriInfo.getQueryParameters().getFirst(OAuth2Constants.ERROR);

        if (error != null) {
            if (error.equals("access_denied")) {
                throw new RuntimeException("Access denied.");
            } else {
                throw new RuntimeException(error);
            }
        }

        try {
            String authorizationCode = uriInfo.getQueryParameters().getFirst(OAUTH2_PARAMETER_CODE);

            if (authorizationCode != null) {
                String response = SimpleHttp.doPost(getConfig().getTokenUrl())
                        .param(OAUTH2_PARAMETER_CODE, authorizationCode)
                        .param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                        .param(OAUTH2_PARAMETER_CLIENT_SECRET, getConfig().getClientSecret())
                        .param(OAUTH2_PARAMETER_REDIRECT_URI, request.getRedirectUri())
                        .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE).asString();

                return doHandleResponse(response);
            }

            throw new RuntimeException("No authorization code from identity provider.");
        } catch (Exception e) {
            throw new RuntimeException("Could not process response from identity provider.", e);
        }
    }

    protected AuthenticationResponse doHandleResponse(String response) throws IOException {
        String token = extractTokenFromResponse(response, OAUTH2_PARAMETER_ACCESS_TOKEN);

        if (token == null) {
            throw new RuntimeException("No access token from server.");
        }

        return AuthenticationResponse.end(getFederatedIdentity(token));
    }

    protected String extractTokenFromResponse(String response, String tokenName) throws IOException {
        if (response.startsWith("{")) {
            return mapper.readTree(response).get(tokenName).getTextValue();
        } else {
            Matcher matcher = Pattern.compile(tokenName + "=([^&]+)").matcher(response);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    protected FederatedIdentity getFederatedIdentity(String accessToken) {
        throw new RuntimeException("Not implemented.");
    };

    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        return UriBuilder.fromPath(getConfig().getAuthorizationUrl())
                .queryParam(OAUTH2_PARAMETER_SCOPE, getConfig().getDefaultScope())
                .queryParam(OAUTH2_PARAMETER_STATE, request.getState())
                .queryParam(OAUTH2_PARAMETER_RESPONSE_TYPE, "code")
                .queryParam(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                .queryParam(OAUTH2_PARAMETER_REDIRECT_URI, request.getRedirectUri());
    }

    protected String getJsonProperty(JsonNode jsonNode, String name) {
        if (jsonNode.has(name)) {
            return jsonNode.get(name).asText();
        }

        return null;
    }

    protected JsonNode asJsonNode(String json) throws IOException {
        return mapper.readTree(json);
    }
}
