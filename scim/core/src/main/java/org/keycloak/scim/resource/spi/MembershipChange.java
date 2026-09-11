package org.keycloak.scim.resource.spi;

import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;

/**
 * Represents a group membership change (a user joining or leaving a group) that resulted from processing a
 * SCIM PATCH request. Reported by a {@link MembershipChangeAware} resource type provider so the caller can
 * emit a dedicated admin event, consistently with the equivalent Admin REST API operation.
 */
public record MembershipChange(GroupModel group, UserModel user, boolean added) {
}
