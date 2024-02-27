/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.util.Map;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.services.cors.Cors;

/**
 * Provider interface for OAuth 2.0 grant types
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public interface OAuth2GrantType extends Provider {

    /**
     * Returns the event type associated with this OAuth 2.0 grant type.
     *
     * @return event type
     */
    EventType getEventType();

    /**
     * Processes grant request.
     * @param context grant request context
     *
     * @return token response
     */
    Response process(Context context);

    public static class Context {
        protected KeycloakSession session;
        protected RealmModel realm;
        protected ClientModel client;
        protected Object clientConfig;
        protected ClientConnection clientConnection;
        protected Map<String, String> clientAuthAttributes;
        protected HttpRequest request;
        protected HttpResponse response;
        protected HttpHeaders headers;
        protected MultivaluedMap<String, String> formParams;
        protected EventBuilder event;
        protected Cors cors;
        protected Object tokenManager;
        protected DPoP dPoP;

        public Context(KeycloakSession session, Object clientConfig, Map<String, String> clientAuthAttributes,
                MultivaluedMap<String, String> formParams, EventBuilder event, Cors cors, Object tokenManager, DPoP dPoP) {
            this.session = session;
            this.realm = session.getContext().getRealm();
            this.client = session.getContext().getClient();
            this.clientConfig = clientConfig;
            this.clientConnection = session.getContext().getConnection();
            this.clientAuthAttributes = clientAuthAttributes;
            this.request = session.getContext().getHttpRequest();
            this.response = session.getContext().getHttpResponse();
            this.headers = session.getContext().getRequestHeaders();
            this.formParams = formParams;
            this.event = event;
            this.cors = cors;
            this.tokenManager = tokenManager;
            this.dPoP = dPoP;
        }

        public Context(Context context) {
            this.session = context.session;
            this.realm = context.realm;
            this.client = context.client;
            this.clientConfig = context.clientConfig;
            this.clientConnection = context.clientConnection;
            this.clientAuthAttributes = context.clientAuthAttributes;
            this.request = context.request;
            this.response = context.response;
            this.headers = context.headers;
            this.formParams = context.formParams;
            this.event = context.event;
            this.cors = context.cors;
            this.tokenManager = context.tokenManager;
            this.dPoP = context.dPoP;
        }

        public void setFormParams(MultivaluedHashMap<String, String> formParams) {
            this.formParams = formParams;
        }

        public void setClient(ClientModel client) {
            this.client = client;
        }

        public void setClientConfig(Object clientConfig) {
            this.clientConfig = clientConfig;
        }

        public void setClientAuthAttributes(Map<String, String> clientAuthAttributes) {
            this.clientAuthAttributes = clientAuthAttributes;
        }

    }

}
