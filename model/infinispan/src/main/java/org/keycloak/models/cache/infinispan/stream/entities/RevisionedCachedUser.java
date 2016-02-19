package org.keycloak.models.cache.infinispan.stream.entities;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.entities.CachedUser;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RevisionedCachedUser extends CachedUser implements Revisioned, InRealm {
    public RevisionedCachedUser(Long revision, RealmModel realm, UserModel user) {
        super(realm, user);
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
