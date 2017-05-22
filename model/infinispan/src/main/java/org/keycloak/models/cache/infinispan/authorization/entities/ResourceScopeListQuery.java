package org.keycloak.models.cache.infinispan.authorization.entities;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceScopeListQuery extends ResourceListQuery implements InScope {

    private final String scopeId;

    public ResourceScopeListQuery(Long revision, String id, String scopeId, Set<String> resources, String serverId) {
        super(revision, id, resources, serverId);
        this.scopeId = scopeId;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }
}