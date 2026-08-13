package org.keycloak.models.workflow;

import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;

/**
 * Defines a provider interface for handling and converting events into workflow events.
 * </p>
 * Implementations of this interface are responsible for determining whether specific
 * events (user events, admin events, or provider events) are supported, converting them
 * to workflow events, and evaluating workflow conditions based on these events.
 */
public interface WorkflowEventProvider extends Provider {

    /**
     * Returns the {@link ResourceType} that this event provider is capable of handling.
     *
     * @return the supported ResourceType for this event provider implementation
     */
    ResourceType getSupportedResourceType();

    /**
     * Creates a {@link WorkflowEvent} from a user {@link Event}.
     *
     * @param event the user event to convert
     * @return a WorkflowEvent representing the given event, or {@code null} if the event is not supported
     */
    WorkflowEvent create(Event event);

    /**
     * Creates a {@link WorkflowEvent} from an {@link AdminEvent}.
     *
     * @param adminEvent the admin event to convert
     * @return a WorkflowEvent representing the given admin event, or {@code null} if the event is not supported
     */
    WorkflowEvent create(AdminEvent adminEvent);

    /**
     * Creates a {@link WorkflowEvent} from a {@link ProviderEvent}.
     *
     * @param providerEvent the provider event to convert
     * @return a WorkflowEvent representing the given provider event, or {@code null} if the event is not supported
     */
    WorkflowEvent create(ProviderEvent providerEvent);

    /**
     * Determines whether this provider supports the given user {@link Event}.
     *
     * @param event the user event to check
     * @return {@code true} if the event is supported, {@code false} otherwise
     */
    boolean supports(Event event);

    /**
     * Determines whether this provider supports the given {@link AdminEvent}.
     *
     * @param adminEvent the admin event to check
     * @return {@code true} if the event is supported, {@code false} otherwise
     */
    boolean supports(AdminEvent adminEvent);

    /**
     * Determines whether this provider supports the given {@link ProviderEvent}.
     *
     * @param providerEvent the provider event to check
     * @return {@code true} if the event is supported, {@code false} otherwise
     */
    boolean supports(ProviderEvent providerEvent);

    /**
     * Evaluates whether the event in the workflow execution context matches this provider's criteria.
     * </p>
     * Implementations should inspect the workflow event in the provided {@code context} and return
     * {@code true} when the event matches the provider's specific conditions.
     *
     * @param context the execution context for the workflow evaluation
     * @return {@code true} if the event matches the criteria, {@code false} otherwise
     */
    boolean evaluate(WorkflowExecutionContext context);

    @Override
    default void close() {
        // no-op
    }

}
