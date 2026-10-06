/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.workflow;

import java.util.List;
import java.util.Set;

import org.keycloak.authentication.requiredactions.util.RequiredActionsValidator;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.RedirectUtils;

public class InviteUserStepProviderFactory implements WorkflowStepProviderFactory<InviteUserStepProvider> {

    public static final String ID = "invite-user";

    @Override
    public InviteUserStepProvider create(KeycloakSession session, ComponentModel model) {
        return new InviteUserStepProvider(session, model);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<ResourceType> getSupportedResourceTypes() {
        return Set.of(ResourceType.USERS);
    }

    @Override
    public String getHelpText() {
        return "Sends an invitation email with an action link to the user";
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        String clientId = model.get(InviteUserStepProvider.CONFIG_CLIENT_ID);
        String redirectUri = model.get(InviteUserStepProvider.CONFIG_REDIRECT_URI);

        if (redirectUri != null && clientId == null) {
            throw new ComponentValidationException("'redirect-uri' requires 'client-id' to be set");
        }
        if (clientId != null) {
            ClientModel client = realm.getClientByClientId(clientId);
            if (client == null) {
                throw new ComponentValidationException("Client '" + clientId + "' does not exist");
            }
            if (redirectUri != null && RedirectUtils.verifyRedirectUri(session, redirectUri, client) == null) {
                throw new ComponentValidationException("'redirect-uri' is not a valid redirect URI for client '" + clientId + "'");
            }
        }

        List<String> actions = model.getConfig().get(InviteUserStepProvider.CONFIG_ACTIONS);
        if (actions != null && !RequiredActionsValidator.validRequiredActions(session, actions)) {
            throw new ComponentValidationException("Invalid required action configured for 'actions'");
        }

        // The step runs on a workflow executor thread with no active HTTP request, so the
        // invitation base URI must come from static configuration (see resolveBaseUri).
        // Reject the step up-front instead of silently dropping invites at runtime.
        if (InviteUserStepProvider.resolveBaseUri(realm) == null) {
            throw new ComponentValidationException(InviteUserStepProvider.HOSTNAME_NOT_CONFIGURED_MESSAGE);
        }
    }
}
