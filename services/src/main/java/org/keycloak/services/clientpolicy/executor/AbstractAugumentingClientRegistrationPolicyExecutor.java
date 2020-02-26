/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientUpdateContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public abstract class AbstractAugumentingClientRegistrationPolicyExecutor implements ClientPolicyExecutorProvider {

    protected static final Logger logger = Logger.getLogger(AbstractAugumentingClientRegistrationPolicyExecutor.class);

    protected static final String IS_AUGMENT = "is-augment";

    protected final KeycloakSession session;
    protected final ComponentModel componentModel;


    public AbstractAugumentingClientRegistrationPolicyExecutor(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
        case UPDATE:
            ClientUpdateContext clientUpdateContext = (ClientUpdateContext)context;
            augment(clientUpdateContext.getProposedClientRepresentation());
            validate(clientUpdateContext.getProposedClientRepresentation());
            break;
        default:
            return;
        }
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

    /**
     * overrides the client settings specified by the argument.
     *
     * @param rep - the client settings
     */
    protected abstract void augment(ClientRepresentation rep);

    /**
     * validate the client settings specified by the argument to check
     * whether they follows what the executor expects.
     *
     * @param rep - the client settings
     */
    protected abstract void validate(ClientRepresentation rep) throws ClientPolicyException;

}
