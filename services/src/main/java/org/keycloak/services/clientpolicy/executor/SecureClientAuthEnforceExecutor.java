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

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureClientAuthEnforceExecutor implements ClientPolicyExecutorProvider<SecureClientAuthEnforceExecutor.Configuration> {

    private final KeycloakSession session;
    private Configuration configuration;

    public SecureClientAuthEnforceExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(SecureClientAuthEnforceExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration extends ClientPolicyExecutorConfiguration {
        @JsonProperty("client-authns")
        protected List<String> clientAuthns;
        @JsonProperty("client-authns-augment")
        protected String clientAuthnsAugment;
        @JsonProperty("is-augment")
        protected Boolean augment;

        public List<String> getClientAuthns() {
            return clientAuthns;
        }

        public void setClientAuthns(List<String> clientAuthns) {
            this.clientAuthns = clientAuthns;
        }

        public String getClientAuthnsAugment() {
            return clientAuthnsAugment;
        }

        public void setClientAuthnsAugment(String clientAuthnsAugment) {
            this.clientAuthnsAugment = clientAuthnsAugment;
        }

        public Boolean isAugment() {
            return augment;
        }

        public void setAugment(Boolean augment) {
            this.augment = augment;
        }
    }

    @Override
    public String getProviderId() {
        return SecureClientAuthEnforceExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
        case UPDATE:
            ClientCRUDContext clientUpdateContext = (ClientCRUDContext)context;
            augment(clientUpdateContext.getProposedClientRepresentation());
            validate(clientUpdateContext.getProposedClientRepresentation());
            break;
        default:
            return;
        }
    }

    private void augment(ClientRepresentation rep) {
        if (configuration.isAugment())
            rep.setClientAuthenticatorType(enforcedClientAuthenticatorType());
    }

    private void validate(ClientRepresentation rep) throws ClientPolicyException {
        verifyClientAuthenticationMethod(rep.getClientAuthenticatorType());
    }

    private String enforcedClientAuthenticatorType() {
        return configuration.getClientAuthnsAugment();
    }

    private void verifyClientAuthenticationMethod(String clientAuthenticatorType) throws ClientPolicyException {
        List<String> acceptableClientAuthn = configuration.getClientAuthns();
        if (acceptableClientAuthn != null && acceptableClientAuthn.stream().anyMatch(i->i.equals(clientAuthenticatorType))) return;
        throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: token_endpoint_auth_method");
    }


}
