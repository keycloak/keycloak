package org.keycloak.models.cache.infinispan.entities;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmListQuery extends AbstractRevisioned implements RealmQuery {
    private final Set<String> realms;

    public RealmListQuery(Long revision, String id, String realm) {
        super(revision, id);
        realms = new HashSet<>();
        realms.add(realm);
    }
    public RealmListQuery(Long revision, String id, Set<String> realms) {
        super(revision, id);
        this.realms = realms;
    }

    @Override
    public Set<String> getRealms() {
        return realms;
    }
}