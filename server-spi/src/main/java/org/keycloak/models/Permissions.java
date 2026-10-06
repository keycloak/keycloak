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

    /**
     * Returns {@code true} if the given user has any admin role assigned, either directly, via group membership, or via composite roles.
     *
     * @param user the user to check
     * @return {@code true} if the user has any admin role, {@code false} otherwise
     */
    boolean isAdminUser(UserModel user);

    /**
     * Returns {@code true} if the given group has any admin role assigned, either directly, via parent groups, or via composite roles.
     *
     * @param group the group to check
     * @return {@code true} if the group has any admin role, {@code false} otherwise
     */
    boolean isAdminGroup(GroupModel group);
}
