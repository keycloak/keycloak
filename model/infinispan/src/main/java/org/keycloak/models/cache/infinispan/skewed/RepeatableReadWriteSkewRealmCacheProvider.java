/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.cache.infinispan.skewed;

import org.jboss.logging.Logger;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.entities.CachedClient;
import org.keycloak.models.cache.entities.CachedClientRole;
import org.keycloak.models.cache.entities.CachedClientTemplate;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRealmRole;
import org.keycloak.models.cache.entities.CachedRole;
import org.keycloak.models.cache.infinispan.ClientAdapter;
import org.keycloak.models.cache.infinispan.ClientTemplateAdapter;
import org.keycloak.models.cache.infinispan.GroupAdapter;
import org.keycloak.models.cache.infinispan.RealmAdapter;
import org.keycloak.models.cache.infinispan.RoleAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * DO NOT USE THIS!!
 *
 * Tries unsuccessfully to use Infinispan with REPEATABLE_READ, write-skew-checking
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RepeatableReadWriteSkewRealmCacheProvider implements CacheRealmProvider {
    protected static final Logger logger = Logger.getLogger(RepeatableReadWriteSkewRealmCacheProvider.class);

    protected RepeatableReadWriteSkewRealmCache cache;
    protected KeycloakSession session;
    protected RealmProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Set<String> realmInvalidations = new HashSet<>();
    protected Set<String> appInvalidations = new HashSet<>();
    protected Set<String> clientTemplateInvalidations = new HashSet<>();
    protected Set<String> roleInvalidations = new HashSet<>();
    protected Set<String> groupInvalidations = new HashSet<>();
    protected Map<String, RealmModel> managedRealms = new HashMap<>();
    protected Map<String, ClientModel> managedApplications = new HashMap<>();
    protected Map<String, ClientTemplateModel> managedClientTemplates = new HashMap<>();
    protected Map<String, RoleModel> managedRoles = new HashMap<>();
    protected Map<String, GroupModel> managedGroups = new HashMap<>();

    protected boolean clearAll;

    public RepeatableReadWriteSkewRealmCacheProvider(RepeatableReadWriteSkewRealmCache cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;

        session.getTransaction().enlistAfterCompletion(getTransaction());
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public MigrationModel getMigrationModel() {
        return getDelegate().getMigrationModel();
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
    public void registerClientTemplateInvalidation(String id) {
        clientTemplateInvalidations.add(id);
    }

    @Override
    public void registerRoleInvalidation(String id) {
        roleInvalidations.add(id);
    }

    @Override
    public void registerGroupInvalidation(String id) {
        groupInvalidations.add(id);

    }

    protected void runInvalidations() {
        for (String id : realmInvalidations) {
            cache.invalidateCachedRealmById(id);
        }
        for (String id : roleInvalidations) {
            cache.invalidateRoleById(id);
        }
        for (String id : groupInvalidations) {
            cache.invalidateGroupById(id);
        }
        for (String id : appInvalidations) {
            cache.invalidateCachedApplicationById(id);
        }
        for (String id : clientTemplateInvalidations) {
            cache.invalidateCachedClientTemplateById(id);
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
        registerRealmInvalidation(realm.getId());
        return realm;
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmModel realm =  getDelegate().createRealm(id, name);
        registerRealmInvalidation(realm.getId());
        return realm;
    }

    @Override
    public RealmModel getRealm(String id) {
        //cache.startBatch();
        cache.startBatch();
        boolean batchEnded = false;
        try {
            CachedRealm cached = cache.getCachedRealm(id);
            boolean wasNull = cached == null;
            if (cached == null) {
                RealmModel model = getDelegate().getRealm(id);
                if (model == null) return null;
                if (realmInvalidations.contains(id)) return model;
                cached = new CachedRealm(cache, this, model);
                cache.addCachedRealm(cached);
                try {
                    batchEnded = true;
                    cache.endBatch(true);
                    logger.trace("returning new cached realm");
                } catch (Exception exception) {
                    logger.trace("failed to add to cache", exception);
                    return model;
                }
            } else if (realmInvalidations.contains(id)) {
                return getDelegate().getRealm(id);
            } else if (managedRealms.containsKey(id)) {
                return managedRealms.get(id);
            }
            if (!wasNull) logger.trace("returning cached realm: " + cached.getName());
            RealmAdapter adapter = new RealmAdapter(cached, this);
            managedRealms.put(id, adapter);
            return adapter;
        } finally {
            if (!batchEnded) cache.endBatch(true);
        }
    }

    @Override
    public RealmModel getRealmByName(String name) {
        cache.startBatch();
        boolean batchEnded = false;
        try {
            CachedRealm cached = cache.getCachedRealmByName(name);
            boolean wasNull = cached == null;
            if (cached == null) {
                RealmModel model = getDelegate().getRealmByName(name);
                if (model == null) return null;
                if (realmInvalidations.contains(model.getId())) return model;
                cached = new CachedRealm(cache, this, model);
                cache.addCachedRealm(cached);
                try {
                    batchEnded = true;
                    cache.endBatch(true);
                    logger.trace("returning new cached realm: " + cached.getName());
                } catch (Exception exception) {
                    logger.trace("failed to add to cache", exception);
                    return model;
                }
            } else if (realmInvalidations.contains(cached.getId())) {
                return getDelegate().getRealmByName(name);
            } else if (managedRealms.containsKey(cached.getId())) {
                return managedRealms.get(cached.getId());
            }
            if (!wasNull) logger.trace("returning cached realm: " + cached.getName());
            RealmAdapter adapter = new RealmAdapter(cached, this);
            managedRealms.put(cached.getId(), adapter);
            return adapter;
        } finally {
            if (!batchEnded) cache.endBatch(true);

        }
    }

    @Override
    public List<RealmModel> getRealms() {
        // Retrieve realms from backend
        List<RealmModel> backendRealms = getDelegate().getRealms();

        // Return cache delegates to ensure cache invalidated during write operations
        List<RealmModel> cachedRealms = new LinkedList<RealmModel>();
        for (RealmModel realm : backendRealms) {
            RealmModel cached = getRealm(realm.getId());
            cachedRealms.add(cached);
        }
        return cachedRealms;
    }

    @Override
    public boolean removeRealm(String id) {
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
        cache.startBatch();
        boolean batchEnded = false;
        try {
            CachedRole cached = cache.getRole(id);
            if (cached != null && !cached.getRealm().equals(realm.getId())) {
                cached = null;
            }

            if (cached == null) {
                RoleModel model = getDelegate().getRoleById(id, realm);
                if (model == null) return null;
                if (roleInvalidations.contains(id)) return model;
                if (model.getContainer() instanceof ClientModel) {
                    cached = new CachedClientRole(((ClientModel) model.getContainer()).getId(), model, realm);
                } else {
                    cached = new CachedRealmRole(model, realm);
                }
                cache.addCachedRole(cached);
                try {
                    batchEnded = true;
                    cache.endBatch(true);
                } catch (Exception exception) {
                    logger.trace("failed to add to cache", exception);
                    return model;
                }

            } else if (roleInvalidations.contains(id)) {
                return getDelegate().getRoleById(id, realm);
            } else if (managedRoles.containsKey(id)) {
                return managedRoles.get(id);
            }
            RoleAdapter adapter = new RoleAdapter(cached, cache, this, realm);
            managedRoles.put(id, adapter);
            return adapter;
        } finally {
            if (!batchEnded) cache.endBatch(true);

        }
    }

    @Override
    public GroupModel getGroupById(String id, RealmModel realm) {
        cache.startBatch();
        boolean batchEnded = false;
        try {
            CachedGroup cached = cache.getGroup(id);
            if (cached != null && !cached.getRealm().equals(realm.getId())) {
                cached = null;
            }

            if (cached == null) {
                GroupModel model = getDelegate().getGroupById(id, realm);
                if (model == null) return null;
                if (groupInvalidations.contains(id)) return model;
                cached = new CachedGroup(realm, model);
                cache.addCachedGroup(cached);
                try {
                    batchEnded = true;
                    cache.endBatch(true);
                } catch (Exception exception) {
                    logger.trace("failed to add to cache", exception);
                    return model;
                }

            } else if (groupInvalidations.contains(id)) {
                return getDelegate().getGroupById(id, realm);
            } else if (managedGroups.containsKey(id)) {
                return managedGroups.get(id);
            }
            GroupAdapter adapter = new GroupAdapter(cached, this, session, realm);
            managedGroups.put(id, adapter);
            return adapter;
        } finally {
            if (!batchEnded) cache.endBatch(true);

        }
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        cache.startBatch();
        boolean batchEnded = false;
        CachedClient cached = cache.getApplication(id);
        try {
            if (cached != null && !cached.getRealm().equals(realm.getId())) {
                cached = null;
            }

            if (cached == null) {
                ClientModel model = getDelegate().getClientById(id, realm);
                if (model == null) return null;
                if (appInvalidations.contains(id)) return model;
                cached = new CachedClient(cache, getDelegate(), realm, model);
                cache.addCachedClient(cached);
                try {
                    batchEnded = true;
                    cache.endBatch(true);
                } catch (Exception exception) {
                    logger.trace("failed to add to cache", exception);
                    return model;
                }
            } else if (appInvalidations.contains(id)) {
                return getDelegate().getClientById(id, realm);
            } else if (managedApplications.containsKey(id)) {
                return managedApplications.get(id);
            }
            ClientAdapter adapter = new ClientAdapter(realm, cached, this, cache);
            managedApplications.put(id, adapter);
            return adapter;
        } finally {
            if (!batchEnded) cache.endBatch(true);

        }
    }
    @Override
    public ClientTemplateModel getClientTemplateById(String id, RealmModel realm) {
        cache.startBatch();
        boolean batchEnded = false;
        try {
            CachedClientTemplate cached = cache.getClientTemplate(id);
            if (cached != null && !cached.getRealm().equals(realm.getId())) {
                cached = null;
            }

            if (cached == null) {
                ClientTemplateModel model = getDelegate().getClientTemplateById(id, realm);
                if (model == null) return null;
                if (clientTemplateInvalidations.contains(id)) return model;
                cached = new CachedClientTemplate(cache, getDelegate(), realm, model);
                cache.addCachedClientTemplate(cached);
                try {
                    batchEnded = true;
                    cache.endBatch(true);
                } catch (Exception exception) {
                    logger.trace("failed to add to cache", exception);
                    return model;
                }
            } else if (clientTemplateInvalidations.contains(id)) {
                return getDelegate().getClientTemplateById(id, realm);
            } else if (managedClientTemplates.containsKey(id)) {
                return managedClientTemplates.get(id);
            }
            ClientTemplateModel adapter = new ClientTemplateAdapter(realm, cached, this, cache);
            managedClientTemplates.put(id, adapter);
            return adapter;
        } finally {
            if (!batchEnded) cache.endBatch(true);

        }
    }

}
