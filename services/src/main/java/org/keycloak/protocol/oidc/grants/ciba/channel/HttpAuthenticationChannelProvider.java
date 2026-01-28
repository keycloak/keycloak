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
package org.keycloak.protocol.oidc.grants.ciba.channel;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;
import org.keycloak.util.TokenUtil;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class HttpAuthenticationChannelProvider implements AuthenticationChannelProvider{

    public static final String AUTHENTICATION_CHANNEL_ID = "authentication_channel_id";

    protected KeycloakSession session;
    protected MultivaluedMap<String, String> formParams;
    protected RealmModel realm;
    protected Map<String, String> clientAuthAttributes;
    protected Cors cors;
    protected final String httpAuthenticationChannelUri;

    public HttpAuthenticationChannelProvider(KeycloakSession session, String httpAuthenticationRequestUri) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.httpAuthenticationChannelUri = httpAuthenticationRequestUri;
    }

    @Override
    public boolean requestAuthentication(CIBAAuthenticationRequest request, String infoUsedByAuthenticator) {
        // Creates JWT formatted/JWS signed/JWE encrypted Authentication Channel ID by the same manner in creating auth_req_id.
        // Authentication Channel ID binds Backchannel Authentication Request with Authentication by Authentication Device (AD).
        // JWE serialized Authentication Channel ID works as a bearer token. It includes client_id 
        // that can be used on Authentication Channel Callback Endpoint to recognize the Consumption Device (CD)
        // that sent Backchannel Authentication Request.

        // The following scopes should be displayed on AD:
        // 1. scopes specified explicitly as query parameter in the authorization request
        // 2. scopes specified implicitly as default client scope in keycloak

        checkAuthenticationChannel();

        ClientModel client = request.getClient();

        try {
            AuthenticationChannelRequest channelRequest = new AuthenticationChannelRequest();

            channelRequest.setScope(request.getScope());
            channelRequest.setBindingMessage(request.getBindingMessage());
            channelRequest.setLoginHint(infoUsedByAuthenticator);
            channelRequest.setConsentRequired(client.isConsentRequired());
            channelRequest.setAcrValues(request.getAcrValues());
            channelRequest.setAdditionalParameters(request.getOtherClaims());

            SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doPost(httpAuthenticationChannelUri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .json(channelRequest)
                    .auth(createBearerToken(request, client));
 
            int status = completeDecoupledAuthnRequest(simpleHttp, channelRequest).asStatus();

            if (status == Status.CREATED.getStatusCode()) {
                return true;
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Authentication Channel Access failed.", ioe);
        }

        return false;
    }

    private String createBearerToken(CIBAAuthenticationRequest request, ClientModel client) {
        AccessToken bearerToken = new AccessToken();

        bearerToken.type(TokenUtil.TOKEN_TYPE_BEARER);
        bearerToken.issuer(request.getIssuer());
        bearerToken.id(request.getAuthResultId());
        bearerToken.issuedFor(client.getClientId());
        bearerToken.audience(request.getIssuer());
        bearerToken.iat(request.getIat());
        bearerToken.exp(request.getExp());
        bearerToken.subject(request.getSubject());

        return session.tokens().encode(bearerToken);
    }

    protected void checkAuthenticationChannel() {
        if (httpAuthenticationChannelUri == null) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
        if (!httpAuthenticationChannelUri.startsWith("http://") && !httpAuthenticationChannelUri.startsWith("https://")) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
    }

    /**
     * Extension point to allow subclass to override this method in order to add data to post to decoupled server.
     */
    protected SimpleHttpRequest completeDecoupledAuthnRequest(SimpleHttpRequest simpleHttp, AuthenticationChannelRequest channelRequest) {
        return simpleHttp;
    }

    @Override
    public void close() {

    }
}
