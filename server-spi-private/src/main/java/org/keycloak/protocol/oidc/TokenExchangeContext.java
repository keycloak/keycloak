/*
 *  Copyright 2021 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.cors.Cors;

/**
 * Token exchange context
 *
 * @author <a href="mailto:dmitryt@backbase.com">Dmitry Telegin</a>
 */
public class TokenExchangeContext {

    private final KeycloakSession session;
    private final MultivaluedMap<String, String> formParams;

    private final Cors cors;
    private final Object tokenManager;

    private final ClientModel client;
    private final RealmModel realm;
    private final EventBuilder event;

    private final ClientConnection clientConnection;
    private final HttpHeaders headers;
    private final Map<String, String> clientAuthAttributes;

    private final Params params = new Params();
    private Set<String> restrictedScopes;

    // Reason why the particular tokenExchange provider cannot be supported
    private String unsupportedReason;

    public TokenExchangeContext(KeycloakSession session,
            MultivaluedMap<String, String> formParams,
            Cors cors,
            RealmModel realm,
            EventBuilder event,
            ClientModel client,
            ClientConnection clientConnection,
            HttpHeaders headers,
            Object tokenManager,
            Map<String, String> clientAuthAttributes) {
        this.session = session;
        this.formParams = formParams;
        this.cors = cors;
        this.client = client;
        this.realm = realm;
        this.event = event;
        this.clientConnection = clientConnection;
        this.headers = headers;
        this.tokenManager = tokenManager;
        this.clientAuthAttributes = clientAuthAttributes;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public MultivaluedMap<String, String> getFormParams() {
        return formParams;
    }

    public Cors getCors() {
        return cors;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public ClientModel getClient() {
        return client;
    }

    public EventBuilder getEvent() {
        return event;
    }

    public ClientConnection getClientConnection() {
        return clientConnection;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public Object getTokenManager() {
        return tokenManager;
    }

    public Map<String, String> getClientAuthAttributes() {
        return clientAuthAttributes;
    }

    public Params getParams() {
        return params;
    }

    public Set<String> getRestrictedScopes() {
        return restrictedScopes;
    }

    public void setRestrictedScopes(Set<String> restrictedScopes) {
        this.restrictedScopes = restrictedScopes;
    }

    public String getUnsupportedReason() {
        return unsupportedReason;
    }

    public void setUnsupportedReason(String unsupportedReason) {
        this.unsupportedReason = unsupportedReason;
    }

    public class Params {

        public String getActorToken() {
            return formParams.getFirst(OAuth2Constants.ACTOR_TOKEN);
        }

        public String getActorTokenType() {
            return formParams.getFirst(OAuth2Constants.ACTOR_TOKEN_TYPE);
        }

        public List<String> getAudience() {
            return formParams.get(OAuth2Constants.AUDIENCE);
        }

        public List<String> getResource() {
            return formParams.get(OAuth2Constants.RESOURCE);
        }

        public String getRequestedTokenType() {
            return formParams.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        }

        public String getScope() {
            return formParams.getFirst(OAuth2Constants.SCOPE);
        }

        public String getSubjectToken() {
            return formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN);
        }

        public String getSubjectTokenType() {
            return formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN_TYPE);
        }

        public String getSubjectIssuer() {
            return formParams.getFirst(OAuth2Constants.SUBJECT_ISSUER);
        }

    }

}
