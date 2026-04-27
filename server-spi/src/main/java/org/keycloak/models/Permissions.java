package org.keycloak.models;

/**
 * A permissions evaluator that can be used to check if the current user has permissions to perform an action on realm resources.
 */
public interface Permissions {

    /**
     * Returns {@code true} if the current user has permissions to perform an action on realm resources with the given {@code resourceType} and with the given {@code scope}.
     *
     * @param resourceType the realm resource type
     * @param scope the scope
     * @return {@code true} if the current user has permissions to perform an action on a realm resource type with the given scope, {@code false} otherwise
     */
    boolean hasPermission(String resourceType, String scope);

    /**
     * Returns {@code true} if the current user has permissions to perform an action on a realm resource type with the given scope
     *
     * @param resourceType the realm resource type
     * @param scope the scope
     * @return {@code true} if the current user has permissions to perform an action on a realm resource type with the given scope, {@code false} otherwise
     */
    boolean hasPermission(Model model, String resourceType, String scope);
}
