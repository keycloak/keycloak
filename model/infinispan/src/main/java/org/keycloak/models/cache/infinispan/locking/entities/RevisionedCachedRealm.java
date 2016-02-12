package org.keycloak.models.cache.infinispan.locking.entities;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.infinispan.locking.Revisioned;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RevisionedCachedRealm extends CachedRealm implements Revisioned {

    public RevisionedCachedRealm(Long revision, RealmCache cache, RealmProvider delegate, RealmModel model) {
        super(cache, delegate, model);
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
    protected void cacheClientTemplates(RealmCache cache, RealmProvider delegate, RealmModel model) {
        for (ClientTemplateModel template : model.getClientTemplates()) {
            clientTemplates.add(template.getId());
        }
    }

    @Override
    protected void cacheClients(RealmCache cache, RealmProvider delegate, RealmModel model) {
        for (ClientModel client : model.getClients()) {
            clients.put(client.getClientId(), client.getId());
        }
    }

    @Override
    protected void cacheRealmRoles(RealmCache cache, RealmModel model) {
        for (RoleModel role : model.getRoles()) {
            realmRoles.put(role.getName(), role.getId());
        }
    }
}
