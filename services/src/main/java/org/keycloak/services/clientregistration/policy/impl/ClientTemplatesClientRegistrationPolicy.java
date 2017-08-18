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

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientTemplatesClientRegistrationPolicy implements ClientRegistrationPolicy {

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ClientTemplatesClientRegistrationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        String clientTemplate = context.getClient().getClientTemplate();
        if (!isTemplateAllowed(clientTemplate)) {
            throw new ClientRegistrationPolicyException("Not permitted to use specified clientTemplate");
        }
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {

    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        String newTemplate = context.getClient().getClientTemplate();

        // Check if template was already set before. Then we allow update
        ClientTemplateModel currentTemplate = clientModel.getClientTemplate();
        if (currentTemplate == null || !currentTemplate.getName().equals(newTemplate)) {
            if (!isTemplateAllowed(newTemplate)) {
                throw new ClientRegistrationPolicyException("Not permitted to use specified clientTemplate");
            }
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

    private boolean isTemplateAllowed(String template) {
        if (template == null) {
            return true;
        } else {
            List<String> allowedTemplates = componentModel.getConfig().getList(ClientTemplatesClientRegistrationPolicyFactory.ALLOWED_CLIENT_TEMPLATES);
            return allowedTemplates.contains(template);
        }
    }
}
