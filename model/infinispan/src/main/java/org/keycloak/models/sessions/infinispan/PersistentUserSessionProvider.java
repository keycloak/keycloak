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
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanUtil;
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
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.ClientSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.changes.UserSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.models.sessions.infinispan.util.FuturesHelper;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.UserModelDelegate;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PersistentUserSessionProvider implements UserSessionProvider, SessionRefreshStore {

    private static final Logger log = Logger.getLogger(PersistentUserSessionProvider.class);

    protected final KeycloakSession session;

    protected final Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache;
    protected final Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache;
    protected final Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache;
    protected final Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionCache;

    protected final UserSessionPersistentChangelogBasedTransaction sessionTx;
    protected final UserSessionPersistentChangelogBasedTransaction offlineSessionTx;
    protected final ClientSessionPersistentChangelogBasedTransaction clientSessionTx;
    protected final ClientSessionPersistentChangelogBasedTransaction offlineClientSessionTx;

    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    protected final CrossDCLastSessionRefreshStore lastSessionRefreshStore;
    protected final CrossDCLastSessionRefreshStore offlineLastSessionRefreshStore;
    protected final PersisterLastSessionRefreshStore persisterLastSessionRefreshStore;

    protected final RemoteCacheInvoker remoteCacheInvoker;
    protected final InfinispanKeyGenerator keyGenerator;

    protected final SessionFunction<UserSessionEntity> offlineSessionCacheEntryLifespanAdjuster;

    protected final SessionFunction<AuthenticatedClientSessionEntity> offlineClientSessionCacheEntryLifespanAdjuster;

    public PersistentUserSessionProvider(KeycloakSession session,
                                         RemoteCacheInvoker remoteCacheInvoker,
                                         CrossDCLastSessionRefreshStore lastSessionRefreshStore,
                                         CrossDCLastSessionRefreshStore offlineLastSessionRefreshStore,
                                         PersisterLastSessionRefreshStore persisterLastSessionRefreshStore,
                                         InfinispanKeyGenerator keyGenerator,
                                         Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache,
                                         Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache,
                                         Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache,
                                         Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionCache,
                                         SessionFunction<UserSessionEntity> offlineSessionCacheEntryLifespanAdjuster,
                                         SessionFunction<AuthenticatedClientSessionEntity> offlineClientSessionCacheEntryLifespanAdjuster) {
        if (!Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
            throw new IllegalStateException("Persistent user sessions are not enabled");
        }

        this.session = session;

        this.sessionCache = sessionCache;
        this.clientSessionCache = clientSessionCache;
        this.offlineSessionCache = offlineSessionCache;
        this.offlineClientSessionCache = offlineClientSessionCache;

        this.sessionTx = new UserSessionPersistentChangelogBasedTransaction(session, sessionCache, remoteCacheInvoker, SessionTimeouts::getUserSessionLifespanMs, SessionTimeouts::getUserSessionMaxIdleMs, false);
        this.offlineSessionTx = new UserSessionPersistentChangelogBasedTransaction(session, offlineSessionCache, remoteCacheInvoker, offlineSessionCacheEntryLifespanAdjuster, SessionTimeouts::getOfflineSessionMaxIdleMs, true);

        this.clientSessionTx = new ClientSessionPersistentChangelogBasedTransaction(session, clientSessionCache, remoteCacheInvoker, SessionTimeouts::getClientSessionLifespanMs, SessionTimeouts::getClientSessionMaxIdleMs, false, keyGenerator, sessionTx);
        this.offlineClientSessionTx = new ClientSessionPersistentChangelogBasedTransaction(session, offlineClientSessionCache, remoteCacheInvoker, offlineClientSessionCacheEntryLifespanAdjuster, SessionTimeouts::getOfflineClientSessionMaxIdleMs, true, keyGenerator, offlineSessionTx);

        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        this.lastSessionRefreshStore = lastSessionRefreshStore;
        this.offlineLastSessionRefreshStore = offlineLastSessionRefreshStore;
        this.persisterLastSessionRefreshStore = persisterLastSessionRefreshStore;
        this.remoteCacheInvoker = remoteCacheInvoker;
        this.keyGenerator = keyGenerator;
        this.offlineSessionCacheEntryLifespanAdjuster = offlineSessionCacheEntryLifespanAdjuster;
        this.offlineClientSessionCacheEntryLifespanAdjuster = offlineClientSessionCacheEntryLifespanAdjuster;

        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
        session.getTransactionManager().enlistAfterCompletion(sessionTx);
        session.getTransactionManager().enlistAfterCompletion(offlineSessionTx);
        session.getTransactionManager().enlistAfterCompletion(clientSessionTx);
        session.getTransactionManager().enlistAfterCompletion(offlineClientSessionTx);
    }

    protected Cache<String, SessionEntityWrapper<UserSessionEntity>> getCache(boolean offline) {
        return offline ? offlineSessionCache : sessionCache;
    }

    protected UserSessionPersistentChangelogBasedTransaction getTransaction(boolean offline) {
        return offline ? offlineSessionTx : sessionTx;
    }

    protected Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> getClientSessionCache(boolean offline) {
        return offline ? offlineClientSessionCache : clientSessionCache;
    }

    protected ClientSessionPersistentChangelogBasedTransaction getClientSessionTransaction(boolean offline) {
        return offline ? offlineClientSessionTx : clientSessionTx;
    }

    @Override
    public CrossDCLastSessionRefreshStore getLastSessionRefreshStore() {
        return lastSessionRefreshStore;
    }

    @Override
    public CrossDCLastSessionRefreshStore getOfflineLastSessionRefreshStore() {
        return offlineLastSessionRefreshStore;
    }

    @Override
    public PersisterLastSessionRefreshStore getPersisterLastSessionRefreshStore() {
        return persisterLastSessionRefreshStore;
    }

    @Override
    public KeycloakSession getKeycloakSession() {
        return session;
    }

    @Override
    public AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        final UUID clientSessionId = PersistentUserSessionProvider.createClientSessionUUID(userSession.getId(), client.getId());
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity(clientSessionId);
        entity.setRealmId(realm.getId());
        entity.setClientId(client.getId());
        entity.setUserSessionId(userSession.getId());
        entity.setTimestamp(Time.currentTime());
        entity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(entity.getTimestamp()));
        entity.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(userSession.getStarted()));
        if (userSession.isRememberMe()) {
            entity.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE, "true");
        }

        UserSessionPersistentChangelogBasedTransaction userSessionUpdateTx = getTransaction(false);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(false);
        AuthenticatedClientSessionAdapter adapter = new AuthenticatedClientSessionAdapter(session, this, entity, client, userSession, clientSessionUpdateTx, false);

        if (Profile.isFeatureEnabled(Feature.PERSISTENT_USER_SESSIONS_NO_CACHE)) {
            if (userSession.isOffline()) {
                // If this is an offline session, and the referred online session doesn't exist anymore, don't register the client session in the transaction.
                // Instead keep it transient and it will be added to the offline session only afterward. This is expected by SessionTimeoutsTest.testOfflineUserClientIdleTimeoutSmallerThanSessionOneRefresh.
                if (userSessionUpdateTx.get(realm, userSession.getId()) == null) {
                    return adapter;
                }
            }
        }

        // For now, the clientSession is considered transient in case that userSession was transient
        UserSessionModel.SessionPersistenceState persistenceState = userSession.getPersistenceState() != null ?
                userSession.getPersistenceState() : UserSessionModel.SessionPersistenceState.PERSISTENT;

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        clientSessionUpdateTx.addTask(clientSessionId, createClientSessionTask, entity, persistenceState);

        SessionUpdateTask<UserSessionEntity> registerClientSessionTask = new RegisterClientSessionTask(client.getId(), clientSessionId);
        userSessionUpdateTx.addTask(userSession.getId(), registerClientSessionTask);

        return adapter;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                              String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        if (id == null) {
            id = keyGenerator.generateKeyString(session, sessionCache);
        }

        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
        updateSessionEntity(entity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

        SessionUpdateTask<UserSessionEntity> createSessionTask = Tasks.addIfAbsentSync();
        sessionTx.addTask(id, createSessionTask, entity, persistenceState);

        UserSessionAdapter adapter = user instanceof LightweightUserAdapter
          ? wrap(realm, entity, false, user)
          : wrap(realm, entity, false);
        adapter.setPersistenceState(persistenceState);
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
        UserSessionPersistentChangelogBasedTransaction tx = getTransaction(offline);
        SessionEntityWrapper<UserSessionEntity> entityWrapper = tx.get(realm, id);
        if (entityWrapper == null) {
            return null;
        }

        UserSessionEntity entity = entityWrapper.getEntity();
        if (entity.getRealmId().equals(realm.getId())) {
            return wrap(realm, entity, offline);
        }

        return null;
    }

    private UserSessionEntity getUserSessionFromTx(RealmModel realm, boolean offline, UserSessionModel persistentUserSession) {
        SessionEntityWrapper<UserSessionEntity> userSessionEntitySessionEntityWrapper = getTransaction(offline).get(realm, persistentUserSession.getId());
        if (userSessionEntitySessionEntityWrapper != null) {
            return userSessionEntitySessionEntityWrapper.getEntity();
        }
        return null;
    }

    private UserSessionEntity getUserSessionEntity(RealmModel realm, String id, boolean offline) {
        UserSessionPersistentChangelogBasedTransaction tx = getTransaction(offline);
        SessionEntityWrapper<UserSessionEntity> entityWrapper = tx.get(realm, id);
        if (entityWrapper == null) {
            return null;
        }

        UserSessionEntity entity = entityWrapper.getEntity();
        if (!entity.getRealmId().equals(realm.getId())) {
            return null;
        }
        return entity;
    }

    private Stream<UserSessionModel> getUserSessionsFromPersistenceProviderStream(RealmModel realm, UserModel user, boolean offline) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.loadUserSessionsStream(realm, user, offline, 0, null)
                .map(persistentUserSession -> (UserSessionModel) getUserSession(realm, persistentUserSession.getId(), offline))
                .filter(Objects::nonNull);
    }


    protected Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserSessionPredicate predicate, boolean offline) {
        // fetch the offline user-sessions from the persistence provider
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        if (predicate.getUserId() != null) {
            UserModel user;
            if (LightweightUserAdapter.isLightweightUser(predicate.getUserId())) {
              user = new UserModelDelegate(null) {
                  @Override
                  public String getId() {
                      return predicate.getUserId();
                  }
              };
            } else {
              user = session.users().getUserById(realm, predicate.getUserId());
            }
            if (user != null) {
                return persister.loadUserSessionsStream(realm, user, offline, 0, null)
                        .filter(predicate.toModelPredicate())
                        .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), offline))
                        .filter(Objects::nonNull);
            } else {
                return Stream.empty();
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
                    persister.loadUserSessionsStream(realm, userModel, offline, 0, null)
                            .filter(predicate.toModelPredicate())
                            .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), offline))
                            .filter(Objects::nonNull) :
                    Stream.empty();
        }

        if (predicate.getClient() != null) {
            ClientModel client = session.clients().getClientById(realm, predicate.getClient());
            return persister.loadUserSessionsStream(realm, client, offline, 0, null)
                    .filter(predicate.toModelPredicate())
                    .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), offline))
                    .filter(Objects::nonNull);
        }

        if (predicate.getBrokerSessionId() != null && !offline) {
            // we haven't yet migrated the old offline entries, so they don't have a brokerSessionId yet
            return Stream.of(persister.loadUserSessionsStreamByBrokerSessionId(realm, predicate.getBrokerSessionId(), offline))
                    .filter(predicate.toModelPredicate())
                    .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), offline))
                    .filter(Objects::nonNull);
        }

        throw new ModelException("For offline sessions, only lookup by userId, brokerUserId and client is supported");
    }

    @Override
    public AuthenticatedClientSessionAdapter getClientSession(UserSessionModel userSession, ClientModel client, String clientSessionId, boolean offline) {
        if (clientSessionId == null) {
            return null;
        }

        UUID clientSessionUUID = UUID.fromString(clientSessionId);
        ClientSessionPersistentChangelogBasedTransaction clientTx = getClientSessionTransaction(offline);

        SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSessionEntity = clientTx.get(client.getRealm(), client, userSession, clientSessionUUID);
        if (clientSessionEntity != null) {
            return new AuthenticatedClientSessionAdapter(session, this, clientSessionEntity.getEntity(), client, userSession, clientTx, offline);
        }

        return null;
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
        // TODO: consider returning a list as it is not guaranteed to be unique, and might be active for different users
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
        UserSessionPredicate predicate = UserSessionPredicate.create(realm.getId()).client(client.getId());

        return paginatedStream(getUserSessionsStream(realm, predicate, offline)
                .sorted(Comparator.comparing(UserSessionModel::getLastSessionRefresh)), firstResult, maxResults);
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

        if (Profile.isFeatureEnabled(Feature.PERSISTENT_USER_SESSIONS_NO_CACHE)) {
            return null;
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
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.getUserSessionsCountsByClients(realm, offline);
    }

    protected long getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {
        // fetch the actual offline user session count from the database
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.getUserSessionsCount(realm, client, offline);
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
        UserSessionPredicate.create(realm.getId()).user(user.getId());
        getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).user(user.getId()), offline)
                .forEach(s -> removeUserSession(realm, s));
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

        session.getProvider(UserSessionPersisterProvider.class).removeUserSessions(realm, false);
    }

    protected void onRemoveUserSessionsEvent(String realmId) {
        removeLocalUserSessions(realmId, false);
        removeLocalUserSessions(realmId, true);
    }

    // public for usage in the testsuite
    public void removeLocalUserSessions(String realmId, boolean offline) {
        FuturesHelper futures = new FuturesHelper();

        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCache = CacheDecorators.localCache(cache);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = getClientSessionCache(offline);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> localClientSessionCache = CacheDecorators.localCache(clientSessionCache);

        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCacheStoreIgnore = CacheDecorators.skipCacheLoadersIfRemoteStoreIsEnabled(localCache);

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

    UserSessionAdapter wrap(RealmModel realm, UserSessionEntity entity, boolean offline, UserModel user) {
        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(offline);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(offline);

        if (entity == null) {
            return null;
        }

        return new UserSessionAdapter(session, user, this, userSessionUpdateTx, clientSessionUpdateTx, realm, entity, offline);
    }

    UserSessionAdapter wrap(RealmModel realm, UserSessionEntity entity, boolean offline) {
        UserModel user = null;
        if (Profile.isFeatureEnabled(Feature.TRANSIENT_USERS) && entity.getNotes().containsKey(SESSION_NOTE_LIGHTWEIGHT_USER)) {
            LightweightUserAdapter lua = LightweightUserAdapter.fromString(session, realm, entity.getNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            final UserSessionAdapter us = wrap(realm, entity, offline, lua);
            lua.setUpdateHandler(lua1 -> {
                if (lua == lua1) {  // Ensure there is no conflicting user model, only the latest lightweight user can be used
                    us.setNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                }
            });
            return us;
        }

        user = session.users().getUserById(realm, entity.getUser());

        if (user == null) {
            return null;
        }

        return wrap(realm, entity, offline, user);
    }

    UserSessionEntity getUserSessionEntity(RealmModel realm, UserSessionModel userSession, boolean offline) {
        if (userSession instanceof UserSessionAdapter) {
            if (!userSession.getRealm().equals(realm)) {
                return null;
            }
            return ((UserSessionAdapter) userSession).getEntity();
        } else {
            return getUserSessionEntity(realm, userSession.getId(), offline);
        }
    }


    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        UserSessionEntity entity = createUserSessionEntityInstance(userSession);

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(true);

        SessionUpdateTask<UserSessionEntity> importTask = Tasks.addIfAbsentSync();
        userSessionUpdateTx.addTask(userSession.getId(), importTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        UserSessionAdapter offlineUserSession = wrap(userSession.getRealm(), entity, true);

        // started and lastSessionRefresh set to current time
        int currentTime = Time.currentTime();
        offlineUserSession.getEntity().setStarted(currentTime);
        offlineUserSession.getEntity().setLastSessionRefresh(currentTime);

        return offlineUserSession;
    }

    @Override
    public UserSessionAdapter getOfflineUserSession(RealmModel realm, String userSessionId) {
        return getUserSession(realm, userSessionId, true);
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
    }

    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        UserSessionAdapter userSessionAdapter = (offlineUserSession instanceof UserSessionAdapter) ? (UserSessionAdapter) offlineUserSession :
                getOfflineUserSession(offlineUserSession.getRealm(), offlineUserSession.getId());

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(true);
        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(true);
        AuthenticatedClientSessionAdapter offlineClientSession = importClientSession(userSessionAdapter, clientSession, userSessionUpdateTx, clientSessionUpdateTx, true, false);

        // update timestamp to current time
        offlineClientSession.setTimestamp(Time.currentTime());
        offlineClientSession.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(offlineClientSession.getTimestamp()));
        offlineClientSession.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(offlineUserSession.getStarted()));

        return offlineClientSession;
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {
        return getUserSessionsFromPersistenceProviderStream(realm, user, true);
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
        persistentUserSessions.forEach(userSessionModel -> importUserSession(userSessionModel, offline));
    }

    public SessionEntityWrapper<UserSessionEntity> importUserSession(UserSessionModel persistentUserSession, boolean offline) {
        Map<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsById = new HashMap<>();

        UserSessionEntity userSessionEntityToImport = createUserSessionEntityInstance(persistentUserSession);

        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionModel clientSession = entry.getValue();
            AuthenticatedClientSessionEntity clientSessionToImport = createAuthenticatedClientSessionInstance(userSessionEntityToImport.getId(), clientSession,
                    userSessionEntityToImport.getRealmId(), clientUUID, offline);
            clientSessionToImport.setUserSessionId(userSessionEntityToImport.getId());

            // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
            clientSessionToImport.setTimestamp(userSessionEntityToImport.getLastSessionRefresh());

            clientSessionsById.put(clientSessionToImport.getId(), new SessionEntityWrapper<>(clientSessionToImport));

            // Update userSession entity with the clientSession
            AuthenticatedClientSessionStore clientSessions = userSessionEntityToImport.getAuthenticatedClientSessions();
            clientSessions.put(clientUUID, clientSessionToImport.getId());
        }

        SessionEntityWrapper<UserSessionEntity>  wrappedUserSessionEntity = new SessionEntityWrapper<>(userSessionEntityToImport);

        Map<String, SessionEntityWrapper<UserSessionEntity>> sessionsById =
                    Stream.of(wrappedUserSessionEntity).collect(Collectors.toMap(sessionEntityWrapper -> sessionEntityWrapper.getEntity().getId(), Function.identity()));

        // Directly put all entities to the infinispan cache
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = CacheDecorators.skipCacheLoadersIfRemoteStoreIsEnabled(getCache(offline));

        sessionsById = importSessionsWithExpiration(sessionsById, cache,
                offline ? offlineSessionCacheEntryLifespanAdjuster : SessionTimeouts::getUserSessionLifespanMs,
                offline ? SessionTimeouts::getOfflineSessionMaxIdleMs : SessionTimeouts::getUserSessionMaxIdleMs);

        if (sessionsById.isEmpty()) {
            return null;
        }

        // put all entities to the remoteCache (if exists)
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);
        if (remoteCache != null) {
            Map<String, SessionEntityWrapper<UserSessionEntity>> sessionsByIdForTransport = Stream.of(wrappedUserSessionEntity)
                    .map(SessionEntityWrapper::forTransport)
                    .collect(Collectors.toMap(sessionEntityWrapper -> sessionEntityWrapper.getEntity().getId(), Function.identity()));

            importSessionsWithExpiration(sessionsByIdForTransport, remoteCache,
                    offline ? offlineSessionCacheEntryLifespanAdjuster : SessionTimeouts::getUserSessionLifespanMs,
                    offline ? SessionTimeouts::getOfflineSessionMaxIdleMs : SessionTimeouts::getUserSessionMaxIdleMs);
        }

        // Import client sessions
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessCache =
                CacheDecorators.skipCacheLoadersIfRemoteStoreIsEnabled(offline ? offlineClientSessionCache : clientSessionCache);

        importSessionsWithExpiration(clientSessionsById, clientSessCache,
                offline ? offlineClientSessionCacheEntryLifespanAdjuster : SessionTimeouts::getClientSessionLifespanMs,
                offline ? SessionTimeouts::getOfflineClientSessionMaxIdleMs : SessionTimeouts::getClientSessionMaxIdleMs);

        // put all entities to the remoteCache (if exists)
        RemoteCache remoteCacheClientSessions = InfinispanUtil.getRemoteCache(clientSessCache);
        if (remoteCacheClientSessions != null) {
            Map<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> sessionsByIdForTransport = clientSessionsById.values().stream()
                    .map(SessionEntityWrapper::forTransport)
                    .collect(Collectors.toMap(sessionEntityWrapper -> sessionEntityWrapper.getEntity().getId(), Function.identity()));

            importSessionsWithExpiration(sessionsByIdForTransport, remoteCacheClientSessions,
                    offline ? offlineClientSessionCacheEntryLifespanAdjuster : SessionTimeouts::getClientSessionLifespanMs,
                    offline ? SessionTimeouts::getOfflineClientSessionMaxIdleMs : SessionTimeouts::getClientSessionMaxIdleMs);
        }

        return sessionsById.entrySet().stream().findFirst().map(Map.Entry::getValue).orElse(null);
    }

    private <T extends SessionEntity, K> Map<K, SessionEntityWrapper<T>> importSessionsWithExpiration(Map<K, SessionEntityWrapper<T>> sessionsById,
                                                                              BasicCache<K, SessionEntityWrapper<T>> cache, SessionFunction<T> lifespanMsCalculator,
                                                                              SessionFunction<T> maxIdleTimeMsCalculator) {
        return sessionsById.entrySet().stream().map(entry -> {

            T sessionEntity = entry.getValue().getEntity();
            RealmModel currentRealm = session.realms().getRealm(sessionEntity.getRealmId());
            ClientModel client = entry.getValue().getClientIfNeeded(currentRealm);
            long lifespan = lifespanMsCalculator.apply(currentRealm, client, sessionEntity);
            long maxIdle = maxIdleTimeMsCalculator.apply(currentRealm, client, sessionEntity);

            if (lifespan != SessionTimeouts.ENTRY_EXPIRED_FLAG
                    && maxIdle != SessionTimeouts.ENTRY_EXPIRED_FLAG) {
                if (cache instanceof RemoteCache) {
                    Retry.executeWithBackoff((int iteration) -> {

                        try {
                            cache.putIfAbsent(entry.getKey(), entry.getValue(), lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
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
                    cache.putIfAbsent(entry.getKey(), entry.getValue(), lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
                }
                return entry;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
            OfflineUserSessionModel oline = (OfflineUserSessionModel) userSession;
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
                                                                  boolean offline, boolean checkExpiration) {
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(sessionToImportInto.getId(), clientSession,
                sessionToImportInto.getRealm().getId(), clientSession.getClient().getId(), offline);
        entity.setUserSessionId(sessionToImportInto.getId());

        // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
        entity.setTimestamp(sessionToImportInto.getLastSessionRefresh());

        if (checkExpiration) {
            SessionFunction<AuthenticatedClientSessionEntity> lifespanChecker = offline
                    ? offlineClientSessionCacheEntryLifespanAdjuster : SessionTimeouts::getClientSessionLifespanMs;
            SessionFunction<AuthenticatedClientSessionEntity> idleTimeoutChecker = offline
                    ? SessionTimeouts::getOfflineClientSessionMaxIdleMs : SessionTimeouts::getClientSessionMaxIdleMs;
            if (idleTimeoutChecker.apply(sessionToImportInto.getRealm(), clientSession.getClient(), entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG
                    || lifespanChecker.apply(sessionToImportInto.getRealm(), clientSession.getClient(), entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
                return null;
            }
        }

        final UUID clientSessionId = entity.getId();

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        clientSessionUpdateTx.addTask(entity.getId(), createClientSessionTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        AuthenticatedClientSessionStore clientSessions = sessionToImportInto.getEntity().getAuthenticatedClientSessions();
        clientSessions.put(clientSession.getClient().getId(), clientSessionId);

        SessionUpdateTask<UserSessionEntity> registerClientSessionTask = new RegisterClientSessionTask(clientSession.getClient().getId(), clientSessionId);
        userSessionUpdateTx.addTask(sessionToImportInto.getId(), registerClientSessionTask);

        return new AuthenticatedClientSessionAdapter(session, this, entity, clientSession.getClient(), sessionToImportInto, clientSessionUpdateTx, offline);
    }


    private AuthenticatedClientSessionEntity createAuthenticatedClientSessionInstance(String userSessionId, AuthenticatedClientSessionModel clientSession,
                                                                                      String realmId, String clientId, boolean offline) {
        final UUID clientSessionId = PersistentUserSessionProvider.createClientSessionUUID(userSessionId, clientId);
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity(clientSessionId);
        entity.setRealmId(realmId);

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes() == null ? new ConcurrentHashMap<>() : clientSession.getNotes());
        entity.setClientId(clientId);
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());
        entity.setCurrentRefreshToken(clientSession.getCurrentRefreshToken());
        entity.setCurrentRefreshTokenUseCount(clientSession.getCurrentRefreshTokenUseCount());

        return entity;
    }

    public SessionEntityWrapper<UserSessionEntity> wrapPersistentEntity(RealmModel realm, boolean offline, UserSessionModel persistentUserSession) {
        UserSessionEntity userSessionEntity = createUserSessionEntityInstance(persistentUserSession);

        if (isUserSessionExpired(realm, userSessionEntity, offline)) {
            return null;
        }

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(offline);
        userSessionUpdateTx.addTask(userSessionEntity.getId(), null, userSessionEntity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx = getClientSessionTransaction(offline);

        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionEntity clientSession = createAuthenticatedClientSessionInstance(persistentUserSession.getId(), entry.getValue(),
                    userSessionEntity.getRealmId(), clientUUID, offline);
            clientSession.setUserSessionId(userSessionEntity.getId());

            // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
            // clientSession.setTimestamp(userSessionEntity.getLastSessionRefresh());

            ClientModel client = session.clients().getClientById(realm, clientSession.getClientId());
            if (isClientSessionExpired(realm, client, clientSession, offline)) {
                continue;
            }

            // Update userSession entity with the clientSession
            AuthenticatedClientSessionStore clientSessions = userSessionEntity.getAuthenticatedClientSessions();
            clientSessions.put(clientUUID, clientSession.getId());
            clientSessionUpdateTx.addTask(clientSession.getId(), null, clientSession, UserSessionModel.SessionPersistenceState.PERSISTENT);
        }

        return userSessionUpdateTx.get(userSessionEntity.getId());

    }

    private boolean isClientSessionExpired(RealmModel realm, ClientModel client, AuthenticatedClientSessionEntity entity, boolean offline) {
        SessionFunction<AuthenticatedClientSessionEntity> idleChecker = offline ? SessionTimeouts::getOfflineClientSessionMaxIdleMs : SessionTimeouts::getClientSessionMaxIdleMs;
        SessionFunction<AuthenticatedClientSessionEntity> lifetimeChecker = offline ? SessionTimeouts::getOfflineClientSessionLifespanMs : SessionTimeouts::getClientSessionLifespanMs;
        return idleChecker.apply(realm, client, entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG || lifetimeChecker.apply(realm, client, entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG;
    }

    private boolean isUserSessionExpired(RealmModel realm, UserSessionEntity entity, boolean offline) {
        SessionFunction<UserSessionEntity> idleChecker = offline ? SessionTimeouts::getOfflineSessionMaxIdleMs : SessionTimeouts::getUserSessionMaxIdleMs;
        SessionFunction<UserSessionEntity> lifetimeChecker = offline ? SessionTimeouts::getOfflineSessionLifespanMs : SessionTimeouts::getUserSessionLifespanMs;
        return idleChecker.apply(realm, null, entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG || lifetimeChecker.apply(realm, null, entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG;
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

    public static UUID createClientSessionUUID(String userSessionId, String clientId) {
        // This allows creating a UUID that is constant even if the entry is reloaded from the database
        return UUID.nameUUIDFromBytes((userSessionId + clientId).getBytes(StandardCharsets.UTF_8));
    }
}
