package org.keycloak.models.workflow;

import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;

public interface WorkflowEventProvider extends Provider {

    ResourceType getSupportedResourceType();

    WorkflowEvent create(Event event);

    WorkflowEvent create(AdminEvent adminEvent);

    WorkflowEvent create(ProviderEvent providerEvent);

    boolean supports(Event event);

    boolean supports(AdminEvent adminEvent);

    boolean supports(ProviderEvent providerEvent);

    boolean evaluate(WorkflowExecutionContext context);

    @Override
    default void close() {
        // no-op
    }

}
