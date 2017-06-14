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
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
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
import org.keycloak.models.cache.CachedRealmModel;
import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientRole;
import org.keycloak.models.cache.infinispan.entities.CachedClientTemplate;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;
import org.keycloak.models.cache.infinispan.entities.CachedRealmRole;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.cache.infinispan.entities.ClientListQuery;
import org.keycloak.models.cache.infinispan.entities.GroupListQuery;
import org.keycloak.models.cache.infinispan.entities.RealmListQuery;
import org.keycloak.models.cache.infinispan.entities.RoleListQuery;
import org.keycloak.models.cache.infinispan.events.ClientAddedEvent;
import org.keycloak.models.cache.infinispan.events.ClientRemovedEvent;
import org.keycloak.models.cache.infinispan.events.ClientTemplateEvent;
import org.keycloak.models.cache.infinispan.events.ClientUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.GroupAddedEvent;
import org.keycloak.models.cache.infinispan.events.GroupMovedEvent;
import org.keycloak.models.cache.infinispan.events.GroupRemovedEvent;
import org.keycloak.models.cache.infinispan.events.GroupUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.RealmRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RealmUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.RoleAddedEvent;
import org.keycloak.models.cache.infinispan.events.RoleRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RoleUpdatedEvent;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * - the high level architecture of this cache is an invalidation cache.
 * - the cache is manual/custom versioned.  When a model is updated, we remove it from the cache
 * which causes an invalidation message to be sent across the cluster.
 * - We had to do it this way because Infinispan REPEATABLE_READ
 * wouldn't cut it in invalidation mode.  Also, REPEATABLE_READ doesn't work very well on relationships and items that are
 * not in the cache.
 * - There are two Infinispan caches.  One clustered that holds actual objects and a another local one that holds revision
 * numbers of cached objects.  Whenever a cached object is removed (invalidated), the local revision
 * cache number or that key is bumped higher based on a local version counter.  Whenever a cache entry is fetched, this
 * revision number is also fetched and compared against the revision number in the cache entry to see if the cache entry
 * is stale.  Whenever a cache entry is added, this revision number is also checked against the revision cache.
 * - Revision entries are actually never removed (although they could be evicted by cache eviction policies).  The reason for this
 * is that it is possible for a stale object to be inserted if one thread loads and the data is updated in the database before
 * it is added to the cache.  So, we keep the version number around for this.
 * - In a transaction, objects are registered to be invalidated.  If an object is marked for invalidation within a transaction
 * a cached object should never be returned.  An DB adapter should always be returned.
 * - After DB commits, the objects marked for invalidation are invalidated, or rather removed from the cache.  At this time
 * the revision cache entry for this object has its version number bumped.
 * - Whenever an object is marked for invalidation, the cache is also searched for any objects that are related to this object
 * and need to also be evicted/removed.  We use the Infinispan Stream SPI for this.
 *
 * ClientList caches:
 * - lists of clients are cached in a specific cache entry i.e. realm clients, find client by clientId
 * - realm client lists need to be invalidated and evited whenever a client is added or removed from a realm.  RealmProvider
 * now has addClient/removeClient at its top level.  All adapaters should use these methods so that the appropriate invalidations
 * can be registered.
 * - whenever a client is added/removed the realm of the client is added to a listInvalidations set
 * this set must be checked before sending back or caching a cached query.  This check is required to
 * avoid caching an uncommitted removal/add in a query cache.
 * - when a client is removed, any queries that contain that client must also be removed.
 * - a client removal will also cause anything that is contained and cached within that client to be removed
 *
 * Clustered caches:
 * - There is a Infinispan @Listener registered.  If an invalidation event happens, this is treated like
 * the object was removed from the database and will perform evictions based on that assumption.
 * - Eviction events will also cascade other evictions, but not assume this is a db removal.
 * - With an invalidation cache, if you remove an entry on node 1 and this entry does not exist on node 2, node 2 will not receive a @Listener invalidation event.
 * so, hat we have to put a marker entry in the invalidation cache before we read from the DB, so if the DB changes in between reading and adding a cache entry, the cache will be notified and bump
 * the version information.
 *
 * DBs with Repeatable Read:
 * - DBs like MySQL are Repeatable Read by default.  So, if you query a Client for instance, it will always return the same result in the same transaction even if the DB
 * was updated in between these queries.  This makes it possible to store stale cache entries.  To avoid this problem, this class stores the current local version counter
 * at the beginningof the transaction.  Whenever an entry is added to the cache, the current coutner is compared against the counter at the beginning of the tx.  If the current
 * is greater, then don't cache.
 *
 * Groups and Roles:
 * - roles are tricky because of composites.  Composite lists are cached too.  So, when a role is removed
 * we also iterate and invalidate any role or group that contains that role being removed.
 *
 * - any relationship should be resolved from session.realms().  For example if JPA.getClientByClientId() is invoked,
 *  JPA should find the id of the client and then call session.realms().getClientById().  THis is to ensure that the cached
 *  object is invoked and all proper invalidation are being invoked.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmCacheSession implements CacheRealmProvider {
    protected static final Logger logger = Logger.getLogger(RealmCacheSession.class);
    public static final String REALM_CLIENTS_QUERY_SUFFIX = ".realm.clients";
    public static final String ROLES_QUERY_SUFFIX = ".roles";
    public static final String ROLE_BY_NAME_QUERY_SUFFIX = ".role.by-name";
    protected RealmCacheManager cache;
    protected KeycloakSession session;
    protected RealmProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Map<String, RealmAdapter> managedRealms = new HashMap<>();
    protected Map<String, ClientAdapter> managedApplications = new HashMap<>();
    protected Map<String, ClientTemplateAdapter> managedClientTemplates = new HashMap<>();
    protected Map<String, RoleAdapter> managedRoles = new HashMap<>();
    protected Map<String, GroupAdapter> managedGroups = new HashMap<>();
    protected Set<String> listInvalidations = new HashSet<>();
    protected Set<String> invalidations = new HashSet<>();
    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster

    protected boolean clearAll;
    protected final long startupRevision;

    public RealmCacheSession(RealmCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        this.startupRevision = cache.getCurrentCounter();
        session.getTransactionManager().enlistPrepare(getPrepareTransaction());
        session.getTransactionManager().enlistAfterCompletion(getAfterTransaction());
    }

    public long getStartupRevision() {
        return startupRevision;
    }

    public boolean isInvalid(String id) {
        return invalidations.contains(id);
    }

    @Override
    public void clear() {
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(InfinispanCacheRealmProviderFactory.REALM_CLEAR_CACHE_EVENTS, new ClearCacheEvent(), false);
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
    public void registerRealmInvalidation(String id, String name) {
        cache.realmUpdated(id, name, invalidations);
        RealmAdapter adapter = managedRealms.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(RealmUpdatedEvent.create(id, name));
    }

    @Override
    public void registerClientInvalidation(String id, String clientId, String realmId) {
        invalidateClient(id);
        invalidationEvents.add(ClientUpdatedEvent.create(id, clientId, realmId));
        cache.clientUpdated(realmId, id, clientId, invalidations);
    }

    private void invalidateClient(String id) {
        invalidations.add(id);
        ClientAdapter adapter = managedApplications.get(id);
        if (adapter != null) adapter.invalidate();
    }

    @Override
    public void registerClientTemplateInvalidation(String id) {
        invalidateClientTemplate(id);
        // Note: Adding/Removing client template is supposed to invalidate CachedRealm as well, so the list of clientTemplates is invalidated.
        // But separate RealmUpdatedEvent will be sent for it. So ClientTemplateEvent don't need to take care of it.
        invalidationEvents.add(ClientTemplateEvent.create(id));
    }

    private void invalidateClientTemplate(String id) {
        invalidations.add(id);
        ClientTemplateAdapter adapter = managedClientTemplates.get(id);
        if (adapter != null) adapter.invalidate();
    }

    @Override
    public void registerRoleInvalidation(String id, String roleName, String roleContainerId) {
        invalidateRole(id);
        cache.roleUpdated(roleContainerId, roleName, invalidations);
        invalidationEvents.add(RoleUpdatedEvent.create(id, roleName, roleContainerId));
    }

    private void roleRemovalInvalidations(String roleId, String roleName, String roleContainerId) {
        Set<String> newInvalidations = new HashSet<>();
        cache.roleRemoval(roleId, roleName, roleContainerId, newInvalidations);
        invalidations.addAll(newInvalidations);
        // need to make sure that scope and group mapping clients and groups are invalidated
        for (String id : newInvalidations) {
            ClientAdapter adapter = managedApplications.get(id);
            if (adapter != null) {
                adapter.invalidate();
                continue;
            }
            GroupAdapter group = managedGroups.get(id);
            if (group != null) {
                group.invalidate();
                continue;
            }
            ClientTemplateAdapter clientTemplate = managedClientTemplates.get(id);
            if (clientTemplate != null) {
                clientTemplate.invalidate();
                continue;
            }
            RoleAdapter role = managedRoles.get(id);
            if (role != null) {
                role.invalidate();
                continue;
            }


        }
    }




    private void invalidateRole(String id) {
        invalidations.add(id);
        RoleAdapter adapter = managedRoles.get(id);
        if (adapter != null) adapter.invalidate();
    }

    private void addedRole(String roleId, String roleContainerId) {
        // this is needed so that a new role that hasn't been committed isn't cached in a query
        listInvalidations.add(roleContainerId);

        invalidateRole(roleId);
        cache.roleAdded(roleContainerId, invalidations);
        invalidationEvents.add(RoleAddedEvent.create(roleId, roleContainerId));
    }

    @Override
    public void registerGroupInvalidation(String id) {
        invalidateGroup(id, null, false);
        addGroupEventIfAbsent(GroupUpdatedEvent.create(id));
    }

    private void invalidateGroup(String id, String realmId, boolean invalidateQueries) {
        invalidateGroup(id);
        if (invalidateQueries) {
            cache.groupQueriesInvalidations(realmId, invalidations);
        }
    }

    private void invalidateGroup(String id) {
        invalidations.add(id);
        GroupAdapter adapter = managedGroups.get(id);
        if (adapter != null) adapter.invalidate();
    }

    protected void runInvalidations() {
        for (String id : invalidations) {
            cache.invalidateObject(id);
        }

        cache.sendInvalidationEvents(session, invalidationEvents);
    }

    private KeycloakTransaction getPrepareTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                /*  THIS WAS CAUSING DEADLOCK IN A CLUSTER
                if (delegate == null) return;
                List<String> locks = new LinkedList<>();
                locks.addAll(invalidations);

                Collections.sort(locks); // lock ordering
                cache.getRevisions().startBatch();

                if (!locks.isEmpty()) cache.getRevisions().getAdvancedCache().lock(locks);
                */

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
        registerRealmInvalidation(realm.getId(), realm.getName());
        return realm;
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmModel realm =  getDelegate().createRealm(id, name);
        registerRealmInvalidation(realm.getId(), realm.getName());
        return realm;
    }

    @Override
    public RealmModel getRealm(String id) {
        CachedRealm cached = cache.get(id, CachedRealm.class);
        if (cached != null) {
            logger.tracev("by id cache hit: {0}", cached.getName());
        }
        boolean wasCached = false;
        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            RealmModel model = getDelegate().getRealm(id);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedRealm(loaded, model);
            cache.addRevisioned(cached, startupRevision);
            wasCached =true;
        } else if (invalidations.contains(id)) {
            return getDelegate().getRealm(id);
        } else if (managedRealms.containsKey(id)) {
            return managedRealms.get(id);
        }
        RealmAdapter adapter = new RealmAdapter(session, cached, this);
        if (wasCached) {
            CachedRealmModel.RealmCachedEvent event = new CachedRealmModel.RealmCachedEvent() {
                @Override
                public CachedRealmModel getRealm() {
                    return adapter;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            };
            session.getKeycloakSessionFactory().publish(event);
        }
        managedRealms.put(id, adapter);
        return adapter;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        String cacheKey = getRealmByNameCacheKey(name);
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
            cache.addRevisioned(query, startupRevision);
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

    static String getRealmByNameCacheKey(String name) {
        return "realm.query.by.name." + name;
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
        RealmModel realm = getRealm(id);
        if (realm == null) return false;

        cache.invalidateObject(id);
        invalidationEvents.add(RealmRemovedEvent.create(id, realm.getName()));
        cache.realmRemoval(id, realm.getName(), invalidations);
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

        invalidateClient(client.getId());
        // this is needed so that a client that hasn't been committed isn't cached in a query
        listInvalidations.add(realm.getId());

        invalidationEvents.add(ClientAddedEvent.create(client.getId(), client.getClientId(), realm.getId()));
        cache.clientAdded(realm.getId(), client.getId(), client.getClientId(), invalidations);
        return client;
    }

    static String getRealmClientsQueryCacheKey(String realm) {
        return realm + REALM_CLIENTS_QUERY_SUFFIX;
    }

    static String getGroupsQueryCacheKey(String realm) {
        return realm + ".groups";
    }

    static String getTopGroupsQueryCacheKey(String realm) {
        return realm + ".top.groups";
    }

    static String getRolesCacheKey(String container) {
        return container + ROLES_QUERY_SUFFIX;
    }
    static String getRoleByNameCacheKey(String container, String name) {
        return container + "." + name + ROLES_QUERY_SUFFIX;
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm) {
        String cacheKey = getRealmClientsQueryCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
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
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        List<ClientModel> list = new LinkedList<>();
        for (String id : query.getClients()) {
            ClientModel client = session.realms().getClientById(id, realm);
            if (client == null) {
                // TODO: Handle with cluster invalidations too
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

        invalidateClient(client.getId());
        // this is needed so that a client that hasn't been committed isn't cached in a query
        listInvalidations.add(realm.getId());

        invalidationEvents.add(ClientRemovedEvent.create(client));
        cache.clientRemoval(realm.getId(), id, client.getClientId(), invalidations);

        for (RoleModel role : client.getRoles()) {
            roleRemovalInvalidations(role.getId(), role.getName(), client.getId());
        }
        return getDelegate().removeClient(id, realm);
    }


    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
        return addRealmRole(realm, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        RoleModel role = getDelegate().addRealmRole(realm, id, name);
        addedRole(role.getId(), realm.getId());
        return role;
    }

    @Override
    public Set<RoleModel> getRealmRoles(RealmModel realm) {
        String cacheKey = getRolesCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getDelegate().getRealmRoles(realm);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getRealmRoles cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<RoleModel> model = getDelegate().getRealmRoles(realm);
            if (model == null) return null;
            Set<String> ids = new HashSet<>();
            for (RoleModel role : model) ids.add(role.getId());
            query = new RoleListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm roles cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        Set<RoleModel> list = new HashSet<>();
        for (String id : query.getRoles()) {
            RoleModel role = session.realms().getRoleById(id, realm);
            if (role == null) {
                invalidations.add(cacheKey);
                return getDelegate().getRealmRoles(realm);
            }
            list.add(role);
        }
        return list;
    }

    @Override
    public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        String cacheKey = getRolesCacheKey(client.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(client.getId());
        if (queryDB) {
            return getDelegate().getClientRoles(realm, client);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getClientRoles cache hit: {0}", client.getClientId());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<RoleModel> model = getDelegate().getClientRoles(realm, client);
            if (model == null) return null;
            Set<String> ids = new HashSet<>();
            for (RoleModel role : model) ids.add(role.getId());
            query = new RoleListQuery(loaded, cacheKey, realm, ids, client.getClientId());
            logger.tracev("adding client roles cache miss: client {0} key {1}", client.getClientId(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        Set<RoleModel> list = new HashSet<>();
        for (String id : query.getRoles()) {
            RoleModel role = session.realms().getRoleById(id, realm);
            if (role == null) {
                invalidations.add(cacheKey);
                return getDelegate().getClientRoles(realm, client);
            }
            list.add(role);
        }
        return list;
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        return addClientRole(realm, client, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        RoleModel role = getDelegate().addClientRole(realm, client, id, name);
        addedRole(role.getId(), client.getId());
        return role;
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        String cacheKey = getRoleByNameCacheKey(realm.getId(), name);
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getDelegate().getRealmRole(realm, name);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getRealmRole cache hit: {0}.{1}", realm.getName(), name);
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            RoleModel model = getDelegate().getRealmRole(realm, name);
            if (model == null) return null;
            query = new RoleListQuery(loaded, cacheKey, realm, model.getId());
            logger.tracev("adding realm role cache miss: client {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        RoleModel role = getRoleById(query.getRoles().iterator().next(), realm);
        if (role == null) {
            invalidations.add(cacheKey);
            return getDelegate().getRealmRole(realm, name);
        }
        return role;
    }

    @Override
    public RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        String cacheKey = getRoleByNameCacheKey(client.getId(), name);
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(client.getId());
        if (queryDB) {
            return getDelegate().getClientRole(realm, client, name);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getClientRole cache hit: {0}.{1}", client.getClientId(), name);
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            RoleModel model = getDelegate().getClientRole(realm, client, name);
            if (model == null) return null;
            query = new RoleListQuery(loaded, cacheKey, realm, model.getId(), client.getClientId());
            logger.tracev("adding client role cache miss: client {0} key {1}", client.getClientId(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        RoleModel role = getRoleById(query.getRoles().iterator().next(), realm);
        if (role == null) {
            invalidations.add(cacheKey);
            return getDelegate().getClientRole(realm, client, name);
        }
        return role;
    }

    @Override
    public boolean removeRole(RealmModel realm, RoleModel role) {
        listInvalidations.add(role.getContainer().getId());

        invalidateRole(role.getId());
        invalidationEvents.add(RoleRemovedEvent.create(role.getId(), role.getName(), role.getContainer().getId()));
        roleRemovalInvalidations(role.getId(), role.getName(), role.getContainer().getId());

        return getDelegate().removeRole(realm, role);
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
            if (model.isClientRole()) {
                cached = new CachedClientRole(loaded, model.getContainerId(), model, realm);
            } else {
                cached = new CachedRealmRole(loaded, model, realm);
            }
            cache.addRevisioned(cached, startupRevision);

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
            cache.addRevisioned(cached, startupRevision);

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
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        invalidateGroup(group.getId(), realm.getId(), true);
        if (toParent != null) invalidateGroup(group.getId(), realm.getId(), false); // Queries already invalidated
        listInvalidations.add(realm.getId());

        invalidationEvents.add(GroupMovedEvent.create(group, toParent, realm.getId()));
        getDelegate().moveGroup(realm, group, toParent);
    }

    @Override
    public List<GroupModel> getGroups(RealmModel realm) {
        String cacheKey = getGroupsQueryCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getDelegate().getGroups(realm);
        }

        GroupListQuery query = cache.get(cacheKey, GroupListQuery.class);
        if (query != null) {
            logger.tracev("getGroups cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<GroupModel> model = getDelegate().getGroups(realm);
            if (model == null) return null;
            Set<String> ids = new HashSet<>();
            for (GroupModel client : model) ids.add(client.getId());
            query = new GroupListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm getGroups cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        List<GroupModel> list = new LinkedList<>();
        for (String id : query.getGroups()) {
            GroupModel group = session.realms().getGroupById(id, realm);
            if (group == null) {
                invalidations.add(cacheKey);
                return getDelegate().getGroups(realm);
            }
            list.add(group);
        }
        return list;
    }

    @Override
    public List<GroupModel> getTopLevelGroups(RealmModel realm) {
        String cacheKey = getTopGroupsQueryCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getDelegate().getTopLevelGroups(realm);
        }

        GroupListQuery query = cache.get(cacheKey, GroupListQuery.class);
        if (query != null) {
            logger.tracev("getTopLevelGroups cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<GroupModel> model = getDelegate().getTopLevelGroups(realm);
            if (model == null) return null;
            Set<String> ids = new HashSet<>();
            for (GroupModel client : model) ids.add(client.getId());
            query = new GroupListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm getTopLevelGroups cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        List<GroupModel> list = new LinkedList<>();
        for (String id : query.getGroups()) {
            GroupModel group = session.realms().getGroupById(id, realm);
            if (group == null) {
                invalidations.add(cacheKey);
                return getDelegate().getTopLevelGroups(realm);
            }
            list.add(group);
        }
        return list;
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        invalidateGroup(group.getId(), realm.getId(), true);
        listInvalidations.add(realm.getId());
        cache.groupQueriesInvalidations(realm.getId(), invalidations);
        if (group.getParentId() != null) {
            invalidateGroup(group.getParentId(), realm.getId(), false); // Queries already invalidated
        }

        invalidationEvents.add(GroupRemovedEvent.create(group, realm.getId()));

        return getDelegate().removeGroup(realm, group);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String name) {
        GroupModel group = getDelegate().createGroup(realm, name);
        return groupAdded(realm, group);
    }

    private GroupModel groupAdded(RealmModel realm, GroupModel group) {
        listInvalidations.add(realm.getId());
        cache.groupQueriesInvalidations(realm.getId(), invalidations);
        invalidations.add(group.getId());
        invalidationEvents.add(GroupAddedEvent.create(group.getId(), realm.getId()));
        return group;
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name) {
        GroupModel group = getDelegate().createGroup(realm, id, name);
        return groupAdded(realm, group);
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        invalidateGroup(subGroup.getId(), realm.getId(), true);
        if (subGroup.getParentId() != null) {
            invalidateGroup(subGroup.getParentId(), realm.getId(), false); // Queries already invalidated
        }

        addGroupEventIfAbsent(GroupMovedEvent.create(subGroup, null, realm.getId()));

        getDelegate().addTopLevelGroup(realm, subGroup);

    }

    private void addGroupEventIfAbsent(InvalidationEvent eventToAdd) {
        String groupId = eventToAdd.getId();

        // Check if we have existing event with bigger priority
        boolean eventAlreadyExists = invalidationEvents.stream().filter((InvalidationEvent event) -> {

            return (event.getId().equals(groupId)) && (event instanceof GroupAddedEvent || event instanceof GroupMovedEvent || event instanceof GroupRemovedEvent);

        }).findFirst().isPresent();

        if (!eventAlreadyExists) {
            invalidationEvents.add(eventToAdd);
        }
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
            cache.addRevisioned(cached, startupRevision);
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
        String cacheKey = getClientByClientIdCacheKey(clientId, realm.getId());
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
            cache.addRevisioned(query, startupRevision);
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

    static String getClientByClientIdCacheKey(String clientId, String realmId) {
        return realmId + ".client.query.by.clientId." + clientId;
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
            cache.addRevisioned(cached, startupRevision);
        } else if (invalidations.contains(id)) {
            return getDelegate().getClientTemplateById(id, realm);
        } else if (managedClientTemplates.containsKey(id)) {
            return managedClientTemplates.get(id);
        }
        ClientTemplateAdapter adapter = new ClientTemplateAdapter(realm, cached, this);
        managedClientTemplates.put(id, adapter);
        return adapter;
    }

}
