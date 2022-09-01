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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.context.Flag;
import org.infinispan.stream.CacheCollectors;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.device.DeviceActivityManager;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.Comparators;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.models.sessions.infinispan.util.FuturesHelper;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProvider.class);

    protected final KeycloakSession session;

    protected final Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache;
    protected final Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache;
    protected final Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache;
    protected final Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionCache;

    protected final InfinispanChangelogBasedTransaction<String, UserSessionEntity> sessionTx;
    protected final InfinispanChangelogBasedTransaction<String, UserSessionEntity> offlineSessionTx;
    protected final InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionTx;
    protected final InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> offlineClientSessionTx;

    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    protected final CrossDCLastSessionRefreshStore lastSessionRefreshStore;
    protected final CrossDCLastSessionRefreshStore offlineLastSessionRefreshStore;
    protected final PersisterLastSessionRefreshStore persisterLastSessionRefreshStore;

    protected final RemoteCacheInvoker remoteCacheInvoker;
    protected final InfinispanKeyGenerator keyGenerator;

    protected final boolean loadOfflineSessionsFromDatabase;

    public InfinispanUserSessionProvider(KeycloakSession session,
                                         RemoteCacheInvoker remoteCacheInvoker,
                                         CrossDCLastSessionRefreshStore lastSessionRefreshStore,
                                         CrossDCLastSessionRefreshStore offlineLastSessionRefreshStore,
                                         PersisterLastSessionRefreshStore persisterLastSessionRefreshStore,
                                         InfinispanKeyGenerator keyGenerator,
                                         Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache,
                                         Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache,
                                         Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache,
                                         Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionCache,
                                         boolean loadOfflineSessionsFromDatabase) {
        this.session = session;

        this.sessionCache = sessionCache;
        this.clientSessionCache = clientSessionCache;
        this.offlineSessionCache = offlineSessionCache;
        this.offlineClientSessionCache = offlineClientSessionCache;

        this.sessionTx = new InfinispanChangelogBasedTransaction<>(session, sessionCache, remoteCacheInvoker, SessionTimeouts::getUserSessionLifespanMs, SessionTimeouts::getUserSessionMaxIdleMs);
        this.offlineSessionTx = new InfinispanChangelogBasedTransaction<>(session, offlineSessionCache, remoteCacheInvoker, SessionTimeouts::getOfflineSessionLifespanMs, SessionTimeouts::getOfflineSessionMaxIdleMs);
        this.clientSessionTx = new InfinispanChangelogBasedTransaction<>(session, clientSessionCache, remoteCacheInvoker, SessionTimeouts::getClientSessionLifespanMs, SessionTimeouts::getClientSessionMaxIdleMs);
        this.offlineClientSessionTx = new InfinispanChangelogBasedTransaction<>(session, offlineClientSessionCache, remoteCacheInvoker, SessionTimeouts::getOfflineClientSessionLifespanMs, SessionTimeouts::getOfflineClientSessionMaxIdleMs);

        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        this.lastSessionRefreshStore = lastSessionRefreshStore;
        this.offlineLastSessionRefreshStore = offlineLastSessionRefreshStore;
        this.persisterLastSessionRefreshStore = persisterLastSessionRefreshStore;
        this.remoteCacheInvoker = remoteCacheInvoker;
        this.keyGenerator = keyGenerator;
        this.loadOfflineSessionsFromDatabase = loadOfflineSessionsFromDatabase;

        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
        session.getTransactionManager().enlistAfterCompletion(sessionTx);
        session.getTransactionManager().enlistAfterCompletion(offlineSessionTx);
        session.getTransactionManager().enlistAfterCompletion(clientSessionTx);
        session.getTransactionManager().enlistAfterCompletion(offlineClientSessionTx);
    }

    protected Cache<String, SessionEntityWrapper<UserSessionEntity>> getCache(boolean offline) {
        return offline ? offlineSessionCache : sessionCache;
    }

    protected InfinispanChangelogBasedTransaction<String, UserSessionEntity> getTransaction(boolean offline) {
        return offline ? offlineSessionTx : sessionTx;
    }

    protected Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> getClientSessionCache(boolean offline) {
        return offline ? offlineClientSessionCache : clientSessionCache;
    }

    protected InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> getClientSessionTransaction(boolean offline) {
        return offline ? offlineClientSessionTx : clientSessionTx;
    }

    protected CrossDCLastSessionRefreshStore getLastSessionRefreshStore() {
        return lastSessionRefreshStore;
    }

    protected CrossDCLastSessionRefreshStore getOfflineLastSessionRefreshStore() {
        return offlineLastSessionRefreshStore;
    }

    protected PersisterLastSessionRefreshStore getPersisterLastSessionRefreshStore() {
        return persisterLastSessionRefreshStore;
    }

    @Override
    public KeycloakSession getKeycloakSession() {
        return session;
    }

    @Override
    public AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        final UUID clientSessionId = keyGenerator.generateKeyUUID(session, clientSessionCache);
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity(clientSessionId);
        entity.setRealmId(realm.getId());
        entity.setTimestamp(Time.currentTime());
        entity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(entity.getTimestamp()));

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(false);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(false);
        AuthenticatedClientSessionAdapter adapter = new AuthenticatedClientSessionAdapter(session, this, entity, client, userSession, clientSessionUpdateTx, false);

        // For now, the clientSession is considered transient in case that userSession was transient
        UserSessionModel.SessionPersistenceState persistenceState = (userSession instanceof UserSessionAdapter && ((UserSessionAdapter) userSession).getPersistenceState() != null) ?
                ((UserSessionAdapter) userSession).getPersistenceState() : UserSessionModel.SessionPersistenceState.PERSISTENT;

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        clientSessionUpdateTx.addTask(clientSessionId, createClientSessionTask, entity, persistenceState);

        SessionUpdateTask registerClientSessionTask = new RegisterClientSessionTask(client.getId(), clientSessionId);
        userSessionUpdateTx.addTask(userSession.getId(), registerClientSessionTask);

        return adapter;
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        final String userSessionId = keyGenerator.generateKeyString(session, sessionCache);
        return createUserSession(userSessionId, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId, UserSessionModel.SessionPersistenceState.PERSISTENT);
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                              String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
        updateSessionEntity(entity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

        SessionUpdateTask<UserSessionEntity> createSessionTask = Tasks.addIfAbsentSync();
        sessionTx.addTask(id, createSessionTask, entity, persistenceState);

        UserSessionAdapter adapter = wrap(realm, entity, false);
        adapter.setPersistenceState(persistenceState);
        
        if (adapter != null) {
            DeviceActivityManager.attachDevice(adapter, session);
        }
        
        return adapter;
    }

    void updateSessionEntity(UserSessionEntity entity, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        entity.setRealmId(realm.getId());
        entity.setUser(user.getId());
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);
        entity.setBrokerSessionId(brokerSessionId);
        entity.setBrokerUserId(brokerUserId);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        return getUserSession(realm, id, false);
    }

    protected UserSessionAdapter getUserSession(RealmModel realm, String id, boolean offline) {

        UserSessionEntity userSessionEntityFromCache = getUserSessionEntity(realm, id, offline);
        if (userSessionEntityFromCache != null) {
            return wrap(realm, userSessionEntityFromCache, offline);
        }

        if (!offline) {
            return null;
        }

        // Try to recover from potentially lost offline-sessions by attempting to fetch and re-import
        // the offline session information from the PersistenceProvider.
        UserSessionEntity userSessionEntityFromPersistenceProvider = getUserSessionEntityFromPersistenceProvider(realm, id, offline);
        if (userSessionEntityFromPersistenceProvider != null) {
            // we successfully recovered the offline session!
            return wrap(realm, userSessionEntityFromPersistenceProvider, offline);
        }

        // no luck, the session is really not there anymore
        return null;
    }

    private UserSessionEntity getUserSessionEntityFromPersistenceProvider(RealmModel realm, String sessionId, boolean offline) {

        log.debugf("Offline user-session not found in infinispan, attempting UserSessionPersisterProvider lookup for sessionId=%s", sessionId);
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        UserSessionModel persistentUserSession = persister.loadUserSession(realm, sessionId, offline);

        if (persistentUserSession == null) {
            log.debugf("Offline user-session not found in UserSessionPersisterProvider for sessionId=%s", sessionId);
            return null;
        }

        return importUserSession(realm, offline, persistentUserSession);
    }

    private UserSessionEntity getUserSessionEntityFromCacheOrImportIfNecessary(RealmModel realm, boolean offline, UserSessionModel persistentUserSession) {

        UserSessionEntity userSessionEntity = getUserSessionEntity(realm, persistentUserSession.getId(), offline);
        if (userSessionEntity != null) {
            // user session present in cache, return existing session
            return userSessionEntity;
        }

        return importUserSession(realm, offline, persistentUserSession);
    }

    private UserSessionEntity importUserSession(RealmModel realm, boolean offline, UserSessionModel persistentUserSession) {

        String sessionId = persistentUserSession.getId();

        log.debugf("Attempting to import user-session for sessionId=%s offline=%s", sessionId, offline);
        session.sessions().importUserSessions(Collections.singleton(persistentUserSession), offline);
        log.debugf("user-session imported, trying another lookup for sessionId=%s offline=%s", sessionId, offline);

        UserSessionEntity ispnUserSessionEntity = getUserSessionEntity(realm, sessionId, offline);

        if (ispnUserSessionEntity != null) {
            log.debugf("user-session found after import for sessionId=%s offline=%s", sessionId, offline);
            return ispnUserSessionEntity;
        }

        log.debugf("user-session could not be found after import for sessionId=%s offline=%s", sessionId, offline);
        return null;
    }

    private UserSessionEntity getUserSessionEntity(RealmModel realm, String id, boolean offline) {
        InfinispanChangelogBasedTransaction<String, UserSessionEntity> tx = getTransaction(offline);
        SessionEntityWrapper<UserSessionEntity> entityWrapper = tx.get(id);
        if (entityWrapper==null) return null;
        UserSessionEntity entity = entityWrapper.getEntity();
        if (!entity.getRealmId().equals(realm.getId())) return null;
        return entity;
    }

    private Stream<UserSessionModel> getUserSessionsFromPersistenceProviderStream(RealmModel realm, UserModel user, boolean offline) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.loadUserSessionsStream(realm, user, offline, 0, null)
                .map(persistentUserSession -> getUserSessionEntityFromCacheOrImportIfNecessary(realm, offline, persistentUserSession))
                .filter(Objects::nonNull)
                .map(userSessionEntity -> wrap(realm, userSessionEntity, offline));
    }


    protected Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserSessionPredicate predicate, boolean offline) {

        if (offline && loadOfflineSessionsFromDatabase) {

            // fetch the offline user-sessions from the persistence provider
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

            if (predicate.getUserId() != null) {
                UserModel user = session.users().getUserById(realm, predicate.getUserId());
                if (user != null) {
                    return persister.loadUserSessionsStream(realm, user, true, 0, null);
                }
            }

            if (predicate.getBrokerUserId() != null) {
                String[] idpAliasSessionId = predicate.getBrokerUserId().split("\\.");

                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.IDP_ALIAS, idpAliasSessionId[0]);
                attributes.put(UserModel.IDP_USER_ID, idpAliasSessionId[1]);

                UserProvider userProvider = session.getProvider(UserProvider.class);
                UserModel userModel = userProvider.searchForUserStream(realm, attributes, 0, null).findFirst().orElse(null);
                return userModel != null ?
                        persister.loadUserSessionsStream(realm, userModel, true, 0, null) :
                        Stream.empty();
            }

            if (predicate.getBrokerSessionId() != null) {
                // TODO add support for offline user-session lookup by brokerSessionId
                // currently it is not possible to access the brokerSessionId in offline user-session in a database agnostic way
                throw new ModelException("Dynamic database lookup for offline user-sessions by broker session ID is currently only supported for preloaded sessions. " +
                        "Set preloadOfflineSessionsFromDatabase option to \"true\" in " + UserSessionSpi.NAME + " SPI in "
                        + InfinispanUserSessionProviderFactory.PROVIDER_ID + " provider to enable the lookup.");
            }
        }

        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        cache = CacheDecorators.skipCacheLoaders(cache);

        // return a stream that 'wraps' the infinispan cache stream so that the cache stream's elements are read one by one
        // and then mapped locally to avoid serialization issues when trying to manipulate the cache stream directly.
        return StreamSupport.stream(cache.entrySet().stream().filter(predicate).spliterator(), false)
                .map(Mappers.userSessionEntity())
                .map(entity -> this.wrap(realm, entity, offline));
    }

    @Override
    public AuthenticatedClientSessionAdapter getClientSession(UserSessionModel userSession, ClientModel client, String clientSessionId, boolean offline) {
        return getClientSession(userSession, client, clientSessionId == null ? null : UUID.fromString(clientSessionId), offline);
    }

    @Override
    public AuthenticatedClientSessionAdapter getClientSession(UserSessionModel userSession, ClientModel client, UUID clientSessionId, boolean offline) {
        AuthenticatedClientSessionEntity entity = getClientSessionEntity(clientSessionId, offline);
        return wrap(userSession, client, entity, offline);
    }

    private AuthenticatedClientSessionEntity getClientSessionEntity(UUID id, boolean offline) {
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> tx = getClientSessionTransaction(offline);
        SessionEntityWrapper<AuthenticatedClientSessionEntity> entityWrapper = tx.get(id);
        return entityWrapper == null ? null : entityWrapper.getEntity();
    }


    @Override
    public Stream<UserSessionModel> getUserSessionsStream(final RealmModel realm, UserModel user) {
        return getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).user(user.getId()), false);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        return getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).brokerUserId(brokerUserId), false);
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        return this.getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).brokerSessionId(brokerSessionId), false)
                .findFirst().orElse(null);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client) {
        return getUserSessionsStream(realm, client, -1, -1);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults) {
        return getUserSessionsStream(realm, client, firstResult, maxResults, false);
    }

    protected Stream<UserSessionModel> getUserSessionsStream(final RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults, final boolean offline) {

        if (offline && loadOfflineSessionsFromDatabase) {
            // fetch the actual offline user session count from the database
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            return persister.loadUserSessionsStream(realm, client, true, firstResult, maxResults);
        }

        final String clientUuid = client.getId();
        UserSessionPredicate predicate = UserSessionPredicate.create(realm.getId()).client(clientUuid);

        return getUserSessionModels(realm, firstResult, maxResults, offline, predicate);
    }

    protected Stream<UserSessionModel> getUserSessionModels(RealmModel realm, Integer firstResult, Integer maxResults, boolean offline, UserSessionPredicate predicate) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        cache = CacheDecorators.skipCacheLoaders(cache);

        // return a stream that 'wraps' the infinispan cache stream so that the cache stream's elements are read one by one
        // and then filtered/mapped locally to avoid serialization issues when trying to manipulate the cache stream directly.
        Stream<UserSessionEntity> stream = StreamSupport.stream(cache.entrySet().stream().filter(predicate).spliterator(), false)
                .map(Mappers.userSessionEntity())
                .sorted(Comparators.userSessionLastSessionRefresh());

        return paginatedStream(stream, firstResult, maxResults).map(entity -> this.wrap(realm, entity, offline));
    }

    @Override
    public UserSessionModel getUserSessionWithPredicate(RealmModel realm, String id, boolean offline, Predicate<UserSessionModel> predicate) {
        UserSessionModel userSession = getUserSession(realm, id, offline);
        if (userSession == null) {
            return null;
        }

        // We have userSession, which passes predicate. No need for remote lookup.
        if (predicate.test(userSession)) {
            log.debugf("getUserSessionWithPredicate(%s): found in local cache", id);
            return userSession;
        }

        // Try lookup userSession from remoteCache
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);

        if (remoteCache != null) {
            SessionEntityWrapper<UserSessionEntity> remoteSessionEntityWrapper = (SessionEntityWrapper<UserSessionEntity>) remoteCache.get(id);
            if (remoteSessionEntityWrapper != null) {
                UserSessionEntity remoteSessionEntity = remoteSessionEntityWrapper.getEntity();
                log.debugf("getUserSessionWithPredicate(%s): remote cache contains session entity %s", id, remoteSessionEntity);

                UserSessionModel remoteSessionAdapter = wrap(realm, remoteSessionEntity, offline);
                if (predicate.test(remoteSessionAdapter)) {

                    InfinispanChangelogBasedTransaction<String, UserSessionEntity> tx = getTransaction(offline);

                    // Remote entity contains our predicate. Update local cache with the remote entity
                    SessionEntityWrapper<UserSessionEntity> sessionWrapper = remoteSessionEntity.mergeRemoteEntityWithLocalEntity(tx.get(id));

                    // Replace entity just in ispn cache. Skip remoteStore
                    cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD, Flag.IGNORE_RETURN_VALUES)
                            .replace(id, sessionWrapper);

                    tx.reloadEntityInCurrentTransaction(realm, id, sessionWrapper);

                    // Recursion. We should have it locally now
                    return getUserSessionWithPredicate(realm, id, offline, predicate);
                } else {
                    log.debugf("getUserSessionWithPredicate(%s): found, but predicate doesn't pass", id);

                    return null;
                }
            } else {
                log.debugf("getUserSessionWithPredicate(%s): not found", id);

                // Session not available on remoteCache. Was already removed there. So removing locally too.
                // TODO: Can be optimized to skip calling remoteCache.remove
                removeUserSession(realm, userSession);

                return null;
            }
        } else {

            log.debugf("getUserSessionWithPredicate(%s): remote cache not available", id);

            return null;
        }
    }


    @Override
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, false);
    }

    @Override
    public Map<String, Long> getActiveClientSessionStats(RealmModel realm, boolean offline) {

        if (offline && loadOfflineSessionsFromDatabase) {
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            return persister.getUserSessionsCountsByClients(realm, true);
        }

        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        cache = CacheDecorators.skipCacheLoaders(cache);
        return cache.entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()))
                .map(Mappers.authClientSessionSetMapper())
                .flatMap((Serializable & Function<Set<String>, Stream<? extends String>>)Mappers::toStream)
                .collect(
                        CacheCollectors.serializableCollector(
                                () -> Collectors.groupingBy(Function.identity(), Collectors.counting())
                        )
                );
    }

     protected long getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {

        if (offline && loadOfflineSessionsFromDatabase) {
            // fetch the actual offline user session count from the database
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            return persister.getUserSessionsCount(realm, client, true);
        }

        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        cache = CacheDecorators.skipCacheLoaders(cache);

        final String clientUuid = client.getId();

        return cache.entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()).client(clientUuid))
                .count();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        UserSessionEntity entity = getUserSessionEntity(realm, session, false);
        if (entity != null) {
            removeUserSession(entity, false);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user, false);
    }

    protected void removeUserSessions(RealmModel realm, UserModel user, boolean offline) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);

        cache = CacheDecorators.skipCacheLoaders(cache);

        Iterator<UserSessionEntity> itr = cache.entrySet().stream().filter(UserSessionPredicate.create(realm.getId()).user(user.getId())).map(Mappers.userSessionEntity()).iterator();

        while (itr.hasNext()) {
            UserSessionEntity userSessionEntity = itr.next();
            removeUserSession(userSessionEntity, offline);
        }
    }

    public void removeAllExpired() {
        // Rely on expiration of cache entries provided by infinispan. Just expire entries from persister is needed
        // TODO: Avoid iteration over all realms here (Details in the KEYCLOAK-16802)
        session.realms().getRealmsStream().forEach(this::removeExpired);

    }

    @Override
    public void removeExpired(RealmModel realm) {
        // Rely on expiration of cache entries provided by infinispan. Nothing needed here besides calling persister
        session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        // Don't send message to all DCs, just to all cluster nodes in current DC. The remoteCache will notify client listeners for removed userSessions.
        clusterEventsSenderTx.addEvent(
                RemoveUserSessionsEvent.createEvent(RemoveUserSessionsEvent.class, InfinispanUserSessionProviderFactory.REMOVE_USER_SESSIONS_EVENT, session, realm.getId(), true),
                ClusterProvider.DCNotify.LOCAL_DC_ONLY);
    }

    protected void onRemoveUserSessionsEvent(String realmId) {
        removeLocalUserSessions(realmId, false);
    }

    // public for usage in the testsuite
    public void removeLocalUserSessions(String realmId, boolean offline) {
        FuturesHelper futures = new FuturesHelper();

        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCache = CacheDecorators.localCache(cache);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = getClientSessionCache(offline);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> localClientSessionCache = CacheDecorators.localCache(clientSessionCache);

        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCacheStoreIgnore = CacheDecorators.skipCacheLoaders(localCache);

        final AtomicInteger userSessionsSize = new AtomicInteger();

        localCacheStoreIgnore
                .entrySet()
                .stream()
                .filter(SessionPredicate.create(realmId))
                .map(Mappers.userSessionEntity())
                .forEach(new Consumer<UserSessionEntity>() {

                    @Override
                    public void accept(UserSessionEntity userSessionEntity) {
                        userSessionsSize.incrementAndGet();

                        // Remove session from remoteCache too. Use removeAsync for better perf
                        Future future = localCache.removeAsync(userSessionEntity.getId());
                        futures.addTask(future);
                        userSessionEntity.getAuthenticatedClientSessions().forEach((clientUUID, clientSessionId) -> {
                            Future f = localClientSessionCache.removeAsync(clientSessionId);
                            futures.addTask(f);
                        });
                    }

                });


        futures.waitForAllToFinish();

        log.debugf("Removed %d sessions in realm %s. Offline: %b", (Object) userSessionsSize.get(), realmId, offline);
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        // Don't send message to all DCs, just to all cluster nodes in current DC. The remoteCache will notify client listeners for removed userSessions.
        clusterEventsSenderTx.addEvent(
                RealmRemovedSessionEvent.createEvent(RealmRemovedSessionEvent.class, InfinispanUserSessionProviderFactory.REALM_REMOVED_SESSION_EVENT, session, realm.getId(), true),
                ClusterProvider.DCNotify.LOCAL_DC_ONLY);

        UserSessionPersisterProvider sessionsPersister = session.getProvider(UserSessionPersisterProvider.class);
        if (sessionsPersister != null) {
            sessionsPersister.onRealmRemoved(realm);
        }
    }

    protected void onRealmRemovedEvent(String realmId) {
        removeLocalUserSessions(realmId, true);
        removeLocalUserSessions(realmId, false);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
//        clusterEventsSenderTx.addEvent(
//                ClientRemovedSessionEvent.createEvent(ClientRemovedSessionEvent.class, InfinispanUserSessionProviderFactory.CLIENT_REMOVED_SESSION_EVENT, session, realm.getId(), true),
//                ClusterProvider.DCNotify.LOCAL_DC_ONLY);
        UserSessionPersisterProvider sessionsPersister = session.getProvider(UserSessionPersisterProvider.class);
        if (sessionsPersister != null) {
            sessionsPersister.onClientRemoved(realm, client);
        }
    }

    protected void onClientRemovedEvent(String realmId, String clientUuid) {
        // Nothing for now. userSession.getAuthenticatedClientSessions() will check lazily if particular client exists and update userSession on-the-fly.
    }


    protected void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user, true);
        removeUserSessions(realm, user, false);

        UserSessionPersisterProvider persisterProvider = session.getProvider(UserSessionPersisterProvider.class);
        if (persisterProvider != null) {
            persisterProvider.onUserRemoved(realm, user);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public int getStartupTime(RealmModel realm) {
        // TODO: take realm.getNotBefore() into account?
        return session.getProvider(ClusterProvider.class).getClusterStartupTime();
    }

    protected void removeUserSession(UserSessionEntity sessionEntity, boolean offline) {
        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(offline);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(offline);
        sessionEntity.getAuthenticatedClientSessions().forEach((clientUUID, clientSessionId) -> clientSessionUpdateTx.addTask(clientSessionId, Tasks.removeSync()));
        SessionUpdateTask<UserSessionEntity> removeTask = Tasks.removeSync();
        userSessionUpdateTx.addTask(sessionEntity.getId(), removeTask);
    }

    UserSessionAdapter wrap(RealmModel realm, UserSessionEntity entity, boolean offline) {
        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(offline);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(offline);
        return entity != null ? new UserSessionAdapter(session, this, userSessionUpdateTx, clientSessionUpdateTx, realm, entity, offline) : null;
    }

    AuthenticatedClientSessionAdapter wrap(UserSessionModel userSession, ClientModel client, AuthenticatedClientSessionEntity entity, boolean offline) {
        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(offline);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(offline);
        return entity != null ? new AuthenticatedClientSessionAdapter(session,this, entity, client, userSession, clientSessionUpdateTx, offline) : null;
    }

    UserSessionEntity getUserSessionEntity(RealmModel realm, UserSessionModel userSession, boolean offline) {
        if (userSession instanceof UserSessionAdapter) {
            if (!userSession.getRealm().equals(realm)) return null;
            return ((UserSessionAdapter) userSession).getEntity();
        } else {
            return getUserSessionEntity(realm, userSession.getId(), offline);
        }
    }


    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        UserSessionAdapter offlineUserSession = importUserSession(userSession, true);

        // started and lastSessionRefresh set to current time
        int currentTime = Time.currentTime();
        offlineUserSession.getEntity().setStarted(currentTime);
        offlineUserSession.setLastSessionRefresh(currentTime);

        session.getProvider(UserSessionPersisterProvider.class).createUserSession(userSession, true);

        return offlineUserSession;
    }

    @Override
    public UserSessionAdapter getOfflineUserSession(RealmModel realm, String userSessionId) {
        return getUserSession(realm, userSessionId, true);
    }

    @Override
    public UserSessionModel getOfflineUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        return this.getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).brokerSessionId(brokerSessionId), true)
                .findFirst().orElse(null);
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        return getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).brokerUserId(brokerUserId), true);
    }

    @Override
    public void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession) {
        UserSessionEntity userSessionEntity = getUserSessionEntity(realm, userSession, true);
        if (userSessionEntity != null) {
            removeUserSession(userSessionEntity, true);
        }
        session.getProvider(UserSessionPersisterProvider.class).removeUserSession(userSession.getId(), true);
    }

    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        UserSessionAdapter userSessionAdapter = (offlineUserSession instanceof UserSessionAdapter) ? (UserSessionAdapter) offlineUserSession :
                getOfflineUserSession(offlineUserSession.getRealm(), offlineUserSession.getId());

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(true);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(true);
        AuthenticatedClientSessionAdapter offlineClientSession = importClientSession(userSessionAdapter, clientSession, userSessionUpdateTx, clientSessionUpdateTx, true);

        // update timestamp to current time
        offlineClientSession.setTimestamp(Time.currentTime());
        offlineClientSession.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(offlineClientSession.getTimestamp()));

        session.getProvider(UserSessionPersisterProvider.class).createClientSession(clientSession, true);

        return offlineClientSession;
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {

        if (loadOfflineSessionsFromDatabase) {
            return getUserSessionsFromPersistenceProviderStream(realm, user, true);
        }

        return getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).user(user.getId()), true);
    }

    @Override
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, true);
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client, Integer first, Integer max) {
        return getUserSessionsStream(realm, client, first, max, true);
    }


    @Override
    public void importUserSessions(Collection<UserSessionModel> persistentUserSessions, boolean offline) {
        if (persistentUserSessions == null || persistentUserSessions.isEmpty()) {
            return;
        }

        Map<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsById = new HashMap<>();

        Map<String, SessionEntityWrapper<UserSessionEntity>> sessionsById = persistentUserSessions.stream()
                .map((UserSessionModel persistentUserSession) -> {

                    UserSessionEntity userSessionEntityToImport = createUserSessionEntityInstance(persistentUserSession);

                    for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
                        String clientUUID = entry.getKey();
                        AuthenticatedClientSessionModel clientSession = entry.getValue();
                        AuthenticatedClientSessionEntity clientSessionToImport = createAuthenticatedClientSessionInstance(clientSession, userSessionEntityToImport.getRealmId(), offline);

                        // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
                        clientSessionToImport.setTimestamp(userSessionEntityToImport.getLastSessionRefresh());

                        clientSessionsById.put(clientSessionToImport.getId(), new SessionEntityWrapper<>(clientSessionToImport));

                        // Update userSession entity with the clientSession
                        AuthenticatedClientSessionStore clientSessions = userSessionEntityToImport.getAuthenticatedClientSessions();
                        clientSessions.put(clientUUID, clientSessionToImport.getId());
                    }

                    return userSessionEntityToImport;
                })
                .map(SessionEntityWrapper::new)
                .collect(Collectors.toMap(sessionEntityWrapper -> sessionEntityWrapper.getEntity().getId(), Function.identity()));

        // Directly put all entities to the infinispan cache
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = CacheDecorators.skipCacheLoaders(getCache(offline));

        boolean importWithExpiration = sessionsById.size() == 1;
        if (importWithExpiration) {
            importSessionsWithExpiration(sessionsById, cache,
                    offline ? SessionTimeouts::getOfflineSessionLifespanMs : SessionTimeouts::getUserSessionLifespanMs,
                    offline ? SessionTimeouts::getOfflineSessionMaxIdleMs : SessionTimeouts::getUserSessionMaxIdleMs);
        } else {
            cache.putAll(sessionsById);
        }

        // put all entities to the remoteCache (if exists)
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);
        if (remoteCache != null) {
            Map<String, SessionEntityWrapper<UserSessionEntity>> sessionsByIdForTransport = sessionsById.values().stream()
                    .map(SessionEntityWrapper::forTransport)
                    .collect(Collectors.toMap(sessionEntityWrapper -> sessionEntityWrapper.getEntity().getId(), Function.identity()));

            if (importWithExpiration) {
                importSessionsWithExpiration(sessionsByIdForTransport, remoteCache,
                        offline ? SessionTimeouts::getOfflineSessionLifespanMs : SessionTimeouts::getUserSessionLifespanMs,
                        offline ? SessionTimeouts::getOfflineSessionMaxIdleMs : SessionTimeouts::getUserSessionMaxIdleMs);
            } else {
                Retry.executeWithBackoff((int iteration) -> {

                    try {
                        remoteCache.putAll(sessionsByIdForTransport);
                    } catch (HotRodClientException re) {
                        if (log.isDebugEnabled()) {
                            log.debugf(re, "Failed to put import %d sessions to remoteCache. Iteration '%s'. Will try to retry the task",
                                    sessionsByIdForTransport.size(), iteration);
                        }

                        // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                        throw re;
                    }

                }, 10, 10);
            }
        }

        // Import client sessions
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessCache = offline ? offlineClientSessionCache : clientSessionCache;
        clientSessCache = CacheDecorators.skipCacheLoaders(clientSessCache);

        if (importWithExpiration) {
            importSessionsWithExpiration(clientSessionsById, clientSessCache,
                    offline ? SessionTimeouts::getOfflineClientSessionLifespanMs : SessionTimeouts::getClientSessionLifespanMs,
                    offline ? SessionTimeouts::getOfflineClientSessionMaxIdleMs : SessionTimeouts::getClientSessionMaxIdleMs);
        } else {
            clientSessCache.putAll(clientSessionsById);
        }

        // put all entities to the remoteCache (if exists)
        RemoteCache remoteCacheClientSessions = InfinispanUtil.getRemoteCache(clientSessCache);
        if (remoteCacheClientSessions != null) {
            Map<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> sessionsByIdForTransport = clientSessionsById.values().stream()
                    .map(SessionEntityWrapper::forTransport)
                    .collect(Collectors.toMap(sessionEntityWrapper -> sessionEntityWrapper.getEntity().getId(), Function.identity()));

            if (importWithExpiration) {
                importSessionsWithExpiration(sessionsByIdForTransport, remoteCacheClientSessions,
                        offline ? SessionTimeouts::getOfflineClientSessionLifespanMs : SessionTimeouts::getClientSessionLifespanMs,
                        offline ? SessionTimeouts::getOfflineClientSessionMaxIdleMs : SessionTimeouts::getClientSessionMaxIdleMs);
            } else {
                Retry.executeWithBackoff((int iteration) -> {

                    try {
                        remoteCacheClientSessions.putAll(sessionsByIdForTransport);
                    } catch (HotRodClientException re) {
                        if (log.isDebugEnabled()) {
                            log.debugf(re, "Failed to put import %d client sessions to remoteCache. Iteration '%s'. Will try to retry the task",
                                    sessionsByIdForTransport.size(), iteration);
                        }

                        // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                        throw re;
                    }

                }, 10, 10);
            }
        }
    }

    private <T extends SessionEntity> void importSessionsWithExpiration(Map<? extends Object, SessionEntityWrapper<T>> sessionsById,
                                              BasicCache cache, BiFunction<RealmModel, T, Long> lifespanMsCalculator,
                                              BiFunction<RealmModel, T, Long> maxIdleTimeMsCalculator) {
        sessionsById.forEach((id, sessionEntityWrapper) -> {

            T sessionEntity = sessionEntityWrapper.getEntity();
            RealmModel currentRealm = session.realms().getRealm(sessionEntity.getRealmId());
            long lifespan = lifespanMsCalculator.apply(currentRealm, sessionEntity);
            long maxIdle = maxIdleTimeMsCalculator.apply(currentRealm, sessionEntity);

            if(lifespan != SessionTimeouts.ENTRY_EXPIRED_FLAG
                    && maxIdle != SessionTimeouts.ENTRY_EXPIRED_FLAG ) {
                if (cache instanceof RemoteCache) {
                    Retry.executeWithBackoff((int iteration) -> {

                        try {
                            cache.put(id, sessionEntityWrapper, lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
                        } catch (HotRodClientException re) {
                            if (log.isDebugEnabled()) {
                                log.debugf(re, "Failed to put import %d sessions to remoteCache. Iteration '%s'. Will try to retry the task",
                                        sessionsById.size(), iteration);
                            }

                            // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                            throw re;
                        }

                    }, 10, 10);
                } else {
                    cache.put(id, sessionEntityWrapper, lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    // Imports just userSession without it's clientSessions
    protected UserSessionAdapter importUserSession(UserSessionModel userSession, boolean offline) {
        UserSessionEntity entity = createUserSessionEntityInstance(userSession);

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(offline);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(offline);

        SessionUpdateTask<UserSessionEntity> importTask = Tasks.addIfAbsentSync();
        userSessionUpdateTx.addTask(userSession.getId(), importTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        UserSessionAdapter importedSession = wrap(userSession.getRealm(), entity, offline);

        return importedSession;
    }


    private UserSessionEntity createUserSessionEntityInstance(UserSessionModel userSession) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(userSession.getId());
        entity.setRealmId(userSession.getRealm().getId());

        entity.setAuthMethod(userSession.getAuthMethod());
        entity.setBrokerSessionId(userSession.getBrokerSessionId());
        entity.setBrokerUserId(userSession.getBrokerUserId());
        entity.setIpAddress(userSession.getIpAddress());
        entity.setNotes(userSession.getNotes() == null ? new ConcurrentHashMap<>() : userSession.getNotes());
        entity.setAuthenticatedClientSessions(new AuthenticatedClientSessionStore());
        entity.setRememberMe(userSession.isRememberMe());
        entity.setState(userSession.getState());
        if (userSession instanceof OfflineUserSessionModel) {
            // this is a hack so that UserModel doesn't have to be available when offline token is imported.
            // see related JIRA - KEYCLOAK-5350 and corresponding test
            OfflineUserSessionModel oline = (OfflineUserSessionModel)userSession;
            entity.setUser(oline.getUserId());
            // NOTE: Hack
            // We skip calling entity.setLoginUsername(userSession.getLoginUsername())

        } else {
            entity.setLoginUsername(userSession.getLoginUsername());
            entity.setUser(userSession.getUser().getId());
        }

        entity.setStarted(userSession.getStarted());
        entity.setLastSessionRefresh(userSession.getLastSessionRefresh());

        return entity;
    }


    private AuthenticatedClientSessionAdapter importClientSession(UserSessionAdapter sessionToImportInto, AuthenticatedClientSessionModel clientSession,
                                                                  InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx,
                                                                  InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx,
                                                                  boolean offline) {
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(clientSession, sessionToImportInto.getRealm().getId(), offline);
        final UUID clientSessionId = entity.getId();

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        clientSessionUpdateTx.addTask(entity.getId(), createClientSessionTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        AuthenticatedClientSessionStore clientSessions = sessionToImportInto.getEntity().getAuthenticatedClientSessions();
        clientSessions.put(clientSession.getClient().getId(), clientSessionId);

        SessionUpdateTask registerClientSessionTask = new RegisterClientSessionTask(clientSession.getClient().getId(), clientSessionId);
        userSessionUpdateTx.addTask(sessionToImportInto.getId(), registerClientSessionTask);

        return new AuthenticatedClientSessionAdapter(session,this, entity, clientSession.getClient(), sessionToImportInto, clientSessionUpdateTx, offline);
    }


    private AuthenticatedClientSessionEntity createAuthenticatedClientSessionInstance(AuthenticatedClientSessionModel clientSession, String realmId, boolean offline) {
        final UUID clientSessionId = keyGenerator.generateKeyUUID(session, getClientSessionCache(offline));
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity(clientSessionId);
        entity.setRealmId(realmId);

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes() == null ? new ConcurrentHashMap<>() : clientSession.getNotes());
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());

        return entity;
    }

    private static class RegisterClientSessionTask implements SessionUpdateTask<UserSessionEntity> {

        private final String clientUuid;
        private final UUID clientSessionId;

        public RegisterClientSessionTask(String clientUuid, UUID clientSessionId) {
            this.clientUuid = clientUuid;
            this.clientSessionId = clientSessionId;
        }

        @Override
        public void runUpdate(UserSessionEntity session) {
            AuthenticatedClientSessionStore clientSessions = session.getAuthenticatedClientSessions();
            clientSessions.put(clientUuid, clientSessionId);
        }

        @Override
        public CacheOperation getOperation(UserSessionEntity session) {
            return CacheOperation.REPLACE;
        }

        @Override
        public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
            return CrossDCMessageStatus.SYNC;
        }
    }

}
