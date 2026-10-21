package org.keycloak.models.workflow;

import java.util.List;

/**
 * A {@link ResourceTypeSelector} implementation provides the mechanism to select existing realm resources
 * that matches the conditions for a specific {@link Workflow}.
 *
 * @see WorkflowProvider#getResourceTypeSelector(ResourceType)
 */
public interface ResourceTypeSelector {

    /**
     * Finds all resources that are eligible for the first action of a workflow.
     *
     * @return A list of eligible resource IDs.
     */
    List<String> getResourceIds(Workflow workflow);

    Object resolveResource(String resourceId);
}
