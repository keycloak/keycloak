package org.keycloak.models.cache.infinispan.authorization.entities;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeListQuery extends AbstractRevisioned implements InResourceServer {
    private final Set<String> scopes;
    private final String serverId;

    public ScopeListQuery(Long revision, String id, String scopeId, String serverId) {
        super(revision, id);
        this.serverId = serverId;
        scopes = new HashSet<>();
        scopes.add(scopeId);
    }
    public ScopeListQuery(Long revision, String id, Set<String> scopes, String serverId) {
        super(revision, id);
        this.serverId = serverId;
        this.scopes = scopes;
    }

    @Override
    public String getResourceServerId() {
        return serverId;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}