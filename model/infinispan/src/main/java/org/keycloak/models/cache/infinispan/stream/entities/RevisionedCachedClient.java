package org.keycloak.models.cache.infinispan.stream.entities;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedClient;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RevisionedCachedClient extends CachedClient implements Revisioned, InRealm {

    public RevisionedCachedClient(Long revision, RealmCache cache, RealmProvider delegate, RealmModel realm, ClientModel model) {
        super(cache, delegate, realm, model);
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

    @Override
    protected void cacheRoles(RealmCache cache, RealmModel realm, ClientModel model) {
        for (RoleModel role : model.getRoles()) {
            roles.put(role.getName(), role.getId());
        }
    }
}
