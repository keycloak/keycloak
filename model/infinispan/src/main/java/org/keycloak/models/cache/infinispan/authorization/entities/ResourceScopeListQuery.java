package org.keycloak.models.cache.infinispan.authorization.entities;

import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
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

    @Override
    public boolean isInvalid(Set<String> invalidations) {
        return super.isInvalid(invalidations) || invalidations.contains(getScopeId());
    }
}