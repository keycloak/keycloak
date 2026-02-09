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
 *
 */

package org.keycloak.services.clientpolicy.executor;

import org.keycloak.events.Errors;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Check that switch "fullScopeAllowed" is not enabled for the clients
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullScopeDisabledExecutor implements ClientPolicyExecutorProvider<FullScopeDisabledExecutor.Configuration> {

    private FullScopeDisabledExecutor.Configuration configuration;

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
            case UPDATE:
                ClientCRUDContext clientUpdateContext = (ClientCRUDContext)context;
                autoConfigure(clientUpdateContext.getProposedClientRepresentation());
                validate(clientUpdateContext.getProposedClientRepresentation());
                break;
            default:
                return;
        }
    }

    @Override
    public void setupConfiguration(FullScopeDisabledExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<FullScopeDisabledExecutor.Configuration> getExecutorConfigurationClass() {
        return FullScopeDisabledExecutor.Configuration.class;
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
        return FullScopeDisabledExecutorFactory.PROVIDER_ID;
    }

    private void autoConfigure(ClientRepresentation rep) {
        if (configuration.isAutoConfigure()) {
            rep.setFullScopeAllowed(false);
        }
    }

    private void validate(ClientRepresentation proposedClient) throws ClientPolicyException {
        if (proposedClient.isFullScopeAllowed() != null && proposedClient.isFullScopeAllowed()) {
            throw new ClientPolicyException(Errors.INVALID_REGISTRATION, "Not permitted to enable fullScopeAllowed");
        }
    }
}
