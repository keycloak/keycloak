package org.keycloak.models.cache.infinispan.authorization.entities;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PermissionTicketListQuery extends AbstractRevisioned implements PermissionTicketQuery {

    private final Set<String> permissions;
    private final String serverId;

    public PermissionTicketListQuery(Long revision, String id, String permissionId, String serverId) {
        super(revision, id);
        this.serverId = serverId;
        permissions = new HashSet<>();
        permissions.add(permissionId);
    }
    public PermissionTicketListQuery(Long revision, String id, Set<String> permissions, String serverId) {
        super(revision, id);
        this.serverId = serverId;
        this.permissions = permissions;
    }

    @Override
    public String getResourceServerId() {
        return serverId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public boolean isInvalid(Set<String> invalidations) {
        return invalidations.contains(getId()) || invalidations.contains(getResourceServerId());
    }
}