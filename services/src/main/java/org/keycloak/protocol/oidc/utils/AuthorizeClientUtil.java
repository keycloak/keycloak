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

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
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
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizeClientUtil {

    private static final Logger logger = Logger.getLogger(AuthorizeClientUtil.class);

    public static ClientAuthResult authorizeClient(KeycloakSession session, EventBuilder event) {
        AuthenticationProcessor processor = getAuthenticationProcessor(session, event);

        Response response = processor.authenticateClient();
        if (response != null) {
            throw new WebApplicationException(response);
        }

        ClientModel client = processor.getClient();
        if (client == null) {
            throw new ErrorResponseException(Errors.INVALID_CLIENT, "Client authentication ended, but client is null", Response.Status.BAD_REQUEST);
        }

        String protocol = client.getProtocol();
        if (protocol == null) {
            logger.warnf("Client '%s' doesn't have protocol set. Fallback to openid-connect. Please fix client configuration", client.getClientId());
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }

        if (!protocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException(Errors.INVALID_CLIENT, "Wrong client protocol.", Response.Status.BAD_REQUEST);
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
                .setRequest(session.getContext().getContextObject(HttpRequest.class));

        return processor;
    }

    public static ClientAuthenticatorFactory findClientAuthenticatorForOIDCAuthMethod(KeycloakSession session, String oidcAuthMethod) {
        List<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactories(ClientAuthenticator.class);
        for (ProviderFactory factory : providerFactories) {
            ClientAuthenticatorFactory clientAuthFactory = (ClientAuthenticatorFactory) factory;
            if (clientAuthFactory.getProtocolAuthenticatorMethods(OIDCLoginProtocol.LOGIN_PROTOCOL).contains(oidcAuthMethod)) {
                return clientAuthFactory;
            }
        }

        return null;
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
