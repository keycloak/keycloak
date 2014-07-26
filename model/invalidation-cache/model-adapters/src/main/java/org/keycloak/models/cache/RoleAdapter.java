package org.keycloak.models.cache;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.entities.CachedApplicationRole;
import org.keycloak.models.cache.entities.CachedRealmRole;
import org.keycloak.models.cache.entities.CachedRole;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements RoleModel {

    protected RoleModel updated;
    protected CachedRole cached;
    protected RealmCache cache;
    protected CacheRealmProvider cacheSession;
    protected RealmModel realm;

    public RoleAdapter(CachedRole cached, RealmCache cache, CacheRealmProvider session, RealmModel realm) {
        this.cached = cached;
        this.cache = cache;
        this.cacheSession = session;
        this.realm = realm;
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerRoleInvalidation(getId());
            updated = cacheSession.getDelegate().getRoleById(getId(), realm);
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }


    @Override
    public String getName() {
        if (updated != null) return updated.getName();
        return cached.getName();
    }

    @Override
    public String getDescription() {
        if (updated != null) return updated.getDescription();
        return cached.getDescription();
    }

    @Override
    public void setDescription(String description) {
        getDelegateForUpdate();
        updated.setDescription(description);
    }

    @Override
    public String getId() {
        if (updated != null) return updated.getId();
        return cached.getId();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);
    }

    @Override
    public boolean isComposite() {
        if (updated != null) return updated.isComposite();
        return cached.isComposite();
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        getDelegateForUpdate();
        updated.addCompositeRole(role);
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        getDelegateForUpdate();
        updated.removeCompositeRole(role);
    }

    @Override
    public Set<RoleModel> getComposites() {
        if (updated != null) return updated.getComposites();
        Set<RoleModel> set = new HashSet<RoleModel>();
        for (String id : cached.getComposites()) {
            RoleModel role = realm.getRoleById(id);
            if (role == null) {
                throw new IllegalStateException("Could not find composite: " + id);
            }
            set.add(role);
        }
        return set;
    }

    @Override
    public RoleContainerModel getContainer() {
        if (cached instanceof CachedRealmRole) {
            return realm;
        } else {
            CachedApplicationRole appRole = (CachedApplicationRole)cached;
            return realm.getApplicationById(appRole.getAppId());
        }
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (this.equals(role)) return true;
        if (!isComposite()) return false;

        Set<RoleModel> visited = new HashSet<RoleModel>();
        return KeycloakModelUtils.searchFor(role, this, visited);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
