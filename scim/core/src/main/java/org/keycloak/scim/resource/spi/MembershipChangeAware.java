package org.keycloak.scim.resource.spi;

import java.util.List;

/**
 * Implemented by {@link ScimResourceTypeProvider}s that need to report group membership changes triggered by a
 * PATCH request, so the caller can emit a dedicated admin event for each change in addition to the generic
 * resource-level event.
 */
public interface MembershipChangeAware {

    /**
     * Returns the membership changes recorded since the last call, then clears them.
     */
    List<MembershipChange> pollMembershipChanges();
}
