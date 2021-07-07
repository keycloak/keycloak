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
 */

package org.keycloak.services.clientpolicy.executor;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureClientAuthenticatorExecutor implements ClientPolicyExecutorProvider<SecureClientAuthenticatorExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureClientAuthenticatorExecutor.class);

    private final KeycloakSession session;
    private Configuration configuration;

    public SecureClientAuthenticatorExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(SecureClientAuthenticatorExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("allowed-client-authenticators")
        protected List<String> allowedClientAuthenticators;
        @JsonProperty("default-client-authenticator")
        protected String defaultClientAuthenticator;

        public List<String> getAllowedClientAuthenticators() {
            return allowedClientAuthenticators;
        }

        public void setAllowedClientAuthenticators(List<String> allowedClientAuthenticators) {
            this.allowedClientAuthenticators = allowedClientAuthenticators;
        }

        public String getDefaultClientAuthenticator() {
            return defaultClientAuthenticator;
        }

        public void setDefaultClientAuthenticator(String defaultClientAuthenticator) {
            this.defaultClientAuthenticator = defaultClientAuthenticator;
        }
    }

    @Override
    public String getProviderId() {
        return SecureClientAuthenticatorExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
        case UPDATE:
            ClientCRUDContext clientUpdateContext = (ClientCRUDContext)context;
            autoConfigure(clientUpdateContext.getProposedClientRepresentation());
            validateDuringClientCRUD(clientUpdateContext.getProposedClientRepresentation());
            break;
        case TOKEN_REQUEST:
        case TOKEN_REFRESH:
        case TOKEN_REVOKE:
        case TOKEN_INTROSPECT:
        case LOGOUT_REQUEST:
            validateDuringClientRequest();
        default:
            return;
        }
    }

    private void autoConfigure(ClientRepresentation rep) {
        String defaultClientAuthenticator = configuration.getDefaultClientAuthenticator();
        if (defaultClientAuthenticator != null) {
            if (rep.getClientAuthenticatorType() == null) {
                logger.tracef("Set default client authenticator %s on client %s", defaultClientAuthenticator, rep.getClientId());
                rep.setClientAuthenticatorType(defaultClientAuthenticator);
            } else {
                logger.tracef("Skip setting default client authenticator on client %s. Client authenticator already set to %s", rep.getClientId(), rep.getClientAuthenticatorType());
            }
        }
    }

    private void validateDuringClientCRUD(ClientRepresentation rep) throws ClientPolicyException {
        // Allow public clients (There is separate executor to check access type)
        if (rep.isPublicClient() != null && rep.isPublicClient()) return;

        String clientAuthenticatorType = rep.getClientAuthenticatorType();
        if (isValidClientAuthenticator(clientAuthenticatorType)) return;
        throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: token_endpoint_auth_method");
    }

    // Validate client authenticator also during client request
    private void validateDuringClientRequest() throws ClientPolicyException {
        ClientModel client = session.getContext().getClient();
        // Allow public clients (There is separate executor to check access type)
        if (client.isPublicClient()) return;

        if (isValidClientAuthenticator(client.getClientAuthenticatorType())) return;
        logger.warnf("Client authentication method not allowed for client: %s", client.getClientId());
        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Configured client authentication method not allowed for client");
    }

    private boolean isValidClientAuthenticator(String clientAuthenticatorType) {
        List<String> acceptableClientAuthn = configuration.getAllowedClientAuthenticators();
        return (acceptableClientAuthn != null && acceptableClientAuthn.stream().anyMatch(i->i.equals(clientAuthenticatorType)));
    }


}
