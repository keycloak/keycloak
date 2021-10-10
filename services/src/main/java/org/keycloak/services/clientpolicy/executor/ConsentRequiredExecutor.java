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

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ConsentRequiredExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        ClientCRUDContext clientUpdateContext = null;
        switch (context.getEvent()) {
            case REGISTERED:
                clientUpdateContext = (ClientCRUDContext)context;
                afterRegister(clientUpdateContext.getTargetClient());
                break;
            case UPDATE:
                clientUpdateContext = (ClientCRUDContext)context;
                beforeUpdate(clientUpdateContext.getTargetClient(), clientUpdateContext.getProposedClientRepresentation());
                break;
            default:
                return;
        }
    }

    @Override
    public String getProviderId() {
        return ConsentRequiredExecutorFactory.PROVIDER_ID;
    }

    private void afterRegister(ClientModel clientModel) {
        clientModel.setConsentRequired(true);
    }

    public void beforeUpdate(ClientModel clientToBeUpdated, ClientRepresentation proposedClient) throws ClientPolicyException {
        if (proposedClient.isConsentRequired() == null) {
            return;
        }
        if (clientToBeUpdated == null) {
            return;
        }

        boolean isConsentRequired = clientToBeUpdated.isConsentRequired();
        boolean newConsentRequired = proposedClient.isConsentRequired();

        if (isConsentRequired && !newConsentRequired) {
            throw new ClientPolicyException(Errors.INVALID_REGISTRATION, "Not permitted to update consentRequired to false");
        }
    }

}
