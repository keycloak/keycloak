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

package org.keycloak.models.cache.infinispan;

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
import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientRole;
import org.keycloak.models.cache.infinispan.entities.CachedClientTemplate;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;
import org.keycloak.models.cache.infinispan.entities.CachedRealmRole;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.cache.infinispan.entities.ClientListQuery;
import org.keycloak.models.cache.infinispan.entities.RealmListQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 * - any relationship should be resolved from session.realms().  For example if JPA.getClientByClientId() is invoked,
 *  JPA should find the id of the client and then call session.realms().getClientById().  THis is to ensure that the cached
 *  object is invoked and all proper invalidation are being invoked.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StreamCacheRealmProvider implements CacheRealmProvider {
    protected static final Logger logger = Logger.getLogger(StreamCacheRealmProvider.class);
    public static final String REALM_CLIENTS_QUERY_SUFFIX = ".realm.clients";
    protected StreamRealmCache cache;
    protected KeycloakSession session;
    protected RealmProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Map<String, RealmModel> managedRealms = new HashMap<>();
    protected Map<String, ClientModel> managedApplications = new HashMap<>();
    protected Map<String, ClientTemplateModel> managedClientTemplates = new HashMap<>();
    protected Map<String, RoleModel> managedRoles = new HashMap<>();
    protected Map<String, GroupModel> managedGroups = new HashMap<>();
    protected Set<String> clientListInvalidations = new HashSet<>();
    protected Set<String> invalidations = new HashSet<>();

    protected boolean clearAll;

    public StreamCacheRealmProvider(StreamRealmCache cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        session.getTransaction().enlistPrepare(getPrepareTransaction());
        session.getTransaction().enlistAfterCompletion(getAfterTransaction());
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
        invalidations.add(id);
        cache.realmInvalidation(id, invalidations);
    }

    @Override
    public void registerClientInvalidation(String id) {
        invalidations.add(id);
        cache.clientInvalidation(id, invalidations);
    }
    @Override
    public void registerClientTemplateInvalidation(String id) {
        invalidations.add(id);
        cache.clientTemplateInvalidation(id, invalidations);
    }

    @Override
    public void registerRoleInvalidation(String id) {
        invalidations.add(id);
        cache.roleInvalidation(id, invalidations);
    }

    @Override
    public void registerGroupInvalidation(String id) {
        invalidations.add(id);
        cache.groupInvalidation(id, invalidations);
    }

    protected void runInvalidations() {
        for (String id : invalidations) {
            cache.invalidateObject(id);
        }
    }

    private KeycloakTransaction getPrepareTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                if (delegate == null) return;
                List<String> locks = new LinkedList<>();
                locks.addAll(invalidations);

                Collections.sort(locks); // lock ordering
                cache.getRevisions().startBatch();
                //if (!invalidates.isEmpty()) cache.getRevisions().getAdvancedCache().lock(invalidates);
                for (String lock : locks) {
                    boolean success = cache.getRevisions().getAdvancedCache().lock(lock);
                }

            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
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

    private KeycloakTransaction getAfterTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                try {
                    if (delegate == null) return;
                    if (clearAll) {
                        cache.clear();
                    }
                    runInvalidations();
                    transactionActive = false;
                } finally {
                    cache.endRevisionBatch();
                }
            }

            @Override
            public void rollback() {
                try {
                    setRollbackOnly = true;
                    runInvalidations();
                    transactionActive = false;
                } finally {
                    cache.endRevisionBatch();
                }
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
        CachedRealm cached = cache.get(id, CachedRealm.class);
        if (cached != null) {
            logger.tracev("by id cache hit: {0}", cached.getName());
        }
        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            RealmModel model = getDelegate().getRealm(id);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedRealm(loaded, model);
            cache.addRevisioned(cached);
        } else if (invalidations.contains(id)) {
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
        String cacheKey = "realm.query.by.name." + name;
        RealmListQuery query = cache.get(cacheKey, RealmListQuery.class);
        if (query != null) {
            logger.tracev("realm by name cache hit: {0}", name);
        }
        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            RealmModel model = getDelegate().getRealmByName(name);
            if (model == null) return null;
            if (invalidations.contains(model.getId())) return model;
            query = new RealmListQuery(loaded, cacheKey, model.getId());
            cache.addRevisioned(query);
            return model;
        } else if (invalidations.contains(cacheKey)) {
            return getDelegate().getRealmByName(name);
        } else {
            String realmId = query.getRealms().iterator().next();
            if (invalidations.contains(realmId)) {
                return getDelegate().getRealmByName(name);
            }
            return getRealm(realmId);
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
        if (getRealm(id) == null) return false;

        invalidations.add(getRealmClientsQueryCacheKey(id));
        cache.invalidateObject(id);
        cache.realmRemoval(id, invalidations);
        return getDelegate().removeRealm(id);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String clientId) {
        ClientModel client = getDelegate().addClient(realm, clientId);
        return addedClient(realm, client);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        ClientModel client = getDelegate().addClient(realm, id, clientId);
        return addedClient(realm, client);
    }

    private ClientModel addedClient(RealmModel realm, ClientModel client) {
        logger.trace("added Client.....");
        // need to invalidate realm client query cache every time as it may not be loaded on this node, but loaded on another
        invalidations.add(getRealmClientsQueryCacheKey(realm.getId()));
        invalidations.add(client.getId());
        cache.clientAdded(realm.getId(), client.getId(), invalidations);
        clientListInvalidations.add(realm.getId());
        return client;
    }

    private String getRealmClientsQueryCacheKey(String realm) {
        return realm + REALM_CLIENTS_QUERY_SUFFIX;
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm) {
        String cacheKey = getRealmClientsQueryCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || clientListInvalidations.contains(realm.getId());
        if (queryDB) {
            return getDelegate().getClients(realm);
        }

        ClientListQuery query = cache.get(cacheKey, ClientListQuery.class);
        if (query != null) {
            logger.tracev("getClients cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<ClientModel> model = getDelegate().getClients(realm);
            if (model == null) return null;
            Set<String> ids = new HashSet<>();
            for (ClientModel client : model) ids.add(client.getId());
            query = new ClientListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm clients cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query);
            return model;
        }
        List<ClientModel> list = new LinkedList<>();
        for (String id : query.getClients()) {
            ClientModel client = session.realms().getClientById(id, realm);
            if (client == null) {
                invalidations.add(cacheKey);
                return getDelegate().getClients(realm);
            }
            list.add(client);
        }
        return list;
    }

    @Override
    public boolean removeClient(String id, RealmModel realm) {
        ClientModel client = getClientById(id, realm);
        if (client == null) return false;
        // need to invalidate realm client query cache every time client list is changed
        invalidations.add(getRealmClientsQueryCacheKey(realm.getId()));
        clientListInvalidations.add(realm.getId());
        registerClientInvalidation(id);
        cache.clientRemoval(realm.getId(), id, invalidations);
        for (RoleModel role : client.getRoles()) {
            cache.roleInvalidation(role.getId(), invalidations);
        }
        return getDelegate().removeClient(id, realm);
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        CachedRole cached = cache.get(id, CachedRole.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            RoleModel model = getDelegate().getRoleById(id, realm);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            if (model.getContainer() instanceof ClientModel) {
                cached = new CachedClientRole(loaded, ((ClientModel) model.getContainer()).getId(), model, realm);
            } else {
                cached = new CachedRealmRole(loaded, model, realm);
            }
            cache.addRevisioned(cached);

        } else if (invalidations.contains(id)) {
            return getDelegate().getRoleById(id, realm);
        } else if (managedRoles.containsKey(id)) {
            return managedRoles.get(id);
        }
        RoleAdapter adapter = new RoleAdapter(cached,this, realm);
        managedRoles.put(id, adapter);
        return adapter;
    }

    @Override
    public GroupModel getGroupById(String id, RealmModel realm) {
        CachedGroup cached = cache.get(id, CachedGroup.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            GroupModel model = getDelegate().getGroupById(id, realm);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedGroup(loaded, realm, model);
            cache.addRevisioned(cached);

        } else if (invalidations.contains(id)) {
            return getDelegate().getGroupById(id, realm);
        } else if (managedGroups.containsKey(id)) {
            return managedGroups.get(id);
        }
        GroupAdapter adapter = new GroupAdapter(cached, this, session, realm);
        managedGroups.put(id, adapter);
        return adapter;
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        CachedClient cached = cache.get(id, CachedClient.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }
        if (cached != null) {
            logger.tracev("client by id cache hit: {0}", cached.getClientId());
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            ClientModel model = getDelegate().getClientById(id, realm);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedClient(loaded, realm, model);
            logger.tracev("adding client by id cache miss: {0}", cached.getClientId());
            cache.addRevisioned(cached);
        } else if (invalidations.contains(id)) {
            return getDelegate().getClientById(id, realm);
        } else if (managedApplications.containsKey(id)) {
            return managedApplications.get(id);
        }
        ClientAdapter adapter = new ClientAdapter(realm, cached, this, null);
        managedApplications.put(id, adapter);
        return adapter;
    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        String cacheKey = realm.getId() + ".client.query.by.clientId." + clientId;
        ClientListQuery query = cache.get(cacheKey, ClientListQuery.class);
        String id = null;

        if (query != null) {
            logger.tracev("client by name cache hit: {0}", clientId);
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            ClientModel model = getDelegate().getClientByClientId(clientId, realm);
            if (model == null) return null;
            if (invalidations.contains(model.getId())) return model;
            id = model.getId();
            query = new ClientListQuery(loaded, cacheKey, realm, id);
            logger.tracev("adding client by name cache miss: {0}", clientId);
            cache.addRevisioned(query);
        } else if (invalidations.contains(cacheKey)) {
            return getDelegate().getClientByClientId(clientId, realm);
        } else {
            id = query.getClients().iterator().next();
            if (invalidations.contains(id)) {
                return getDelegate().getClientByClientId(clientId, realm);
            }
        }
        return getClientById(id, realm);
    }

    @Override
    public ClientTemplateModel getClientTemplateById(String id, RealmModel realm) {
        CachedClientTemplate cached = cache.get(id, CachedClientTemplate.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            ClientTemplateModel model = getDelegate().getClientTemplateById(id, realm);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedClientTemplate(loaded, realm, model);
            cache.addRevisioned(cached);
        } else if (invalidations.contains(id)) {
            return getDelegate().getClientTemplateById(id, realm);
        } else if (managedClientTemplates.containsKey(id)) {
            return managedClientTemplates.get(id);
        }
        ClientTemplateModel adapter = new ClientTemplateAdapter(realm, cached, this);
        managedClientTemplates.put(id, adapter);
        return adapter;
    }

}
