package org.keycloak.models.workflow.events;

import org.keycloak.models.FederatedIdentityModel.FederatedIdentityCreatedEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.AbstractWorkflowEventProvider;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.provider.ProviderEvent;

public class UserFedIdentityAddedWorkflowEventProvider extends AbstractWorkflowEventProvider {

    public UserFedIdentityAddedWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        super(session, configParameter, providerId);
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.USERS;
    }

    @Override
    public boolean supports(ProviderEvent providerEvent) {
        return providerEvent instanceof FederatedIdentityCreatedEvent;
    }

    @Override
    protected String resolveResourceId(ProviderEvent providerEvent) {
        if (providerEvent instanceof FederatedIdentityCreatedEvent fie) {
            return fie.getUser().getId();
        }
        return null;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        if (!super.evaluate(context)) {
            return false;
        }
        if (super.configParameter != null) {
            // this is the case when the idp alias is passed as a parameter to the event provider - like user-federated-identity-added(myidp)
            ProviderEvent fedIdentityEvent = (ProviderEvent) context.getEvent().getEvent();
            if (fedIdentityEvent instanceof FederatedIdentityCreatedEvent fie) {
                return configParameter.equals(fie.getFederatedIdentity().getIdentityProvider());
            } else {
                return false;
            }
        } else {
            // nothing else to check
            return true;
        }
    }
}
