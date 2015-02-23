package org.keycloak.models.cache;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.entities.CachedApplication;
import org.keycloak.models.cache.entities.CachedApplicationRole;
import org.keycloak.models.cache.entities.CachedOAuthClient;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRealmRole;
import org.keycloak.models.cache.entities.CachedRole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultCacheRealmProvider implements CacheRealmProvider {
    protected RealmCache cache;
    protected KeycloakSession session;
    protected RealmProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Set<String> realmInvalidations = new HashSet<String>();
    protected Set<String> appInvalidations = new HashSet<String>();
    protected Set<String> roleInvalidations = new HashSet<String>();
    protected Set<String> clientInvalidations = new HashSet<String>();
    protected Set<String> userInvalidations = new HashSet<String>();
    protected Map<String, RealmModel> managedRealms = new HashMap<String, RealmModel>();
    protected Map<String, ApplicationModel> managedApplications = new HashMap<String, ApplicationModel>();
    protected Map<String, OAuthClientModel> managedClients = new HashMap<String, OAuthClientModel>();
    protected Map<String, RoleModel> managedRoles = new HashMap<String, RoleModel>();

    protected boolean clearAll;

    public DefaultCacheRealmProvider(RealmCache cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;

        session.getTransaction().enlistAfterCompletion(getTransaction());
    }

    @Override
    public boolean isEnabled() {
        return cache.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        cache.setEnabled(enabled);
    }

    @Override
    public RealmProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.getProvider(RealmProvider.class);
        return delegate;
    }

    @Override
    public void registerRealmInvalidation(String id) {
        realmInvalidations.add(id);
    }

    @Override
    public void registerApplicationInvalidation(String id) {
        appInvalidations.add(id);
    }

    @Override
    public void registerRoleInvalidation(String id) {
        roleInvalidations.add(id);
    }

    @Override
    public void registerOAuthClientInvalidation(String id) {
        clientInvalidations.add(id);
    }

    @Override
    public void registerUserInvalidation(String id) {
        userInvalidations.add(id);
    }

    protected void runInvalidations() {
        for (String id : realmInvalidations) {
            cache.invalidateCachedRealmById(id);
        }
        for (String id : roleInvalidations) {
            cache.invalidateRoleById(id);
        }
        for (String id : appInvalidations) {
            cache.invalidateCachedApplicationById(id);
        }
        for (String id : clientInvalidations) {
            cache.invalidateCachedOAuthClientById(id);
        }
    }

    private KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                if (delegate == null) return;
                if (clearAll) {
                    cache.clear();
                }
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }

    @Override
    public RealmModel createRealm(String name) {
        RealmModel realm = getDelegate().createRealm(name);
        if (!cache.isEnabled()) return realm;
        registerRealmInvalidation(realm.getId());
        return realm;
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmModel realm =  getDelegate().createRealm(id, name);
        if (!cache.isEnabled()) return realm;
        registerRealmInvalidation(realm.getId());
        return realm;
    }

    @Override
    public RealmModel getRealm(String id) {
        if (!cache.isEnabled()) return getDelegate().getRealm(id);
        CachedRealm cached = cache.getCachedRealm(id);
        if (cached == null) {
            RealmModel model = getDelegate().getRealm(id);
            if (model == null) return null;
            if (realmInvalidations.contains(id)) return model;
            cached = new CachedRealm(cache, this, model);
            cache.addCachedRealm(cached);
        } else if (realmInvalidations.contains(id)) {
            return getDelegate().getRealm(id);
        } else if (managedRealms.containsKey(id)) {
            return managedRealms.get(id);
        }
        RealmAdapter adapter = new RealmAdapter(cached, this);
        managedRealms.put(id, adapter);
        return adapter;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        if (!cache.isEnabled()) return getDelegate().getRealmByName(name);
        CachedRealm cached = cache.getCachedRealmByName(name);
        if (cached == null) {
            RealmModel model = getDelegate().getRealmByName(name);
            if (model == null) return null;
            if (realmInvalidations.contains(model.getId())) return model;
            cached = new CachedRealm(cache, this, model);
            cache.addCachedRealm(cached);
        } else if (realmInvalidations.contains(cached.getId())) {
            return getDelegate().getRealmByName(name);
        } else if (managedRealms.containsKey(cached.getId())) {
            return managedRealms.get(cached.getId());
        }
        RealmAdapter adapter = new RealmAdapter(cached, this);
        managedRealms.put(cached.getId(), adapter);
        return adapter;
    }

    @Override
    public List<RealmModel> getRealms() {
        // we don't cache this for now
        return getDelegate().getRealms();
    }

    @Override
    public boolean removeRealm(String id) {
        if (!cache.isEnabled()) return getDelegate().removeRealm(id);
        cache.invalidateCachedRealmById(id);

        RealmModel realm = getDelegate().getRealm(id);
        Set<RoleModel> realmRoles = null;
        if (realm != null) {
            realmRoles = realm.getRoles();
        }

        boolean didIt = getDelegate().removeRealm(id);
        realmInvalidations.add(id);

        // TODO: Temporary workaround to invalidate cached realm roles
        if (didIt && realmRoles != null) {
            for (RoleModel role : realmRoles) {
                roleInvalidations.add(role.getId());
            }
        }

        return didIt;
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getRoleById(id, realm);
        CachedRole cached = cache.getRole(id);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            RoleModel model = getDelegate().getRoleById(id, realm);
            if (model == null) return null;
            if (roleInvalidations.contains(id)) return model;
            if (model.getContainer() instanceof ApplicationModel) {
                cached = new CachedApplicationRole(((ApplicationModel) model.getContainer()).getId(), model, realm);
            } else {
                cached = new CachedRealmRole(model, realm);
            }
            cache.addCachedRole(cached);

        } else if (roleInvalidations.contains(id)) {
            return getDelegate().getRoleById(id, realm);
        } else if (managedRoles.containsKey(id)) {
            return managedRoles.get(id);
        }
        RoleAdapter adapter = new RoleAdapter(cached, cache, this, realm);
        managedRoles.put(id, adapter);
        return adapter;
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getApplicationById(id, realm);
        CachedApplication cached = cache.getApplication(id);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            ApplicationModel model = getDelegate().getApplicationById(id, realm);
            if (model == null) return null;
            if (appInvalidations.contains(id)) return model;
            cached = new CachedApplication(cache, getDelegate(), realm, model);
            cache.addCachedApplication(cached);
        } else if (appInvalidations.contains(id)) {
            return getDelegate().getApplicationById(id, realm);
        } else if (managedApplications.containsKey(id)) {
            return managedApplications.get(id);
        }
        ApplicationAdapter adapter = new ApplicationAdapter(realm, cached, this, cache);
        managedApplications.put(id, adapter);
        return adapter;
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getOAuthClientById(id, realm);
        CachedOAuthClient cached = cache.getOAuthClient(id);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            OAuthClientModel model = getDelegate().getOAuthClientById(id, realm);
            if (model == null) return null;
            if (clientInvalidations.contains(id)) return model;
            cached = new CachedOAuthClient(cache, getDelegate(), realm, model);
            cache.addCachedOAuthClient(cached);
        } else if (clientInvalidations.contains(id)) {
            return getDelegate().getOAuthClientById(id, realm);
        } else if (managedClients.containsKey(id)) {
            return managedClients.get(id);
        }
        OAuthClientAdapter adapter = new OAuthClientAdapter(realm, cached, this, cache);
        managedClients.put(id, adapter);
        return adapter;
    }

}
