package org.keycloak.models.cache.infinispan.counter.entities;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.counter.Revisioned;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RevisionedCachedGroup extends CachedGroup implements Revisioned {
    public RevisionedCachedGroup(Long revision, RealmModel realm, GroupModel group) {
        super(realm, group);
        this.revision = revision;
    }

    private Long revision;

    @Override
    public Long getRevision() {
        return revision;
    }

    @Override
    public void setRevision(Long revision) {
        this.revision = revision;
    }


}
