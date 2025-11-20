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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.PersistentSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.CollectionToStreamMapper;
import org.keycloak.models.sessions.infinispan.stream.GroupAndCountCollectorSupplier;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionWrapperPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.models.sessions.infinispan.util.FuturesHelper;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.utils.StreamsUtil;

import org.infinispan.Cache;
import org.infinispan.commons.api.AsyncCache;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.stream.CacheCollectors;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider, SessionRefreshStore, ClientSessionManager {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProvider.class);

    protected final KeycloakSession session;

    protected final InfinispanChangelogBasedTransaction<String, UserSessionEntity> sessionTx;
    protected final InfinispanChangelogBasedTransaction<String, UserSessionEntity> offlineSessionTx;
    protected final InfinispanChangelogBasedTransaction<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> clientSessionTx;
    protected final InfinispanChangelogBasedTransaction<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> offlineClientSessionTx;

    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    protected final PersisterLastSessionRefreshStore persisterLastSessionRefreshStore;

    protected final SessionFunction<UserSessionEntity> offlineSessionCacheEntryLifespanAdjuster;

    protected final SessionFunction<AuthenticatedClientSessionEntity> offlineClientSessionCacheEntryLifespanAdjuster;

    public InfinispanUserSessionProvider(KeycloakSession session,
                                         PersisterLastSessionRefreshStore persisterLastSessionRefreshStore,
                                         InfinispanChangelogBasedTransaction<String, UserSessionEntity> sessionTx,
                                         InfinispanChangelogBasedTransaction<String, UserSessionEntity> offlineSessionTx,
                                         InfinispanChangelogBasedTransaction<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> clientSessionTx,
                                         InfinispanChangelogBasedTransaction<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> offlineClientSessionTx,
                                         SessionFunction<UserSessionEntity> offlineSessionCacheEntryLifespanAdjuster,
                                         SessionFunction<AuthenticatedClientSessionEntity> offlineClientSessionCacheEntryLifespanAdjuster) {
        this.session = session;

        this.sessionTx = sessionTx;
        this.offlineSessionTx = offlineSessionTx;
        this.clientSessionTx = clientSessionTx;
        this.offlineClientSessionTx = offlineClientSessionTx;

        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        this.persisterLastSessionRefreshStore = persisterLastSessionRefreshStore;
        this.offlineSessionCacheEntryLifespanAdjuster = offlineSessionCacheEntryLifespanAdjuster;
        this.offlineClientSessionCacheEntryLifespanAdjuster = offlineClientSessionCacheEntryLifespanAdjuster;

        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
    }

    protected Cache<String, SessionEntityWrapper<UserSessionEntity>> getCache(boolean offline) {
        return offline ? offlineSessionTx.getCache() : sessionTx.getCache();
    }

    protected InfinispanChangelogBasedTransaction<String, UserSessionEntity> getTransaction(boolean offline) {
        return offline ? offlineSessionTx : sessionTx;
    }

    protected Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> getClientSessionCache(boolean offline) {
        return offline ? offlineClientSessionTx.getCache() : clientSessionTx.getCache();
    }

    protected InfinispanChangelogBasedTransaction<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> getClientSessionTransaction(boolean offline) {
        return offline ? offlineClientSessionTx : clientSessionTx;
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
        final EmbeddedClientSessionKey key = new EmbeddedClientSessionKey(userSession.getId(), client.getId());
        var entity = AuthenticatedClientSessionEntity.create(realm, client, userSession);

        AuthenticatedClientSessionAdapter adapter = new AuthenticatedClientSessionAdapter(session, entity, client, userSession, this, key, false);

        // For now, the clientSession is considered transient in case that userSession was transient
        UserSessionModel.SessionPersistenceState persistenceState = userSession.getPersistenceState() != null ?
                userSession.getPersistenceState() : UserSessionModel.SessionPersistenceState.PERSISTENT;

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        clientSessionTx.addTask(key, createClientSessionTask, entity, persistenceState);
        addClientSessionToUserSession(key, false);
        return adapter;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                              String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        if (id == null) {
            id = sessionTx.generateKey();
        }

        UserSessionEntity entity = UserSessionEntity.create(id, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

        SessionUpdateTask<UserSessionEntity> createSessionTask = Tasks.addIfAbsentSync();
        sessionTx.addTask(id, createSessionTask, entity, persistenceState);

        UserSessionAdapter<?> adapter = user instanceof LightweightUserAdapter
          ? wrap(realm, entity, false, user)
          : wrap(realm, entity, false);
        adapter.setPersistenceState(persistenceState);
        return adapter;
    }


    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        return getUserSession(realm, id, false);
    }

    @Override
    public void migrate(String modelVersion) {
        // Changed encoding from JBoss Marshalling to ProtoStream.
        // Unable to read the cached data.
        if ("26.0.0".equals(modelVersion)) {
            log.debug("Clear caches to migrate to Infinispan Protostream");
            CompletionStages.join(session.getProvider(InfinispanConnectionProvider.class).migrateToProtoStream());
        } else if ("26.4.0".equals(modelVersion)) {
            log.debug("Clear caches as client session entries are now outdated and are not migrated");
            // This is a best-effort approach: Even if due to a rolling update some entries are left there, the checking of sessions and tokens does not depend on them.
            // Refreshing of tokens will still work even if the user session does not contain the list of client sessions.
            // This still keeps the user session cache to keep users logged in on a best effort basis.
            // Only the offline user sessions cache is cleared, but not the regular user sessions cache is cleared.
            // All client session caches regular and offline client sessions are cleared as usual.
            var stage = CompletionStages.aggregateCompletionStage();
            var provider = session.getProvider(InfinispanConnectionProvider.class);
            Stream.of(OFFLINE_USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME)
                    .map(s -> provider.getCache(s, false))
                    .filter(Objects::nonNull)
                    .map(AsyncCache::clearAsync)
                    .forEach(stage::dependsOn);
            CompletionStages.join(stage.freeze());
        }
    }

    protected UserSessionAdapter<InfinispanUserSessionProvider> getUserSession(RealmModel realm, String id, boolean offline) {

        UserSessionEntity userSessionEntityFromCache = getUserSessionEntity(realm, id, offline);
        if (userSessionEntityFromCache != null) {
            return wrap(realm, userSessionEntityFromCache, offline);
        }

        if (!offline) {
            return null;
        }

        // Try to recover from potentially lost offline-sessions by attempting to fetch and re-import
        // the offline session information from the PersistenceProvider.
        UserSessionEntity userSessionEntityFromPersistenceProvider = getUserSessionEntityFromPersistenceProvider(realm, id);
        if (userSessionEntityFromPersistenceProvider != null) {
            // we successfully recovered the offline session!
            return wrap(realm, userSessionEntityFromPersistenceProvider, true);
        }

        // no luck, the session is really not there anymore
        return null;
    }

    private UserSessionEntity getUserSessionEntityFromPersistenceProvider(RealmModel realm, String sessionId) {
        log.debugf("Offline user-session not found in infinispan, attempting UserSessionPersisterProvider lookup for sessionId=%s", sessionId);
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        UserSessionModel persistentUserSession = persister.loadUserSession(realm, sessionId, true);

        if (persistentUserSession == null) {
            log.debugf("Offline user-session not found in UserSessionPersisterProvider for sessionId=%s", sessionId);
            return null;
        }

        UserSessionEntity sessionEntity = importUserSession(realm, persistentUserSession);
        if (sessionEntity == null) {
            // TODO session expired, remove or ignore?
            persister.removeUserSession(sessionId, true);
        }

        return sessionEntity;
    }

    private UserSessionEntity getUserSessionEntityFromCacheOrImportIfNecessary(RealmModel realm, UserSessionModel persistentUserSession) {
        UserSessionEntity userSessionEntity = getUserSessionEntity(realm, persistentUserSession.getId(), true);
        if (userSessionEntity != null) {
            // user session present in cache, return existing session
            return userSessionEntity;
        }

        return importUserSession(realm, persistentUserSession);
    }

    private UserSessionEntity importUserSession(RealmModel realm, UserSessionModel persistentUserSession) {
        String sessionId = persistentUserSession.getId();

        log.debugf("Attempting to import user-session for sessionId=%s offline=true", sessionId);

        var userSessionEntityToImport = UserSessionEntity.createFromModel(persistentUserSession);

        long lifespan = offlineSessionCacheEntryLifespanAdjuster.apply(realm, null, userSessionEntityToImport);
        long maxIdle = SessionTimeouts.getOfflineSessionMaxIdleMs(realm, null, userSessionEntityToImport);

        if (lifespan == SessionTimeouts.ENTRY_EXPIRED_FLAG || maxIdle == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
            log.debugf("Session has expired. Do not import user-session for sessionId=%s offline=true", sessionId);
            return null;
        }

        UserSessionEntity existing = getTransaction(true)
                .importSession(realm, userSessionEntityToImport.getId(), new SessionEntityWrapper<>(userSessionEntityToImport),
                        lifespan, maxIdle);

        if (existing != null) {
            // skip import the client sessions, they should have been imported too.
            log.debugf("The user-session already imported by another transaction for sessionId=%s offline=true", sessionId);
            return existing;
        }

        // we need to import the client sessions too.
        log.debugf("Attempting to import the client-sessions for user-session with sessionId=%s offline=true", sessionId);

        var clientSessionsById = computeClientSessionsToImport(persistentUserSession, userSessionEntityToImport);
        getClientSessionTransaction(true).importSessionsConcurrently(realm, clientSessionsById, offlineClientSessionCacheEntryLifespanAdjuster, SessionTimeouts::getOfflineClientSessionMaxIdleMs);

        return userSessionEntityToImport;
    }

    private Map<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> computeClientSessionsToImport(UserSessionModel persistentUserSession, UserSessionEntity userSessionToImport) {
        Map<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsById = new HashMap<>();
        Set<String> clientSessions = userSessionToImport.getClientSessions();
        String userSessionId = userSessionToImport.getId();
        int lastSessionRefresh = userSessionToImport.getLastSessionRefresh();
        String realmId = userSessionToImport.getRealmId();
        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionModel clientSession = entry.getValue();
            AuthenticatedClientSessionEntity clientSessionToImport = createAuthenticatedClientSessionInstance(clientSession,
                    realmId, clientUUID);

            // Update timestamp to the same value as userSession.
            // LastSessionRefresh of userSession from DB will have the correct value.
            clientSessionToImport.setTimestamp(lastSessionRefresh);

            clientSessionsById.put(new EmbeddedClientSessionKey(userSessionId, clientUUID), new SessionEntityWrapper<>(clientSessionToImport));

            // Update userSession entity with the clientSession
            clientSessions.add(clientUUID);
        }
        return clientSessionsById;
    }

    private UserSessionEntity getUserSessionEntity(RealmModel realm, String id, boolean offline) {
        InfinispanChangelogBasedTransaction<String, UserSessionEntity> tx = getTransaction(offline);
        SessionEntityWrapper<UserSessionEntity> entityWrapper = tx.get(id);
        if (entityWrapper == null) {
            return null;
        }
        UserSessionEntity entity = entityWrapper.getEntity();
        if (!entity.getRealmId().equals(realm.getId())) {
            return null;
        }
        return entity;
    }

    protected Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserSessionPredicate predicate, boolean offline) {

        if (offline) {

            // fetch the offline user-sessions from the persistence provider
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

            if (predicate.getUserId() != null) {
                UserModel user = session.users().getUserById(realm, predicate.getUserId());
                if (user != null) {
                    return persister.loadUserSessionsStream(realm, user, true, 0, null);
                }
            }

            if (predicate.getBrokerUserId() != null) {
                int split = predicate.getBrokerUserId().indexOf('.');

                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.IDP_ALIAS, predicate.getBrokerUserId().substring(0, split));
                attributes.put(UserModel.IDP_USER_ID, predicate.getBrokerUserId().substring(split + 1));

                UserProvider userProvider = session.getProvider(UserProvider.class);
                UserModel userModel = userProvider.searchForUserStream(realm, attributes, 0, null).findFirst().orElse(null);
                return userModel != null ?
                        persister.loadUserSessionsStream(realm, userModel, true, 0, null) :
                        Stream.empty();
            }

            throw new ModelException("For offline sessions, only lookup by userId and brokerUserId is supported");
        }

        // return a stream that 'wraps' the infinispan cache stream so that the cache stream's elements are read one by one
        // and then mapped locally to avoid serialization issues when trying to manipulate the cache stream directly.
        return StreamSupport.stream(getCache(false).entrySet().stream().filter(predicate).map(Mappers.userSessionEntity()).spliterator(), false)
                .map(entity -> this.wrap(realm, entity, false))
                .filter(Objects::nonNull).map(Function.identity());
    }

    @Override
    public AuthenticatedClientSessionAdapter getClientSession(UserSessionModel userSession, ClientModel client, boolean offline) {
        var key = new EmbeddedClientSessionKey(userSession.getId(), client.getId());
        AuthenticatedClientSessionEntity clientSessionEntityFromCache = getClientSessionEntity(key, offline);
        if (clientSessionEntityFromCache != null) {
            return wrap(userSession, client, clientSessionEntityFromCache, key, offline);
        }

        // offline client session lookup in the persister
        if (offline) {
            log.debugf("Offline client session is not found in cache, try to load from db, %s", key);
            return getClientSessionEntityFromPersistenceProvider(userSession, client);
        }

        return null;
    }

    private AuthenticatedClientSessionAdapter getClientSessionEntityFromPersistenceProvider(UserSessionModel userSession, ClientModel client) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        AuthenticatedClientSessionModel clientSession = persister.loadClientSession(session.getContext().getRealm(), client, userSession, true);

        if (clientSession == null) {
            return null;
        }

        return importClientSession((UserSessionAdapter<?>) userSession, clientSession, getTransaction(true),
                getClientSessionTransaction(true), true);
    }

    private AuthenticatedClientSessionEntity getClientSessionEntity(EmbeddedClientSessionKey key, boolean offline) {
        var tx = getClientSessionTransaction(offline);
        SessionEntityWrapper<AuthenticatedClientSessionEntity> entityWrapper = tx.get(key);
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
        if (offline) {
            // fetch the actual offline user session count from the database
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            return persister.loadUserSessionsStream(realm, client, true, firstResult, maxResults)
                    .map(persistentUserSession -> getUserSessionEntityFromCacheOrImportIfNecessary(realm, persistentUserSession))
                    .filter(Objects::nonNull)
                    .map(userSessionEntity -> (UserSessionModel) wrap(realm, userSessionEntity, true))
                    .filter(Objects::nonNull);
        }

        UserSessionPredicate predicate = UserSessionPredicate.create(realm.getId()).client(client.getId());

        // If the sorted stream is used within a flatMap (like in SessionsResource), it will not terminate early unless wrapped with
        // StreamsUtil.prepareSortedStreamToWorkInsideOfFlatMapWithTerminalOperations causing unnecessary operations.
        return paginatedStream(StreamsUtil.prepareSortedStreamToWorkInsideOfFlatMapWithTerminalOperations(getUserSessionsStream(realm, predicate, false)
                .sorted(Comparator.comparing(UserSessionModel::getLastSessionRefresh))), firstResult, maxResults);
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

        return null;
    }


    @Override
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, false);
    }

    @Override
    public Map<String, Long> getActiveClientSessionStats(RealmModel realm, boolean offline) {

        if (offline) {
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            return persister.getUserSessionsCountsByClients(realm, true);
        }

        return getCache(false).entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()))
                .map(Mappers.authClientSessionSetMapper())
                .flatMap(CollectionToStreamMapper.getInstance())
                .collect(CacheCollectors.collector(GroupAndCountCollectorSupplier.getInstance()));
    }

    protected long getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {

        if (offline) {
            // fetch the actual offline user session count from the database
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            return persister.getUserSessionsCount(realm, client, true);
        }

        return getUserSessionsStream(realm, UserSessionPredicate.create(realm.getId()).client(client.getId()), false).count();
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
        Iterator<UserSessionEntity> itr = getCache(offline).entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()).user(user.getId()))
                .map(Mappers.userSessionEntity())
                .iterator();

        while (itr.hasNext()) {
            UserSessionEntity userSessionEntity = itr.next();
            removeUserSession(userSessionEntity, offline);
        }
    }

    public void removeAllExpired() {
        // Rely on expiration of cache entries provided by infinispan. Just expire entries from persister is needed
        // TODO: Avoid iteration over all realms here (Details in the KEYCLOAK-16802)
        UserSessionPersisterProvider provider = session.getProvider(UserSessionPersisterProvider.class);
        session.realms().getRealmsStream().forEach(provider::removeExpired);

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
                RemoveUserSessionsEvent.createEvent(RemoveUserSessionsEvent.class, InfinispanUserSessionProviderFactory.REMOVE_USER_SESSIONS_EVENT, session, realm.getId())
        );
    }

    protected void onRemoveUserSessionsEvent(String realmId) {
        removeLocalUserSessions(realmId, false);
        removeLocalUserSessions(realmId, true);
    }

    // public for usage in the testsuite
    public void removeLocalUserSessions(String realmId, boolean offline) {
        FuturesHelper futures = new FuturesHelper();

        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCache = CacheDecorators.localCache(getCache(offline));
        var localClientSessionCache = CacheDecorators.localCache(getClientSessionCache(offline));

        final AtomicInteger userSessionsSize = new AtomicInteger();

        localCache
                .entrySet()
                .stream()
                .filter(SessionWrapperPredicate.create(realmId))
                .forEach(userSessionEntity -> {
                    userSessionsSize.incrementAndGet();

                    // Remove session from remoteCache too. Use removeAsync for better perf
                    Future<?> future = localCache.removeAsync(userSessionEntity.getKey());
                    futures.addTask(future);
                    userSessionEntity.getValue().getEntity().getClientSessions().forEach(clientUUID -> {
                        Future<?> f = localClientSessionCache.removeAsync(new EmbeddedClientSessionKey(userSessionEntity.getKey(), clientUUID));
                        futures.addTask(f);
                    });
                });


        futures.waitForAllToFinish();

        log.debugf("Removed %d sessions in realm %s. Offline: %b", (Object) userSessionsSize.get(), realmId, offline);
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        // Don't send message to all DCs, just to all cluster nodes in current DC. The remoteCache will notify client listeners for removed userSessions.
        clusterEventsSenderTx.addEvent(
                RealmRemovedSessionEvent.createEvent(RealmRemovedSessionEvent.class, InfinispanUserSessionProviderFactory.REALM_REMOVED_SESSION_EVENT, session, realm.getId())
        );

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
        UserSessionPersisterProvider sessionsPersister = session.getProvider(UserSessionPersisterProvider.class);
        if (sessionsPersister != null) {
            sessionsPersister.onClientRemoved(realm, client);
        }
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
        var clientSessionUpdateTx = getClientSessionTransaction(offline);
        sessionEntity.getClientSessions().forEach(clientUUID -> clientSessionUpdateTx.addTask(new EmbeddedClientSessionKey(sessionEntity.getId(), clientUUID), Tasks.removeSync()));
        getTransaction(offline).addTask(sessionEntity.getId(), Tasks.removeSync());
    }

    UserSessionAdapter<InfinispanUserSessionProvider> wrap(RealmModel realm, UserSessionEntity entity, boolean offline, UserModel user) {
        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(offline);
        var clientSessionUpdateTx = getClientSessionTransaction(offline);

        if (entity == null) {
            return null;
        }

        return new UserSessionAdapter<>(session, user, this, userSessionUpdateTx, clientSessionUpdateTx, realm, entity, offline);
    }

    UserSessionAdapter<InfinispanUserSessionProvider> wrap(RealmModel realm, UserSessionEntity entity, boolean offline) {
        if (Profile.isFeatureEnabled(Feature.TRANSIENT_USERS) && entity.getNotes().containsKey(SESSION_NOTE_LIGHTWEIGHT_USER)) {
            LightweightUserAdapter lua = LightweightUserAdapter.fromString(session, realm, entity.getNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            final UserSessionAdapter<InfinispanUserSessionProvider> us = wrap(realm, entity, offline, lua);
            lua.setUpdateHandler(lua1 -> {
                if (lua == lua1) {  // Ensure there is no conflicting user model, only the latest lightweight user can be used
                    us.setNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                }
            });
            return us;
        }

        UserModel user = session.users().getUserById(realm, entity.getUser());

        if (user == null) {
            // remove orphaned user session from the cache and from persister if the session is offline; also removes associated client sessions
            removeUserSession(entity, offline);
            if (offline) {
                session.getProvider(UserSessionPersisterProvider.class).removeUserSession(entity.getId(), true);
            }
            return null;
        }

        return wrap(realm, entity, offline, user);
    }

    AuthenticatedClientSessionAdapter wrap(UserSessionModel userSession, ClientModel client, AuthenticatedClientSessionEntity entity, EmbeddedClientSessionKey key, boolean offline) {
        return entity != null ? new AuthenticatedClientSessionAdapter(session, entity, client, userSession, this, key, offline) : null;
    }

    UserSessionEntity getUserSessionEntity(RealmModel realm, UserSessionModel userSession, boolean offline) {
        if (userSession instanceof UserSessionAdapter<?> usa) {
            if (!usa.getRealm().equals(realm)) {
                return null;
            }
            return usa.getEntity();
        } else {
            return getUserSessionEntity(realm, userSession.getId(), offline);
        }
    }


    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        UserSessionAdapter<?> offlineUserSession = importUserSession(userSession);

        // started and lastSessionRefresh set to current time
        int currentTime = Time.currentTime();
        offlineUserSession.getEntity().setStarted(currentTime);
        offlineUserSession.getEntity().setLastSessionRefresh(currentTime);

        session.getProvider(UserSessionPersisterProvider.class).createUserSession(userSession, true);

        return offlineUserSession;
    }

    @Override
    public UserSessionAdapter<InfinispanUserSessionProvider> getOfflineUserSession(RealmModel realm, String userSessionId) {
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
        session.getProvider(UserSessionPersisterProvider.class).removeUserSession(userSession.getId(), true);
    }

    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        UserSessionAdapter<?> userSessionAdapter = (offlineUserSession instanceof UserSessionAdapter) ? (UserSessionAdapter<?>) offlineUserSession :
                getOfflineUserSession(offlineUserSession.getRealm(), offlineUserSession.getId());

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(true);
        var clientSessionUpdateTx = getClientSessionTransaction(true);
        AuthenticatedClientSessionAdapter offlineClientSession = importClientSession(userSessionAdapter, clientSession, userSessionUpdateTx, clientSessionUpdateTx, false);
        assert offlineClientSession != null; // no expiration checked, it is never null

        // update timestamp to current time
        offlineClientSession.setTimestamp(Time.currentTime());
        offlineClientSession.setNote(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(offlineClientSession.getTimestamp()));
        offlineClientSession.setNote(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(offlineUserSession.getStarted()));

        session.getProvider(UserSessionPersisterProvider.class).createClientSession(clientSession, true);

        return offlineClientSession;
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.loadUserSessionsStream(realm, user, true, 0, null)
                .map(persistentUserSession -> getUserSessionEntityFromCacheOrImportIfNecessary(realm, persistentUserSession))
                .filter(Objects::nonNull)
                .map(userSessionEntity -> (UserSessionModel) wrap(realm, userSessionEntity, true))
                .filter(Objects::nonNull);
    }

    @Override
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, true);
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client, Integer first, Integer max) {
        return getUserSessionsStream(realm, client, first, max, true);
    }


    @SuppressWarnings("removal")
    @Override
    @Deprecated(forRemoval = true, since = "25.0")
    public void importUserSessions(Collection<UserSessionModel> persistentUserSessions, boolean offline) {
        if (persistentUserSessions == null || persistentUserSessions.isEmpty()) {
            return;
        }

        Map<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsById = new HashMap<>();

        Map<String, SessionEntityWrapper<UserSessionEntity>> sessionsById = persistentUserSessions.stream()
                .map((UserSessionModel persistentUserSession) -> {

                    UserSessionEntity userSessionEntityToImport = UserSessionEntity.createFromModel(persistentUserSession);
                    Set<String> clientSessions = userSessionEntityToImport.getClientSessions();
                    String userSessionId = userSessionEntityToImport.getId();

                    for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
                        String clientUUID = entry.getKey();
                        AuthenticatedClientSessionModel clientSession = entry.getValue();
                        AuthenticatedClientSessionEntity clientSessionToImport = createAuthenticatedClientSessionInstance(clientSession,
                                userSessionEntityToImport.getRealmId(), clientUUID);

                        // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
                        clientSessionToImport.setTimestamp(userSessionEntityToImport.getLastSessionRefresh());

                        clientSessionsById.put(new EmbeddedClientSessionKey(userSessionId, clientUUID), new SessionEntityWrapper<>(clientSessionToImport));

                        // Update userSession entity with the clientSession
                        clientSessions.add(clientUUID);
                    }

                    return userSessionEntityToImport;
                })
                .map(SessionEntityWrapper::new)
                .collect(Collectors.toMap(sessionEntityWrapper -> sessionEntityWrapper.getEntity().getId(), Function.identity()));

        // Directly put all entities to the infinispan cache
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);

        boolean importWithExpiration = sessionsById.size() == 1;
        if (importWithExpiration) {
            importSessionsWithExpiration(sessionsById, cache,
                    offline ? offlineSessionCacheEntryLifespanAdjuster : SessionTimeouts::getUserSessionLifespanMs,
                    offline ? SessionTimeouts::getOfflineSessionMaxIdleMs : SessionTimeouts::getUserSessionMaxIdleMs);
        } else {
            Retry.executeWithBackoff((int iteration) -> cache.putAll(sessionsById), 10, 10);
        }

        // Import client sessions
        var clientSessCache = getClientSessionCache(offline);

        if (importWithExpiration) {
            importSessionsWithExpiration(clientSessionsById, clientSessCache,
                    offline ? offlineClientSessionCacheEntryLifespanAdjuster : SessionTimeouts::getClientSessionLifespanMs,
                    offline ? SessionTimeouts::getOfflineClientSessionMaxIdleMs : SessionTimeouts::getClientSessionMaxIdleMs);
        } else {
            Retry.executeWithBackoff((int iteration) -> clientSessCache.putAll(clientSessionsById), 10, 10);
        }
    }

    private <K, T extends SessionEntity> void importSessionsWithExpiration(Map<K, SessionEntityWrapper<T>> sessionsById,
                                                                        Cache<K, SessionEntityWrapper<T>> cache, SessionFunction<T> lifespanMsCalculator,
                                                                        SessionFunction<T> maxIdleTimeMsCalculator) {
        sessionsById.forEach((id, sessionEntityWrapper) -> {

            T sessionEntity = sessionEntityWrapper.getEntity();
            RealmModel currentRealm = session.realms().getRealm(sessionEntity.getRealmId());
            ClientModel client = sessionEntityWrapper.getClientIfNeeded(currentRealm);
            long lifespan = lifespanMsCalculator.apply(currentRealm, client, sessionEntity);
            long maxIdle = maxIdleTimeMsCalculator.apply(currentRealm, client, sessionEntity);

            if (lifespan != SessionTimeouts.ENTRY_EXPIRED_FLAG
                    && maxIdle != SessionTimeouts.ENTRY_EXPIRED_FLAG) {
                cache.put(id, sessionEntityWrapper, lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
            }
        });
    }

    // Imports just userSession without it's clientSessions
    protected UserSessionAdapter<InfinispanUserSessionProvider> importUserSession(UserSessionModel userSession) {
        UserSessionEntity entity = UserSessionEntity.createFromModel(userSession);

        InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx = getTransaction(true);

        SessionUpdateTask<UserSessionEntity> importTask = Tasks.addIfAbsentSync();
        userSessionUpdateTx.addTask(userSession.getId(), importTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        return wrap(userSession.getRealm(), entity, true);
    }


    private AuthenticatedClientSessionAdapter importClientSession(UserSessionAdapter<?> sessionToImportInto, AuthenticatedClientSessionModel clientSession,
                                                                  InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx,
                                                                  InfinispanChangelogBasedTransaction<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> clientSessionUpdateTx,
                                                                  boolean checkExpiration) {
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(clientSession,
                sessionToImportInto.getRealm().getId(), clientSession.getClient().getId());

        // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
        entity.setTimestamp(sessionToImportInto.getLastSessionRefresh());

        if (checkExpiration) {
            if (SessionTimeouts.getOfflineClientSessionMaxIdleMs(sessionToImportInto.getRealm(), clientSession.getClient(), entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG
                    || offlineClientSessionCacheEntryLifespanAdjuster.apply(sessionToImportInto.getRealm(), clientSession.getClient(), entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
                return null;
            }
        }

        String clientUUID = clientSession.getClient().getId();
        String userSessionId = sessionToImportInto.getId();


        var key = new EmbeddedClientSessionKey(userSessionId, clientUUID);
        clientSessionUpdateTx.addTask(key, Tasks.addIfAbsentSync(), entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        sessionToImportInto.getEntity().getClientSessions().add(clientUUID);

        userSessionUpdateTx.addTask(sessionToImportInto.getId(), new RegisterClientSessionTask(clientUUID));

        return new AuthenticatedClientSessionAdapter(session, entity, clientSession.getClient(), sessionToImportInto, this, key, true);
    }


    private AuthenticatedClientSessionEntity createAuthenticatedClientSessionInstance(AuthenticatedClientSessionModel clientSession,
                                                                                      String realmId, String clientId) {
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();
        entity.setRealmId(realmId);
        entity.setClientId(clientId);
        entity.setUserSessionId(clientSession.getUserSession().getId());
        entity.setUserId(clientSession.getUserSession().getId());

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes() == null ? new ConcurrentHashMap<>() : clientSession.getNotes());
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());

        return entity;
    }

    @Override
    public void addChange(EmbeddedClientSessionKey key, PersistentSessionUpdateTask<AuthenticatedClientSessionEntity> task) {
        getClientSessionTransaction(task.isOffline()).addTask(key, task);
    }

    @Override
    public void restartEntity(EmbeddedClientSessionKey key, PersistentSessionUpdateTask<AuthenticatedClientSessionEntity> task) {
        getClientSessionTransaction(task.isOffline()).restartEntity(key, task);
        addClientSessionToUserSession(key, task.isOffline());
    }

    private void addClientSessionToUserSession(EmbeddedClientSessionKey cacheKey, boolean offline) {
        getTransaction(offline).addTask(cacheKey.userSessionId(), new RegisterClientSessionTask(cacheKey.clientId()));
    }

    private record RegisterClientSessionTask(String clientUuid)
            implements SessionUpdateTask<UserSessionEntity> {

        @Override
        public void runUpdate(UserSessionEntity session) {
            session.getClientSessions().add(clientUuid);
        }

        @Override
        public CacheOperation getOperation() {
            return CacheOperation.REPLACE;
        }

    }

}
