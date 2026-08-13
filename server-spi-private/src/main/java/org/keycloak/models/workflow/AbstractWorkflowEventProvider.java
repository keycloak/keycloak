package org.keycloak.models.workflow;

import java.util.Objects;

import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderEvent;

/**
 * Utility class to simplify implementation of WorkflowEventProvider interface, providing default implementations for most methods.
 * </p>
 * Subclasses need to implement only the {@link #getSupportedResourceType()} method and the relevant {@code supports} methods. If
 * the provider supports {@link ProviderEvent}s, they also need to implement the {@link  #resolveResourceId} method because there is no
 * common way to resolve resource IDs from provider events, so that logic is provider-specific.
 */
public abstract class AbstractWorkflowEventProvider implements WorkflowEventProvider {

    protected final String providerId;
    protected final String configParameter;
    protected final KeycloakSession session;

    public AbstractWorkflowEventProvider(final KeycloakSession session, final String configParameter, final String providerId) {
        this.providerId = providerId;
        this.configParameter = configParameter;
        this.session = session;
    }

    @Override
    public WorkflowEvent create(Event event) {
        if (supports(event)) {
            ResourceType resourceType = getSupportedResourceType();
            String resourceIdFromEvent = resourceType.resolveResourceId(session, event);
            return resourceIdFromEvent != null ? new WorkflowEvent(resourceType, resourceIdFromEvent, event, providerId) : null;
        }
        return null;
    }

    @Override
    public WorkflowEvent create(AdminEvent adminEvent) {
        if (supports(adminEvent)) {
            return new WorkflowEvent(getSupportedResourceType(), adminEvent.getResourceId(), adminEvent, providerId);
        }
        return null;
    }

    @Override
    public WorkflowEvent create(ProviderEvent providerEvent) {
        if (supports(providerEvent)) {
            String resourceId = resolveResourceId(providerEvent);
            return resourceId != null ? new WorkflowEvent(getSupportedResourceType(), resourceId, providerEvent, providerId) : null;
        }
        return null;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        WorkflowEvent event = context.getEvent();
        return event != null && Objects.equals(this.providerId, event.getEventProviderId());
    }

    @Override
    public boolean supports(Event event) {
        return false;
    }

    @Override
    public boolean supports(AdminEvent adminEvent) {
        return false;
    }

    @Override
    public boolean supports(ProviderEvent providerEvent) {
        return false;
    }

    /**
     * Resolves the resource ID from the given {@link ProviderEvent}.
     *
     * @param providerEvent the provider event
     * @return the resolved resource ID, or {@code null} if it cannot be resolved
     */
    protected String resolveResourceId(ProviderEvent providerEvent) {
        return null;
    }
}
