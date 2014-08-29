package org.keycloak.models.cache;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.entities.CachedOAuthClient;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter extends ClientAdapter implements OAuthClientModel {
    protected OAuthClientModel updated;
    protected CachedOAuthClient cached;

    public OAuthClientAdapter(RealmModel cachedRealm, CachedOAuthClient cached, CacheRealmProvider cacheSession, RealmCache cache) {
        super(cachedRealm, cached, cache, cacheSession);
        this.cached = cached;
    }

    @Override
    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerOAuthClientInvalidation(getId());
            updatedClient = updated = cacheSession.getDelegate().getOAuthClientById(getId(), cachedRealm);
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    @Override
    public String getClientId() {
        if (updated != null) return updated.getClientId();
        return cached.getName();
    }

    @Override
    public void setClientId(String id) {
        getDelegateForUpdate();
        updated.setClientId(id);
        cacheSession.registerRealmInvalidation(cachedRealm.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OAuthClientModel)) return false;

        OAuthClientModel that = (OAuthClientModel) o;

        return that.getId().equals(this.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
