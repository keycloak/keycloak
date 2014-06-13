package org.keycloak.models.cache;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.entities.CachedApplication;
import org.keycloak.models.cache.entities.CachedOAuthClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter extends ClientAdapter implements OAuthClientModel {
    protected OAuthClientModel updated;
    protected CachedOAuthClient cached;

    public OAuthClientAdapter(RealmModel cachedRealm, CachedOAuthClient cached, CacheKeycloakSession cacheSession, KeycloakCache cache) {
        super(cachedRealm, cached, cache, cacheSession);
        this.cached = cached;
    }

    @Override
    protected void getDelegateForUpdate() {
        if (updated == null) {
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
    }
}
