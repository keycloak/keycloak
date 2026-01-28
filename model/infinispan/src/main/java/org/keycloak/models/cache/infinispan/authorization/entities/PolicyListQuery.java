package org.keycloak.models.cache.infinispan.authorization.entities;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyListQuery extends AbstractRevisioned implements PolicyQuery {
    private final Set<String> policies;
    private final String serverId;

    public PolicyListQuery(Long revision, String id, String policyId, String serverId) {
        super(revision, id);
        this.serverId = serverId;
        policies = new HashSet<>();
        policies.add(policyId);
    }
    public PolicyListQuery(Long revision, String id, Set<String> policies, String serverId) {
        super(revision, id);
        this.serverId = serverId;
        this.policies = policies;
    }

    @Override
    public String getResourceServerId() {
        return serverId;
    }

    public Set<String> getPolicies() {
        return policies;
    }

    @Override
    public boolean isInvalid(Set<String> invalidations) {
        return invalidations.contains(getId()) || invalidations.contains(getResourceServerId());
    }
}