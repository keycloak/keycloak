/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.utils;

import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.cors.Cors;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizeClientUtil {

    private static final Logger logger = Logger.getLogger(AuthorizeClientUtil.class);

    public static ClientAuthResult authorizeClient(KeycloakSession session, EventBuilder event, Cors cors) {
        AuthenticationProcessor processor = getAuthenticationProcessor(session, event);

        Response response = processor.authenticateClient();
        if (response != null) {
            if (cors != null) {
                cors.allowAllOrigins();
                cors.add();
            }
            throw new WebApplicationException(response);
        }

        ClientModel client = processor.getClient();
        if (client == null) {
            throwErrorResponseException(Errors.INVALID_CLIENT, "Client authentication ended, but client is null", Response.Status.BAD_REQUEST, cors);
        }

        if(!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throwErrorResponseException(Errors.INVALID_CLIENT, "Invalid client or Invalid client credentials", Response.Status.UNAUTHORIZED, cors);
        }

        if (cors != null) {
            cors.allowedOrigins(session, client);
        }

        String protocol = client.getProtocol();
        if (protocol == null) {
            logger.warnf("Client '%s' doesn't have protocol set. Fallback to openid-connect. Please fix client configuration", client.getClientId());
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }

        if (!protocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            event.error(Errors.INVALID_CLIENT);
            throwErrorResponseException(Errors.INVALID_CLIENT, "Wrong client protocol.", Response.Status.BAD_REQUEST, cors);
        }

        session.getContext().setClient(client);

        return new ClientAuthResult(client, processor.getClientAuthAttributes());
    }

    public static AuthenticationProcessor getAuthenticationProcessor(KeycloakSession session, EventBuilder event) {
        RealmModel realm = session.getContext().getRealm();

        AuthenticationFlowModel clientAuthFlow = realm.getClientAuthenticationFlow();
        String flowId = clientAuthFlow.getId();

        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setFlowId(flowId)
                .setConnection(session.getContext().getConnection())
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(session.getContext().getHttpRequest());

        return processor;
    }

    public static ClientAuthenticatorFactory findClientAuthenticatorForOIDCAuthMethod(KeycloakSession session, String oidcAuthMethod) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(ClientAuthenticator.class)
                .map(ClientAuthenticatorFactory.class::cast)
                .filter(caf -> caf.getProtocolAuthenticatorMethods(OIDCLoginProtocol.LOGIN_PROTOCOL).contains(oidcAuthMethod))
                .findFirst()
                .orElse(null);
    }

    private static void throwErrorResponseException(String error, String errorDescription, Response.Status status, Cors cors) {
        if (cors == null) {
            throw new ErrorResponseException(error, errorDescription, status);
        } else {
            cors.allowAllOrigins();
            throw new CorsErrorResponseException(cors, error, errorDescription, status);
        }
    }

    public static class ClientAuthResult {

        private final ClientModel client;
        private final Map<String, String> clientAuthAttributes;

        private ClientAuthResult(ClientModel client, Map<String, String> clientAuthAttributes) {
            this.client = client;
            this.clientAuthAttributes = clientAuthAttributes;
        }

        public ClientModel getClient() {
            return client;
        }

        public Map<String, String> getClientAuthAttributes() {
            return clientAuthAttributes;
        }
    }

}
