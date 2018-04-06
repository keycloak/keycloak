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

package org.keycloak.services.clientregistration.policy.impl;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ScopeClientRegistrationPolicy implements ClientRegistrationPolicy {

    private static final Logger logger = Logger.getLogger(ScopeClientRegistrationPolicy.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ScopeClientRegistrationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }


    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {

    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {
        clientModel.setFullScopeAllowed(false);
    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        if (context.getClient().isFullScopeAllowed() == null) {
            return;
        }
        if (clientModel == null) {
            return;
        }

        boolean isAllowed = clientModel.isFullScopeAllowed();
        boolean newAllowed = context.getClient().isFullScopeAllowed();

        if (!isAllowed && newAllowed) {
            throw new ClientRegistrationPolicyException("Not permitted to enable fullScopeAllowed");
        }
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {

    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {

    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {

    }
}
