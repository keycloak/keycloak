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
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CachedRealmModel;
import org.keycloak.models.cache.infinispan.entities.*;
import org.keycloak.models.cache.infinispan.events.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.LegacyStoreManagers;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.client.ClientStorageProviderModel;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
    private static final String SCOPE_KEY_DEFAULT = "default";
    private static final String SCOPE_KEY_OPTIONAL = "optional";
    protected RealmCacheManager cache;
    protected KeycloakSession session;
    protected RealmProvider realmDelegate;
    protected ClientProvider clientDelegate;
    protected ClientScopeProvider clientScopeDelegate;
    protected GroupProvider groupDelegate;
    protected RoleProvider roleDelegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Map<String, RealmAdapter> managedRealms = new HashMap<>();
    protected Map<String, ClientModel> managedApplications = new HashMap<>();
    protected Map<String, ClientScopeAdapter> managedClientScopes = new HashMap<>();
    protected Map<String, RoleAdapter> managedRoles = new HashMap<>();
    protected Map<String, GroupAdapter> managedGroups = new HashMap<>();
    protected Set<String> listInvalidations = new HashSet<>();
    protected Set<String> invalidations = new HashSet<>();
    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster

    protected boolean clearAll;
    protected final long startupRevision;
    private final LegacyStoreManagers datastoreProvider;

    public RealmCacheSession(RealmCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        this.startupRevision = cache.getCurrentCounter();
        this.datastoreProvider = (LegacyStoreManagers) session.getProvider(DatastoreProvider.class);
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
        cluster.notify(InfinispanCacheRealmProviderFactory.REALM_CLEAR_CACHE_EVENTS, new ClearCacheEvent(), false, ClusterProvider.DCNotify.ALL_DCS);
    }

    @Override
    public RealmProvider getRealmDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (realmDelegate != null) return realmDelegate;
        realmDelegate = session.getProvider(RealmProvider.class);
        return realmDelegate;
    }
    public ClientProvider getClientDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (clientDelegate != null) return clientDelegate;
        clientDelegate = this.datastoreProvider.clientStorageManager();
        return clientDelegate;
    }
    public ClientScopeProvider getClientScopeDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (clientScopeDelegate != null) return clientScopeDelegate;
        clientScopeDelegate = this.datastoreProvider.clientScopeStorageManager();
        return clientScopeDelegate;
    }
    public RoleProvider getRoleDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (roleDelegate != null) return roleDelegate;
        roleDelegate = this.datastoreProvider.roleStorageManager();
        return roleDelegate;
    }
    public GroupProvider getGroupDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (groupDelegate != null) return groupDelegate;
        groupDelegate = this.datastoreProvider.groupStorageManager();
        return groupDelegate;
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
        ClientModel adapter = managedApplications.get(id);
        if (adapter != null && adapter instanceof ClientAdapter) ((ClientAdapter)adapter).invalidate();
    }

    @Override
    public void registerClientScopeInvalidation(String id, String realmId) {
        invalidateClientScope(id);
        cache.clientScopeUpdated(realmId, invalidations);
        invalidationEvents.add(ClientTemplateEvent.create(id));
    }

    private void invalidateClientScope(String id) {
        invalidations.add(id);
        ClientScopeAdapter adapter = managedClientScopes.get(id);
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
            ClientModel adapter = managedApplications.get(id);
            if (adapter != null && adapter instanceof ClientAdapter){
                ((ClientAdapter)adapter).invalidate();
                continue;
            }
            GroupAdapter group = managedGroups.get(id);
            if (group != null) {
                group.invalidate();
                continue;
            }
            ClientScopeAdapter clientScope = managedClientScopes.get(id);
            if (clientScope != null) {
                clientScope.invalidate();
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

        cache.sendInvalidationEvents(session, invalidationEvents, InfinispanCacheRealmProviderFactory.REALM_INVALIDATION_EVENTS);
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
        RealmModel realm = getRealmDelegate().createRealm(name);
        registerRealmInvalidation(realm.getId(), realm.getName());
        return realm;
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmModel realm =  getRealmDelegate().createRealm(id, name);
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
            RealmModel model = getRealmDelegate().getRealm(id);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedRealm(loaded, model);
            cache.addRevisioned(cached, startupRevision);
            wasCached =true;
        } else if (invalidations.contains(id)) {
            return getRealmDelegate().getRealm(id);
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
            RealmModel model = getRealmDelegate().getRealmByName(name);
            if (model == null) return null;
            if (invalidations.contains(model.getId())) return model;
            query = new RealmListQuery(loaded, cacheKey, model.getId());
            cache.addRevisioned(query, startupRevision);
            return model;
        } else if (invalidations.contains(cacheKey)) {
            return getRealmDelegate().getRealmByName(name);
        } else {
            String realmId = query.getRealms().iterator().next();
            if (invalidations.contains(realmId)) {
                return getRealmDelegate().getRealmByName(name);
            }
            return getRealm(realmId);
        }
    }

    static String getRealmByNameCacheKey(String name) {
        return "realm.query.by.name." + name;
    }

    @Override
    public Stream<RealmModel> getRealmsWithProviderTypeStream(Class<?> type) {
        // Retrieve realms from backend
        return getRealms(getRealmDelegate().getRealmsWithProviderTypeStream(type));
    }

    @Override
    public Stream<RealmModel> getRealmsStream() {
        // Retrieve realms from backend
        return getRealms(getRealmDelegate().getRealmsStream());
    }

    private Stream<RealmModel> getRealms(Stream<RealmModel> backendRealms) {
        // Return cache delegates to ensure cache invalidated during write operations
        return backendRealms.map(RealmModel::getId).map(this::getRealm);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmModel realm = getRealm(id);
        if (realm == null) return false;

        listInvalidations.add(id);
        evictRealmOnRemoval(realm);
        return getRealmDelegate().removeRealm(id);
    }

    public void evictRealmOnRemoval(RealmModel realm) {
        cache.invalidateObject(realm.getId());
        invalidationEvents.add(RealmRemovedEvent.create(realm.getId(), realm.getName()));
        cache.realmRemoval(realm.getId(), realm.getName(), invalidations);
    }


    @Override
    public ClientModel addClient(RealmModel realm, String clientId) {
        ClientModel client = getClientDelegate().addClient(realm, clientId);
        return addedClient(realm, client);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        ClientModel client = getClientDelegate().addClient(realm, id, clientId);
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

    static String getClientScopesCacheKey(String realm) {
        return realm + ".clientscopes";
    }

    static String getClientScopesCacheKey(String client, boolean defaultScope) {
        return client + "." + (defaultScope ? SCOPE_KEY_DEFAULT : SCOPE_KEY_OPTIONAL) + ".clientscopes";
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
    public Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return getClientDelegate().getClientsStream(realm, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        return getClientDelegate().getClientsStream(realm);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm) {
        return getClientDelegate().getAlwaysDisplayInConsoleClientsStream(realm);
    }

    @Override
    public Map<ClientModel, Set<String>> getAllRedirectUrisOfEnabledClients(RealmModel realm) {
        return getClientDelegate().getAllRedirectUrisOfEnabledClients(realm);
    }

    @Override
    public void removeClients(RealmModel realm) {
        getClientDelegate().removeClients(realm);
    }

    @Override
    public boolean removeClient(RealmModel realm, String id) {
        ClientModel client = getClientById(realm, id);
        if (client == null) return false;

        invalidateClient(client.getId());
        // this is needed so that a client that hasn't been committed isn't cached in a query
        listInvalidations.add(realm.getId());

        invalidationEvents.add(ClientRemovedEvent.create(client));
        cache.clientRemoval(realm.getId(), id, client.getClientId(), invalidations);

        client.getRolesStream().forEach(role -> {
            roleRemovalInvalidations(role.getId(), role.getName(), client.getId());
        });
        
        if (client.isServiceAccountsEnabled()) {
            UserModel serviceAccount = session.users().getServiceAccount(client);

            if (serviceAccount != null) {
                session.users().removeUser(realm, serviceAccount);
            }
        }

        return getClientDelegate().removeClient(realm, id);
    }


    @Override
    public void close() {
        if (realmDelegate != null) realmDelegate.close();
        if (clientDelegate != null) clientDelegate.close();
        if (clientScopeDelegate != null) clientScopeDelegate.close();
        if (roleDelegate != null) roleDelegate.close();
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
        return addRealmRole(realm, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        RoleModel role = getRoleDelegate().addRealmRole(realm, id, name);
        addedRole(role.getId(), realm.getId());
        return role;
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm) {
        String cacheKey = getRolesCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getRoleDelegate().getRealmRolesStream(realm);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getRealmRoles cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<RoleModel> model = getRoleDelegate().getRealmRolesStream(realm).collect(Collectors.toSet());
            if (model == null) return null;
            Set<String> ids = model.stream().map(RoleModel::getId).collect(Collectors.toSet());
            query = new RoleListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm roles cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model.stream();
        }
        Set<RoleModel> list = new HashSet<>();
        for (String id : query.getRoles()) {
            RoleModel role = session.roles().getRoleById(realm, id);
            if (role == null) {
                invalidations.add(cacheKey);
                return getRoleDelegate().getRealmRolesStream(realm);
            }
            list.add(role);
        }
        return list.stream();
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client) {
        String cacheKey = getRolesCacheKey(client.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(client.getId()) || listInvalidations.contains(client.getRealm().getId());
        if (queryDB) {
            return getRoleDelegate().getClientRolesStream(client);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getClientRoles cache hit: {0}", client.getClientId());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<RoleModel> model = getRoleDelegate().getClientRolesStream(client).collect(Collectors.toSet());
            if (model == null) return null;
            Set<String> ids = model.stream().map(RoleModel::getId).collect(Collectors.toSet());
            query = new RoleListQuery(loaded, cacheKey, client.getRealm(), ids, client.getClientId());
            logger.tracev("adding client roles cache miss: client {0} key {1}", client.getClientId(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model.stream();
        }
        Set<RoleModel> list = new HashSet<>();
        for (String id : query.getRoles()) {
            RoleModel role = session.roles().getRoleById(client.getRealm(), id);
            if (role == null) {
                invalidations.add(cacheKey);
                return getRoleDelegate().getClientRolesStream(client);
            }
            list.add(role);
        }
        return list.stream();
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        return getRoleDelegate().getRealmRolesStream(realm, first, max);
    }

    @Override
    public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        return getRoleDelegate().getRolesStream(realm, ids, search, first, max);
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        return getRoleDelegate().getClientRolesStream(client, first, max);
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        return getRoleDelegate().searchForClientRolesStream(client, search, first, max);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        return getRoleDelegate().searchForRolesStream(realm, search, first, max);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String name) {
        return addClientRole(client, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        RoleModel role = getRoleDelegate().addClientRole(client, id, name);
        addedRole(role.getId(), client.getId());
        return role;
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        String cacheKey = getRoleByNameCacheKey(realm.getId(), name);
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getRoleDelegate().getRealmRole(realm, name);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getRealmRole cache hit: {0}.{1}", realm.getName(), name);
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            RoleModel model = getRoleDelegate().getRealmRole(realm, name);
            if (model == null) return null;
            query = new RoleListQuery(loaded, cacheKey, realm, model.getId());
            logger.tracev("adding realm role cache miss: client {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        RoleModel role = getRoleById(realm, query.getRoles().iterator().next());
        if (role == null) {
            invalidations.add(cacheKey);
            return getRoleDelegate().getRealmRole(realm, name);
        }
        return role;
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        String cacheKey = getRoleByNameCacheKey(client.getId(), name);
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(client.getId()) || listInvalidations.contains(client.getRealm().getId());
        if (queryDB) {
            return getRoleDelegate().getClientRole(client, name);
        }

        RoleListQuery query = cache.get(cacheKey, RoleListQuery.class);
        if (query != null) {
            logger.tracev("getClientRole cache hit: {0}.{1}", client.getClientId(), name);
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            RoleModel model = getRoleDelegate().getClientRole(client, name);
            if (model == null) return null;
            query = new RoleListQuery(loaded, cacheKey, client.getRealm(), model.getId(), client.getClientId());
            logger.tracev("adding client role cache miss: client {0} key {1}", client.getClientId(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        RoleModel role = getRoleById(client.getRealm(), query.getRoles().iterator().next());
        if (role == null) {
            invalidations.add(cacheKey);
            return getRoleDelegate().getClientRole(client, name);
        }
        return role;
    }

    @Override
    public boolean removeRole(RoleModel role) {
        listInvalidations.add(role.getContainer().getId());

        invalidateRole(role.getId());
        invalidationEvents.add(RoleRemovedEvent.create(role.getId(), role.getName(), role.getContainer().getId()));
        roleRemovalInvalidations(role.getId(), role.getName(), role.getContainer().getId());

        return getRoleDelegate().removeRole(role);
    }

    @Override
    public void removeRoles(RealmModel realm) {
        getRoleDelegate().removeRoles(realm);
    }

    @Override
    public void removeRoles(ClientModel client) {
        getRoleDelegate().removeRoles(client);
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        CachedRole cached = cache.get(id, CachedRole.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            RoleModel model = getRoleDelegate().getRoleById(realm, id);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            if (model.isClientRole()) {
                cached = new CachedClientRole(loaded, model.getContainerId(), model, realm);
            } else {
                cached = new CachedRealmRole(loaded, model, realm);
            }
            cache.addRevisioned(cached, startupRevision);

        } else if (invalidations.contains(id)) {
            return getRoleDelegate().getRoleById(realm, id);
        } else if (managedRoles.containsKey(id)) {
            return managedRoles.get(id);
        }
        RoleAdapter adapter = new RoleAdapter(cached,this, realm);
        managedRoles.put(id, adapter);
        return adapter;
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        CachedGroup cached = cache.get(id, CachedGroup.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            GroupModel model = getGroupDelegate().getGroupById(realm, id);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedGroup(loaded, realm, model);
            cache.addRevisioned(cached, startupRevision);

        } else if (invalidations.contains(id)) {
            return getGroupDelegate().getGroupById(realm, id);
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
        if (toParent != null) invalidateGroup(toParent.getId(), realm.getId(), false); // Queries already invalidated
        listInvalidations.add(realm.getId());

        invalidationEvents.add(GroupMovedEvent.create(group, toParent, realm.getId()));
        getGroupDelegate().moveGroup(realm, group, toParent);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        String cacheKey = getGroupsQueryCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getGroupDelegate().getGroupsStream(realm);
        }

        GroupListQuery query = cache.get(cacheKey, GroupListQuery.class);
        if (query != null) {
            logger.tracev("getGroups cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<GroupModel> model = getGroupDelegate().getGroupsStream(realm).collect(Collectors.toList());
            if (model.isEmpty()) return Stream.empty();
            Set<String> ids = new HashSet<>();
            for (GroupModel client : model) ids.add(client.getId());
            query = new GroupListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm getGroups cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model.stream();
        }
        List<GroupModel> list = new LinkedList<>();
        for (String id : query.getGroups()) {
            GroupModel group = session.groups().getGroupById(realm, id);
            if (group == null) {
                invalidations.add(cacheKey);
                return getGroupDelegate().getGroupsStream(realm);
            }
            list.add(group);
        }

        return list.stream().sorted(GroupModel.COMPARE_BY_NAME);
    }

    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        return getGroupDelegate().getGroupsStream(realm, ids, search, first, max);
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Stream<String> ids, String search) {
        return getGroupDelegate().getGroupsCount(realm, ids, search);
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        return getGroupDelegate().getGroupsCount(realm, onlyTopGroups);
    }

    @Override
    public long getClientsCount(RealmModel realm) {
        return getClientDelegate().getClientsCount(realm);
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return getGroupDelegate().getGroupsCountByNameContaining(realm, search);
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
    	return getGroupDelegate().getGroupsByRoleStream(realm, role, firstResult, maxResults);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm) {
        String cacheKey = getTopGroupsQueryCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getGroupDelegate().getTopLevelGroupsStream(realm);
        }

        GroupListQuery query = cache.get(cacheKey, GroupListQuery.class);
        if (query != null) {
            logger.tracev("getTopLevelGroups cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<GroupModel> model = getGroupDelegate().getTopLevelGroupsStream(realm).collect(Collectors.toList());
            if (model.isEmpty()) return Stream.empty();
            Set<String> ids = new HashSet<>();
            for (GroupModel client : model) ids.add(client.getId());
            query = new GroupListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm getTopLevelGroups cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model.stream();
        }
        List<GroupModel> list = new LinkedList<>();
        for (String id : query.getGroups()) {
            GroupModel group = session.groups().getGroupById(realm, id);
            if (group == null) {
                invalidations.add(cacheKey);
                return getGroupDelegate().getTopLevelGroupsStream(realm);
            }
            list.add(group);
        }

        return list.stream().sorted(GroupModel.COMPARE_BY_NAME);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer first, Integer max) {
        String cacheKey = getTopGroupsQueryCacheKey(realm.getId() + first + max);
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId() + first + max)
                || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getGroupDelegate().getTopLevelGroupsStream(realm, first, max);
        }

        GroupListQuery query = cache.get(cacheKey, GroupListQuery.class);
        if (Objects.nonNull(query)) {
            logger.tracev("getTopLevelGroups cache hit: {0}", realm.getName());
        }

        if (Objects.isNull(query)) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<GroupModel> model = getGroupDelegate().getTopLevelGroupsStream(realm, first, max).collect(Collectors.toList());
            if (model.isEmpty()) return Stream.empty();
            Set<String> ids = new HashSet<>();
            for (GroupModel client : model) ids.add(client.getId());
            query = new GroupListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding realm getTopLevelGroups cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model.stream();
        }
        List<GroupModel> list = new LinkedList<>();
        for (String id : query.getGroups()) {
            GroupModel group = session.groups().getGroupById(realm, id);
            if (Objects.isNull(group)) {
                invalidations.add(cacheKey);
                return getGroupDelegate().getTopLevelGroupsStream(realm);
            }
            list.add(group);
        }

        return list.stream().sorted(GroupModel.COMPARE_BY_NAME);
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer first, Integer max) {
        return getGroupDelegate().searchForGroupByNameStream(realm, search, first, max);
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

        return getGroupDelegate().removeGroup(realm, group);
    }

    private GroupModel groupAdded(RealmModel realm, GroupModel group, GroupModel toParent) {
        listInvalidations.add(realm.getId());
        invalidateGroup(group.getId(), realm.getId(), true);
        if (toParent != null) invalidateGroup(toParent.getId(), realm.getId(), false); // Queries already invalidated
        String parentId = toParent == null ? null : toParent.getId();
        invalidationEvents.add(GroupAddedEvent.create(group.getId(), parentId, realm.getId()));
        return group;
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        GroupModel group = getGroupDelegate().createGroup(realm, id, name, toParent);
        return groupAdded(realm, group, toParent);
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        invalidateGroup(subGroup.getId(), realm.getId(), true);
        if (subGroup.getParentId() != null) {
            invalidateGroup(subGroup.getParentId(), realm.getId(), false); // Queries already invalidated
        }

        addGroupEventIfAbsent(GroupMovedEvent.create(subGroup, null, realm.getId()));

        getGroupDelegate().addTopLevelGroup(realm, subGroup);

    }

    private void addGroupEventIfAbsent(InvalidationEvent eventToAdd) {
        String groupId = eventToAdd.getId();

        // Check if we have existing event with bigger priority
        boolean eventAlreadyExists = invalidationEvents.stream()
                .anyMatch((InvalidationEvent event) -> (event.getId().equals(groupId)) &&
                        (event instanceof GroupAddedEvent || event instanceof GroupMovedEvent || event instanceof GroupRemovedEvent));

        if (!eventAlreadyExists) {
            invalidationEvents.add(eventToAdd);
        }
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        CachedClient cached = cache.get(id, CachedClient.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }
        boolean queryDB = invalidations.contains(id) || listInvalidations.contains(realm.getId());
        if (queryDB) {  // short-circuit if the client has been potentially invalidated
            return getClientDelegate().getClientById(realm, id);
        }
        if (cached != null) {
            logger.tracev("client by id cache hit: {0}", cached.getClientId());
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            ClientModel model = getClientDelegate().getClientById(realm, id);
            if (model == null) return null;
            ClientModel adapter = cacheClient(realm, model, loaded);
            managedApplications.put(id, adapter);
            return adapter;
        } else if (managedApplications.containsKey(id)) {
            return managedApplications.get(id);
        }
        ClientModel adapter = validateCache(realm, cached);
        managedApplications.put(id, adapter);
        return adapter;
    }

    protected ClientModel cacheClient(RealmModel realm, ClientModel delegate, Long revision) {
        if (invalidations.contains(delegate.getId())) return delegate;
        StorageId storageId = new StorageId(delegate.getId());
        CachedClient cached = null;
        ClientAdapter adapter = null;

        if (!storageId.isLocal()) {
            ComponentModel component = realm.getComponent(storageId.getProviderId());
            ClientStorageProviderModel model = new ClientStorageProviderModel(component);
            if (!model.isEnabled()) {
                return delegate;
            }
            ClientStorageProviderModel.CachePolicy policy = model.getCachePolicy();
            if (policy != null && policy == ClientStorageProviderModel.CachePolicy.NO_CACHE) {
                return delegate;
            }

            cached = new CachedClient(revision, realm, delegate);
            adapter = new ClientAdapter(realm, cached, this);

            long lifespan = model.getLifespan();
            if (lifespan > 0) {
                cache.addRevisioned(cached, startupRevision, lifespan);
            } else {
                cache.addRevisioned(cached, startupRevision);
            }
        } else {
            cached = new CachedClient(revision, realm, delegate);
            adapter = new ClientAdapter(realm, cached, this);
            cache.addRevisioned(cached, startupRevision);
        }

        return adapter;
    }


    protected ClientModel validateCache(RealmModel realm, CachedClient cached) {
        if (!realm.getId().equals(cached.getRealm())) {
            return null;
        }

        StorageId storageId = new StorageId(cached.getId());
        if (!storageId.isLocal()) {
            ComponentModel component = realm.getComponent(storageId.getProviderId());
            ClientStorageProviderModel model = new ClientStorageProviderModel(component);

            // although we do set a timeout, Infinispan has no guarantees when the user will be evicted
            // its also hard to test stuff
            if (model.shouldInvalidate(cached)) {
                registerClientInvalidation(cached.getId(), cached.getClientId(), realm.getId());
                return getClientDelegate().getClientById(realm, cached.getId());
            }
        }
        ClientAdapter adapter = new ClientAdapter(realm, cached, this);

        return adapter;
    }

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        return getClientDelegate().searchClientsByClientIdStream(realm, clientId, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return getClientDelegate().searchClientsByAttributes(realm, attributes, firstResult, maxResults);
    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        String cacheKey = getClientByClientIdCacheKey(clientId, realm.getId());
        ClientListQuery query = cache.get(cacheKey, ClientListQuery.class);
        String id = null;

        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {  // short-circuit if the client has been potentially invalidated
            return getClientDelegate().getClientByClientId(realm, clientId);
        }
        if (query != null) {
            logger.tracev("client by name cache hit: {0}", clientId);
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            ClientModel model = getClientDelegate().getClientByClientId(realm, clientId);
            if (model == null) return null;
            if (invalidations.contains(model.getId())) return model;
            id = model.getId();
            query = new ClientListQuery(loaded, cacheKey, realm, id);
            logger.tracev("adding client by name cache miss: {0}", clientId);
            cache.addRevisioned(query, startupRevision);
        } else {
            id = query.getClients().iterator().next();
            if (invalidations.contains(id)) {
                return getClientDelegate().getClientByClientId(realm, clientId);
            }
        }
        return getClientById(realm, id);
    }

    static String getClientByClientIdCacheKey(String clientId, String realmId) {
        return realmId + ".client.query.by.clientId." + clientId;
    }

    @Override
    public ClientScopeModel getClientScopeById(RealmModel realm, String id) {
        CachedClientScope cached = cache.get(id, CachedClientScope.class);
        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(id);
            ClientScopeModel model = getClientScopeDelegate().getClientScopeById(realm, id);
            if (model == null) return null;
            if (invalidations.contains(id)) return model;
            cached = new CachedClientScope(loaded, realm, model);
            cache.addRevisioned(cached, startupRevision);
        } else if (invalidations.contains(id)) {
            return getClientScopeDelegate().getClientScopeById(realm, id);
        } else if (managedClientScopes.containsKey(id)) {
            return managedClientScopes.get(id);
        }
        ClientScopeAdapter adapter = new ClientScopeAdapter(realm, cached, this);
        managedClientScopes.put(id, adapter);
        return adapter;
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream(RealmModel realm) {
        String cacheKey = getClientScopesCacheKey(realm.getId());
        boolean queryDB = invalidations.contains(cacheKey) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getClientScopeDelegate().getClientScopesStream(realm);
        }

        ClientScopeListQuery query = cache.get(cacheKey, ClientScopeListQuery.class);
        if (query != null) {
            logger.tracev("getClientScopesStream cache hit: {0}", realm.getName());
        }

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<ClientScopeModel> model = getClientScopeDelegate().getClientScopesStream(realm).collect(Collectors.toSet());
            if (model == null) return null;
            Set<String> ids = model.stream().map(ClientScopeModel::getId).collect(Collectors.toSet());
            query = new ClientScopeListQuery(loaded, cacheKey, realm, ids);
            logger.tracev("adding client scopes cache miss: realm {0} key {1}", realm.getName(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model.stream();
        }
        Set<ClientScopeModel> list = new HashSet<>();
        for (String id : query.getClientScopes()) {
            ClientScopeModel clientScope = session.clientScopes().getClientScopeById(realm, id);
            if (clientScope == null) {
                invalidations.add(cacheKey);
                return getClientScopeDelegate().getClientScopesStream(realm);
            }
            list.add(clientScope);
        }
        return list.stream();
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String name) {
        ClientScopeModel clientScope = getClientScopeDelegate().addClientScope(realm, name);
        return addedClientScope(realm, clientScope);
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String id, String name) {
        ClientScopeModel clientScope = getClientScopeDelegate().addClientScope(realm, id, name);
        return addedClientScope(realm, clientScope);
    }

    private ClientScopeModel addedClientScope(RealmModel realm, ClientScopeModel clientScope) {
        logger.tracef("Added client scope %s", clientScope.getId());

        invalidateClientScope(clientScope.getId());
        // this is needed so that a client scope that hasn't been committed isn't cached in a query
        listInvalidations.add(realm.getId());

        invalidationEvents.add(ClientScopeAddedEvent.create(clientScope.getId(), realm.getId()));
        cache.clientScopeAdded(realm.getId(), invalidations);
        return clientScope;
    }

    @Override
    public boolean removeClientScope(RealmModel realm, String id) {
        //removeClientScope can throw ModelException in case the client scope us used so invalidate only if the removal is succesful
        if (getClientScopeDelegate().removeClientScope(realm, id)) {
            listInvalidations.add(realm.getId());

            invalidateClientScope(id);
            invalidationEvents.add(ClientScopeRemovedEvent.create(id, realm.getId()));

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeClientScopes(RealmModel realm) {
        getClientScopesStream(realm).map(ClientScopeModel::getId).forEach(id -> removeClientScope(realm, id));
    }

    @Override
    public void addClientScopes(RealmModel realm, ClientModel client, Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        getClientDelegate().addClientScopes(realm, client, clientScopes, defaultScope);
        registerClientInvalidation(client.getId(), client.getId(), realm.getId());
    }

    @Override
    public void removeClientScope(RealmModel realm, ClientModel client, ClientScopeModel clientScope) {
        getClientDelegate().removeClientScope(realm, client, clientScope);
        registerClientInvalidation(client.getId(), client.getId(), realm.getId());
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScopes) {
        String cacheKey = getClientScopesCacheKey(client.getId(), defaultScopes);
        boolean queryDB = invalidations.contains(cacheKey) || invalidations.contains(client.getId()) || listInvalidations.contains(realm.getId());
        if (queryDB) {
            return getClientDelegate().getClientScopes(realm, client, defaultScopes);
        }
        ClientScopeListQuery query = cache.get(cacheKey, ClientScopeListQuery.class);

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Map<String, ClientScopeModel> model = getClientDelegate().getClientScopes(realm, client, defaultScopes);
            if (model == null) return null;
            Set<String> ids = model.values().stream().map(ClientScopeModel::getId).collect(Collectors.toSet());
            query = new ClientScopeListQuery(loaded, cacheKey, realm, client.getId(), ids);
            logger.tracev("adding assigned client scopes cache miss: client {0} key {1}", client.getClientId(), cacheKey);
            cache.addRevisioned(query, startupRevision);
            return model;
        }
        Map<String, ClientScopeModel> assignedScopes = new HashMap<>();
        for (String id : query.getClientScopes()) {
            ClientScopeModel clientScope = session.clientScopes().getClientScopeById(realm, id);
            if (clientScope == null) {
                invalidations.add(cacheKey);
                return getClientDelegate().getClientScopes(realm, client, defaultScopes);
            }
            if (clientScope.getProtocol().equals((client.getProtocol() == null) ? "openid-connect" : client.getProtocol())) {
                assignedScopes.put(clientScope.getName(), clientScope);
            }
        }
        return assignedScopes;
    }

    // Don't cache ClientInitialAccessModel for now
    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count) {
        return getRealmDelegate().createClientInitialAccessModel(realm, expiration, count);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id) {
        return getRealmDelegate().getClientInitialAccessModel(realm, id);
    }

    @Override
    public void removeClientInitialAccessModel(RealmModel realm, String id) {
        getRealmDelegate().removeClientInitialAccessModel(realm, id);
    }

    @Override
    public Stream<ClientInitialAccessModel> listClientInitialAccessStream(RealmModel realm) {
        return getRealmDelegate().listClientInitialAccessStream(realm);
    }

    @Override
    public void removeExpiredClientInitialAccess() {
        getRealmDelegate().removeExpiredClientInitialAccess();
    }

    @Override
    public void saveLocalizationText(RealmModel realm, String locale, String key, String text) {
        getRealmDelegate().saveLocalizationText(realm, locale, key, text);
        registerRealmInvalidation(realm.getId(), locale);
    }

    @Override
    public void saveLocalizationTexts(RealmModel realm, String locale, Map<String, String> localizationTexts) {
        getRealmDelegate().saveLocalizationTexts(realm, locale, localizationTexts);
        registerRealmInvalidation(realm.getId(), locale);
    }

    @Override
    public boolean updateLocalizationText(RealmModel realm, String locale, String key, String text) {
        boolean wasFound = getRealmDelegate().updateLocalizationText(realm, locale, key, text);
        if (wasFound) {
            registerRealmInvalidation(realm.getId(), locale);
        }
        return wasFound;
    }

    @Override
    public boolean deleteLocalizationTextsByLocale(RealmModel realm, String locale) {
        boolean wasDeleted = getRealmDelegate().deleteLocalizationTextsByLocale(realm, locale);
        if(wasDeleted) {
            registerRealmInvalidation(realm.getId(), locale);
        }
        return wasDeleted;
    }

    @Override
    public boolean deleteLocalizationText(RealmModel realm, String locale, String key) {
        boolean wasFound = getRealmDelegate().deleteLocalizationText(realm, locale, key);
        if (wasFound) {
            registerRealmInvalidation(realm.getId(), locale);
        }
        return wasFound;
    }

    @Override
    public String getLocalizationTextsById(RealmModel realm, String locale, String key) {
        Map<String, String> localizationTexts = getRealm(realm.getId()).getRealmLocalizationTextsByLocale(locale);
        if(localizationTexts != null) {
            return localizationTexts.get(key);
        }
        return null;
    }
}
