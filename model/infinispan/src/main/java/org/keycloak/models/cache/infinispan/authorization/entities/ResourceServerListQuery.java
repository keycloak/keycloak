package org.keycloak.models.cache.infinispan.authorization.entities;

import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceServerListQuery extends AbstractRevisioned {
    private final Set<String> servers;

    public ResourceServerListQuery(Long revision, String id, String serverId) {
        super(revision, id);
        servers = new HashSet<>();
        servers.add(serverId);
    }
    public ResourceServerListQuery(Long revision, String id, Set<String> servers) {
        super(revision, id);
        this.servers = servers;
    }

    public Set<String> getResourceServers() {
        return servers;
    }
}