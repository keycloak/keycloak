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

import static org.keycloak.OAuth2Constants.SCOPE;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.services.resources.Cors;

public class HttpAuthenticationChannelProvider implements AuthenticationChannelProvider{

    public static final String AUTHENTICATION_CHANNEL_ID = "authentication_channel_id";
    public static final String AUTHENTICATION_CHANNEL_USER_INFO = "user_info";
    public static final String AUTHENTICATION_STATUS = "auth_result";
    public static final String AUTHENTICATION_CHANNEL_IS_CONSENT_REQUIRED = "is_consent_required";

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
    public boolean requestAuthentication(AuthenticationRequest request, String infoUsedByAuthenticator) {
        // create JWT formatted/JWS signed/JWE encrypted Authentication Channel ID by the same manner in creating auth_req_id
        // Authentication Channel ID binds Backchannel Authentication Request with Authentication by AD(Authentication Device).
        // By including userSessionIdWillBeCreated. keycloak can create UserSession whose ID is userSessionIdWillBeCreated on Authentication Channel Callback Endpoint,
        // which can bind userSessionIdWillBeCreated (namely, Backchannel Authentication Request) with authenticated UserSession.
        // By including authId, keycloak can create Authentication Channel Result of Authentication by AD on Authentication Channel Callback Endpoint,
        // which can bind authId with Authentication Channel Result of Authentication by AD.
        // By including client_id, Authentication Channel Callback Endpoint can recognize the CD(Consumption Device) who sent Backchannel Authentication Request.

        // The following scopes should be displayed on AD(Authentication Device):
        // 1. scopes specified explicitly as query parameter in the authorization request
        // 2. scopes specified implicitly as default client scope in keycloak

        checkAuthenticationChannel();

        ClientModel client = request.getClient();

        try {
            // TODO: we should probably just pass the serialized authentication request
            int status = SimpleHttp.doPost(httpAuthenticationChannelUri, session)
                .param(AUTHENTICATION_CHANNEL_ID, request.serialize(session))
                .param(AUTHENTICATION_CHANNEL_USER_INFO, infoUsedByAuthenticator)
                .param(AUTHENTICATION_CHANNEL_IS_CONSENT_REQUIRED, Boolean.toString(client.isConsentRequired()))
                .param(SCOPE, request.getScope())
                .param(BINDING_MESSAGE, request.getBindingMessage()).asStatus();

            if (status == Status.CREATED.getStatusCode()) {
                return true;
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Authentication Channel Access failed.", ioe);
        }

        return false;
    }

    protected void checkAuthenticationChannel() {
        if (httpAuthenticationChannelUri == null) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
        if (!httpAuthenticationChannelUri.startsWith("http://") && !httpAuthenticationChannelUri.startsWith("https://")) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
    }

    @Override
    public void close() {

    }
}
