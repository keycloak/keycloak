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

import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ConsentRequiredExecutor implements ClientPolicyExecutorProvider<ConsentRequiredExecutor.Configuration> {

    private ConsentRequiredExecutor.Configuration configuration;

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        ClientCRUDContext clientUpdateContext = null;
        switch (context.getEvent()) {
            case REGISTER:
                clientUpdateContext = (ClientCRUDContext)context;
                autoConfigure(clientUpdateContext.getProposedClientRepresentation());
                validate(clientUpdateContext.getProposedClientRepresentation());
                break;
            case UPDATE:
                clientUpdateContext = (ClientCRUDContext)context;
                autoConfigure(clientUpdateContext.getProposedClientRepresentation());
                beforeUpdate(clientUpdateContext.getTargetClient(), clientUpdateContext.getProposedClientRepresentation());
                break;
            default:
                return;
        }
    }

    @Override
    public void setupConfiguration(ConsentRequiredExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<ConsentRequiredExecutor.Configuration> getExecutorConfigurationClass() {
        return ConsentRequiredExecutor.Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("auto-configure")
        protected Boolean autoConfigure;

        public Boolean isAutoConfigure() {
            return autoConfigure;
        }

        public void setAutoConfigure(Boolean autoConfigure) {
            this.autoConfigure = autoConfigure;
        }
    }

    @Override
    public String getProviderId() {
        return ConsentRequiredExecutorFactory.PROVIDER_ID;
    }

    private void autoConfigure(ClientRepresentation proposedClient) throws ClientPolicyException {
        if (configuration.isAutoConfigure()) {
            proposedClient.setConsentRequired(true);
        }
    }

    private void validate(ClientRepresentation proposedClient) throws ClientPolicyException {
        if (proposedClient.isConsentRequired() == null || !proposedClient.isConsentRequired()) {
            throw new ClientPolicyException(Errors.INVALID_REGISTRATION, "Client is required to enable consentRequired");
        }
    }

    public void beforeUpdate(ClientModel clientToBeUpdated, ClientRepresentation proposedClient) throws ClientPolicyException {
        if (clientToBeUpdated == null) {
            return;
        }
        // We are not updating consentRequired in the representation, but it is already set to true on the client
        if (proposedClient.isConsentRequired() == null && clientToBeUpdated.isConsentRequired()) {
            return;
        }

        validate(proposedClient);
    }

}
