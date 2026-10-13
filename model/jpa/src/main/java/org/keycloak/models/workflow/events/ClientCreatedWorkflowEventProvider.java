package org.keycloak.models.workflow.events;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.provider.ProviderEvent;

public class ClientCreatedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public ClientCreatedWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.CLIENTS;
    }

    @Override
    public boolean supports(ProviderEvent providerEvent) {
        return providerEvent instanceof ClientModel.ClientCreationEvent;
    }

    @Override
    protected String resolveResourceId(ProviderEvent providerEvent) {
        if (providerEvent instanceof ClientModel.ClientCreationEvent cce) {
            return cce.getCreatedClient().getId();
        }
        return null;
    }
}
