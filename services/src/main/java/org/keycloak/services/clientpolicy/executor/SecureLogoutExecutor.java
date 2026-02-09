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

import java.util.Optional;

import jakarta.ws.rs.HttpMethod;

import org.keycloak.events.Errors;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecureLogoutExecutor implements ClientPolicyExecutorProvider<SecureLogoutExecutor.Configuration> {

    private final KeycloakSession session;
    private Configuration configuration;

    public SecureLogoutExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty(SecureLogoutExecutorFactory.ALLOW_FRONT_CHANNEL_LOGOUT)
        protected Boolean allowFrontChannelLogout = Boolean.FALSE;

        public Boolean isAllowFrontChannelLogout() {
            return allowFrontChannelLogout;
        }

        public void setAllowFrontChannelLogout(Boolean allowFrontChannelLogout) {
            this.allowFrontChannelLogout = allowFrontChannelLogout;
        }
    }

    @Override
    public String getProviderId() {
        return SecureLogoutExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
            case UPDATE:
                ClientCRUDContext updateContext = (ClientCRUDContext)context;
                ClientRepresentation client = updateContext.getProposedClientRepresentation();
                OIDCAdvancedConfigWrapper clientWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);

                if (!configuration.isAllowFrontChannelLogout()
                        && (Optional.ofNullable(client.isFrontchannelLogout()).orElse(false) || StringUtil.isNotBlank(clientWrapper.getFrontChannelLogoutUrl()))) {
                    throwFrontChannelLogoutNotAllowed();
                }

                return;
            case LOGOUT_REQUEST:
                HttpRequest request = session.getContext().getHttpRequest();

                if (HttpMethod.GET.equalsIgnoreCase(request.getHttpMethod()) && !configuration.isAllowFrontChannelLogout()) {
                    throwFrontChannelLogoutNotAllowed();
                }

                return;
            default:
                return;
        }
    }

    private void throwFrontChannelLogoutNotAllowed() throws ClientPolicyException {
        throw new ClientPolicyException(Errors.INVALID_REGISTRATION, "Front-channel logout is not allowed for this client");
    }
}
