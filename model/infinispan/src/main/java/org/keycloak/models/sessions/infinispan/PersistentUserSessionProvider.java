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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
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
import org.keycloak.models.session.PersistentUserSessionAdapter;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.ClientSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.JpaChangesPerformer;
import org.keycloak.models.sessions.infinispan.changes.MergedUpdate;
import org.keycloak.models.sessions.infinispan.changes.PersistentSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdatesList;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.changes.UserSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.ClientSessionFilterByUser;
import org.keycloak.models.sessions.infinispan.stream.MapEntryToKeyMapper;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.RemoveKeyConsumer;
import org.keycloak.models.sessions.infinispan.stream.SessionWrapperPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.models.sessions.infinispan.util.FuturesHelper;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;

import io.reactivex.rxjava3.core.Flowable;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.AsyncCache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.util.ByRef;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.persistence.manager.PersistenceManager;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;
import static org.keycloak.models.sessions.infinispan.changes.ClientSessionPersistentChangelogBasedTransaction.createAuthenticatedClientSessionInstance;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PersistentUserSessionProvider implements UserSessionProvider, SessionRefreshStore, ClientSessionManager {

    private static final Logger log = Logger.getLogger(PersistentUserSessionProvider.class);

    protected final KeycloakSession session;

    protected final UserSessionPersistentChangelogBasedTransaction sessionTx;
    protected final ClientSessionPersistentChangelogBasedTransaction clientSessionTx;

    protected final SessionEventsSenderTransaction clusterEventsSenderTx;
    protected final UserSessionPersisterProvider userSessionPersister;

    public PersistentUserSessionProvider(KeycloakSession session,
                                         UserSessionPersistentChangelogBasedTransaction sessionTx,
                                         ClientSessionPersistentChangelogBasedTransaction clientSessionTx) {
        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
            throw new IllegalStateException("Persistent user sessions are not enabled");
        }

        this.session = session;
        this.sessionTx = sessionTx;
        this.clientSessionTx = clientSessionTx;
        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
        userSessionPersister = session.getProvider(UserSessionPersisterProvider.class);
    }

    protected Cache<String, SessionEntityWrapper<UserSessionEntity>> getCache(boolean offline) {
        return sessionTx.getCache(offline);
    }

    protected Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> getClientSessionCache(boolean offline) {
        return clientSessionTx.getCache(offline);
    }

    @Override
    public PersisterLastSessionRefreshStore getPersisterLastSessionRefreshStore() {
        throw new IllegalStateException("PersisterLastSessionRefreshStore is not supported in PersistentUserSessionProvider");
    }

    @Override
    public KeycloakSession getKeycloakSession() {
        return session;
    }

    @Override
    public AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        final EmbeddedClientSessionKey cacheKey = new EmbeddedClientSessionKey(userSession.getId(), client.getId());
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();
        entity.setRealmId(realm.getId());
        entity.setClientId(client.getId());
        entity.setUserSessionId(userSession.getId());
        entity.setUserId(userSession.getUser().getId());
        entity.setTimestamp(Time.currentTime());
        entity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(entity.getTimestamp()));
        entity.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(userSession.getStarted()));
        if (userSession.isRememberMe()) {
            entity.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE, "true");
        }

        final boolean offline = userSession.isOffline();
        AuthenticatedClientSessionAdapter adapter = new AuthenticatedClientSessionAdapter(session, entity, client,
                userSession, this, cacheKey, false);

        if (offline) {
            // If this is an offline session, and the referred online session doesn't exist anymore, don't register the client session in the transaction.
            // Instead keep it transient and it will be added to the offline session only afterward. This is expected by SessionTimeoutsTest.testOfflineUserClientIdleTimeoutSmallerThanSessionOneRefresh.
            if (sessionTx.get(realm, userSession.getId(), userSession, false) == null) {
                return adapter;
            }
        }

        // For now, the clientSession is considered transient in case that userSession was transient
        UserSessionModel.SessionPersistenceState persistenceState = userSession.getPersistenceState() != null ?
                userSession.getPersistenceState() : UserSessionModel.SessionPersistenceState.PERSISTENT;

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        clientSessionTx.addTask(cacheKey, createClientSessionTask, entity, persistenceState);
        addClientSessionToUserSession(cacheKey, offline);

        return adapter;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                              String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        if (id == null) {
            id = sessionTx.generateKey();
        }

        UserSessionEntity entity = new UserSessionEntity(id);
        updateSessionEntity(entity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

        SessionUpdateTask<UserSessionEntity> createSessionTask = Tasks.addIfAbsentSync();
        sessionTx.addTask(id, createSessionTask, entity, persistenceState);

        UserSessionAdapter<?> adapter = user instanceof LightweightUserAdapter
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
        return getUserSession(realm, id, null, false);
    }

    private UserSessionAdapter<?> getUserSession(RealmModel realm, String id, UserSessionModel userSession, boolean offline) {
        SessionEntityWrapper<UserSessionEntity> entityWrapper = sessionTx.get(realm, id, userSession, offline);
        return entityWrapper != null ? wrap(realm, entityWrapper.getEntity(), offline) : null;
    }

    private UserSessionEntity getUserSessionEntity(RealmModel realm, String id, boolean offline) {
        SessionEntityWrapper<UserSessionEntity> entityWrapper = sessionTx.get(realm, id, null, offline);
        return entityWrapper != null ? entityWrapper.getEntity() : null;
    }

    private Stream<UserSessionModel> getUserSessionsFromPersistenceProviderStream(RealmModel realm, UserModel user) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.loadUserSessionsStream(realm, user, true, 0, null)
                .map(persistentUserSession -> (UserSessionModel) getUserSession(realm, persistentUserSession.getId(), persistentUserSession, true))
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
                        .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), s, offline))
                        .filter(Objects::nonNull);
            } else {
                return Stream.empty();
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
                    persister.loadUserSessionsStream(realm, userModel, offline, 0, null)
                            .filter(predicate.toModelPredicate())
                            .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), s, offline))
                            .filter(Objects::nonNull) :
                    Stream.empty();
        }

        if (predicate.getClient() != null) {
            ClientModel client = session.clients().getClientById(realm, predicate.getClient());
            return persister.loadUserSessionsStream(realm, client, offline, 0, null)
                    .filter(predicate.toModelPredicate())
                    .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), s, offline))
                    .filter(Objects::nonNull);
        }

        if (predicate.getBrokerSessionId() != null && !offline) {
            // we haven't yet migrated the old offline entries, so they don't have a brokerSessionId yet
            return Stream.of(persister.loadUserSessionsStreamByBrokerSessionId(realm, predicate.getBrokerSessionId(), false))
                    .filter(predicate.toModelPredicate())
                    .map(s -> (UserSessionModel) getUserSession(realm, s.getId(), s, false))
                    .filter(Objects::nonNull);
        }

        throw new ModelException("For offline sessions, only lookup by userId, brokerUserId and client is supported");
    }

    @Override
    public AuthenticatedClientSessionAdapter getClientSession(UserSessionModel userSession, ClientModel client, boolean offline) {
        var key = new EmbeddedClientSessionKey(userSession.getId(), client.getId());
        SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSessionEntity = clientSessionTx.get(client.getRealm(), client, userSession, key, offline);
        if (clientSessionEntity != null) {
            return new AuthenticatedClientSessionAdapter(session, clientSessionEntity.getEntity(), client, userSession, this, key, offline);
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

        return paginatedStream(getUserSessionsStream(realm, predicate, offline), firstResult, maxResults);
    }

    @Override
    public UserSessionModel getUserSessionWithPredicate(RealmModel realm, String id, boolean offline, Predicate<UserSessionModel> predicate) {
        UserSessionModel userSession = getUserSession(realm, id, null, offline);
        if (userSession == null) {
            return null;
        }

        // We have userSession, which passes predicate. No need for remote lookup.
        if (predicate.test(userSession)) {
            log.debugf("getUserSessionWithPredicate(%s): found in local cache", id);
            return userSession;
        }

        // The logic to remove the local entry if there is no entry in the remote cache that is present in the InfinispanUserSessionProvider is removed here,
        // as with persistent sessions we will have only a subset of all sessions in memory (both locally and in the remote store).
        return null;
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
        // Send message to all DCs as each site might have different entries in the cache
        clusterEventsSenderTx.addEvent(
                RemoveUserSessionsEvent.createEvent(RemoveUserSessionsEvent.class, InfinispanUserSessionProviderFactory.REMOVE_USER_SESSIONS_EVENT, session, realm.getId())
        );

        session.getProvider(UserSessionPersisterProvider.class).removeUserSessions(realm);
    }

    protected void onRemoveUserSessionsEvent(String realmId) {
        removeLocalUserSessions(realmId, false);
        removeLocalUserSessions(realmId, true);
    }

    // public for usage in the testsuite
    public void removeLocalUserSessions(String realmId, boolean offline) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCache = CacheDecorators.localCache(getCache(offline));
        var localClientSessionCache = CacheDecorators.localCache(getClientSessionCache(offline));
        final AtomicInteger userSessionsSize = new AtomicInteger();

        removeEntriesByRealm(realmId, localCache, userSessionsSize, localClientSessionCache);
        log.debugf("Removed %d sessions in realm %s. Offline: %b", (Object) userSessionsSize.get(), realmId, offline);
    }

    private static void removeEntriesByRealm(String realmId, Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionsCache, AtomicInteger userSessionsSize, Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessions) {
        FuturesHelper futures = new FuturesHelper();

        sessionsCache
                .entrySet()
                .stream()
                .filter(SessionWrapperPredicate.create(realmId))
                .map(Mappers.userSessionEntity())
                .forEach((Consumer<UserSessionEntity>) userSessionEntity -> {
                    userSessionsSize.incrementAndGet();

                    // Remove session from remoteCache too. Use removeAsync for better perf
                    Future<SessionEntityWrapper<UserSessionEntity>> future = sessionsCache.removeAsync(userSessionEntity.getId());
                    futures.addTask(future);
                    userSessionEntity.getClientSessions().forEach(clientUUID -> {
                        Future<SessionEntityWrapper<AuthenticatedClientSessionEntity>> f = clientSessions.removeAsync(new EmbeddedClientSessionKey(userSessionEntity.getId(), clientUUID));
                        futures.addTask(f);
                    });
                });

        futures.waitForAllToFinish();
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        // Send message to all DCs, as each DC might have different entries in their site cache
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
        userSessionPersister.onUserRemoved(realm, user);
        removeCachedUserAndClientSessionForUser(realm.getId(), user.getId(), true);
        removeCachedUserAndClientSessionForUser(realm.getId(), user.getId(), false);
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
        sessionEntity.getClientSessions().forEach(clientUUID -> clientSessionTx.addTask(new EmbeddedClientSessionKey(sessionEntity.getId(), clientUUID), Tasks.removeSync(offline)));
        SessionUpdateTask<UserSessionEntity> removeTask = Tasks.removeSync(offline);
        sessionTx.addTask(sessionEntity.getId(), removeTask);
    }

    UserSessionAdapter<?> wrap(RealmModel realm, UserSessionEntity entity, boolean offline, UserModel user) {
        if (entity == null) {
            return null;
        }

        return new UserSessionAdapter<>(session, user, this, sessionTx, clientSessionTx, realm, entity, offline);
    }

    UserSessionAdapter<?> wrap(RealmModel realm, UserSessionEntity entity, boolean offline) {
        UserModel user;
        if (Profile.isFeatureEnabled(Feature.TRANSIENT_USERS) && entity.getNotes().containsKey(SESSION_NOTE_LIGHTWEIGHT_USER)) {
            LightweightUserAdapter lua = LightweightUserAdapter.fromString(session, realm, entity.getNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            final UserSessionAdapter<?> us = wrap(realm, entity, offline, lua);
            lua.setUpdateHandler(lua1 -> {
                if (lua == lua1) {  // Ensure there is no conflicting user model, only the latest lightweight user can be used
                    us.setNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                }
            });
            return us;
        }

        user = session.users().getUserById(realm, entity.getUser());

        if (user == null) {
            // mark the user session for removal when the user bound to the session can not be resolved
            removeUserSession(realm, wrap(realm, entity, offline, null));
            return null;
        }

        return wrap(realm, entity, offline, user);
    }

    UserSessionEntity getUserSessionEntity(RealmModel realm, UserSessionModel userSession, boolean offline) {
        if (userSession instanceof UserSessionAdapter<?> usa) {
            if (!userSession.getRealm().equals(realm)) {
                return null;
            }
            return usa.getEntity();
        } else {
            return getUserSessionEntity(realm, userSession.getId(), offline);
        }
    }


    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        UserSessionEntity entity = createUserSessionEntityInstance(userSession);
        entity.setOffline(true);

        SessionUpdateTask<UserSessionEntity> importTask = Tasks.addIfAbsentSync();
        sessionTx.addTask(userSession.getId(), importTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        UserSessionAdapter<?> offlineUserSession = wrap(userSession.getRealm(), entity, true);

        // started and lastSessionRefresh set to current time
        int currentTime = Time.currentTime();
        offlineUserSession.getEntity().setStarted(currentTime);
        offlineUserSession.getEntity().setLastSessionRefresh(currentTime);

        return offlineUserSession;
    }

    @Override
    public UserSessionAdapter<?> getOfflineUserSession(RealmModel realm, String userSessionId) {
        return getUserSession(realm, userSessionId, null, true);
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
        UserSessionAdapter<?> userSessionAdapter = offlineUserSession instanceof UserSessionAdapter<?> ousa ?
                ousa :
                getOfflineUserSession(offlineUserSession.getRealm(), offlineUserSession.getId());

        AuthenticatedClientSessionAdapter offlineClientSession = importOfflineClientSession(userSessionAdapter, clientSession);

        // update timestamp to current time
        offlineClientSession.setTimestamp(Time.currentTime());
        offlineClientSession.setNote(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(offlineClientSession.getTimestamp()));
        offlineClientSession.setNote(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(offlineUserSession.getStarted()));

        return offlineClientSession;
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {
        return getUserSessionsFromPersistenceProviderStream(realm, user);
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

    /**
     * Imports a {@link UserSessionModel} and its {@link AuthenticatedClientSessionModel}.
     *
     * @param persistentUserSession The {@link UserSessionModel} read from the database.
     * @param offline               {@code true} if it is an offline user session.
     * @return The {@link SessionEntityWrapper} to be used to keep track of any further session changes.
     */
    public SessionEntityWrapper<UserSessionEntity> importUserSession(UserSessionModel persistentUserSession, boolean offline) {
        UserSessionEntity userSessionEntityToImport = createUserSessionEntityInstance(persistentUserSession);
        String realmId = userSessionEntityToImport.getRealmId();
        String sessionId = userSessionEntityToImport.getId();
        String userId = userSessionEntityToImport.getUser();
        RealmModel realm = session.realms().getRealm(realmId);

        long lifespan = offline ?
                SessionTimeouts.getOfflineSessionLifespanMs(realm, null, userSessionEntityToImport) :
                SessionTimeouts.getUserSessionLifespanMs(realm, null, userSessionEntityToImport);

        long maxIdle = offline ?
                SessionTimeouts.getOfflineSessionMaxIdleMs(realm, null, userSessionEntityToImport) :
                SessionTimeouts.getUserSessionMaxIdleMs(realm, null, userSessionEntityToImport);

        if (lifespan == SessionTimeouts.ENTRY_EXPIRED_FLAG || maxIdle == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
            log.debugf("Session has expired. Do not import user-session for sessionId=%s offline=%s", sessionId, offline);
            return null;
        }

        Map<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsById = new HashMap<>();

        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionModel clientSession = entry.getValue();
            AuthenticatedClientSessionEntity clientSessionToImport = createAuthenticatedClientSessionInstance(sessionId, userId, clientSession,
                    realmId, clientUUID, offline);

            if (offline) {
                // Update timestamp to the same value as userSession. LastSessionRefresh of userSession from DB will have a correct value.
                // This is an optimization with the old code before persistent user sessions existed, and is probably valid as an offline user session is supposed to have only one client session.
                // Remove this code once this once the persistent sessions is the only way to handle sessions, and the old client sessions have been migrated to have an updated timestamp.
                clientSessionToImport.setTimestamp(userSessionEntityToImport.getLastSessionRefresh());
            }

            clientSessionsById.put(new EmbeddedClientSessionKey(persistentUserSession.getId(), clientUUID), new SessionEntityWrapper<>(clientSessionToImport));

            // Update userSession entity with the clientSession
            userSessionEntityToImport.getClientSessions().add(clientUUID);
        }

        SessionEntityWrapper<UserSessionEntity>  wrappedUserSessionEntity = new SessionEntityWrapper<>(userSessionEntityToImport);

        SessionEntityWrapper<UserSessionEntity> existingSession = sessionTx.importSession(realm, sessionId, wrappedUserSessionEntity, offline, lifespan, maxIdle);
        if (existingSession != null) {
            // skip import the client sessions, they should have been imported too.
            log.debugf("The user-session already imported by another transaction for sessionId=%s offline=%s", sessionId, offline);
            return existingSession;
        }

        // importing here when the transaction has the changelog available.
        if (!offline) {
            migrateRememberMe(persistentUserSession);
        }

        // Import client sessions
        clientSessionTx.importSessionsConcurrently(realm, clientSessionsById, offline);
        clientSessionTx.setUserSessionId(clientSessionsById.keySet(), sessionId, offline);
        return wrappedUserSessionEntity;
    }

    // new import logic has been added to PersistentSessionsChangelogBasedTransaction, no longer in use.
    @Deprecated(forRemoval = true, since = "26.4")
    public <T extends SessionEntity, K> Map<K, SessionEntityWrapper<T>> importSessionsWithExpiration(Map<K, SessionEntityWrapper<T>> sessionsById,
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

    private static UserSessionEntity createUserSessionEntityInstance(UserSessionModel userSession) {
        UserSessionEntity entity = new UserSessionEntity(userSession.getId());
        entity.setRealmId(userSession.getRealm().getId());

        entity.setAuthMethod(userSession.getAuthMethod());
        entity.setBrokerSessionId(userSession.getBrokerSessionId());
        entity.setBrokerUserId(userSession.getBrokerUserId());
        entity.setIpAddress(userSession.getIpAddress());
        entity.setNotes(userSession.getNotes() == null ? new ConcurrentHashMap<>() : userSession.getNotes());
        entity.setRememberMe(userSession.isRememberMe());
        entity.setState(userSession.getState());
        if (userSession instanceof OfflineUserSessionModel offlineUserSession) {
            // this is a hack so that UserModel doesn't have to be available when offline token is imported.
            // see related JIRA - KEYCLOAK-5350 and corresponding test
            entity.setUser(offlineUserSession.getUserId());
            // NOTE: Hack
            // We skip calling entity.setLoginUsername(userSession.getLoginUsername())

        } else {
            entity.setLoginUsername(userSession.getLoginUsername());
            entity.setUser(userSession.getUser().getId());
        }

        entity.setStarted(userSession.getStarted());
        entity.setLastSessionRefresh(userSession.getLastSessionRefresh());
        entity.setOffline(userSession.isOffline());

        return entity;
    }


    private AuthenticatedClientSessionAdapter importOfflineClientSession(UserSessionAdapter<?> sessionToImportInto,
                                                                         AuthenticatedClientSessionModel clientSession) {
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(sessionToImportInto.getId(), sessionToImportInto.getUser().getId(), clientSession,
                sessionToImportInto.getRealm().getId(), clientSession.getClient().getId(), true);

        // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
        entity.setTimestamp(sessionToImportInto.getLastSessionRefresh());

        var clientUUID = clientSession.getClient().getId();

        var key = new EmbeddedClientSessionKey(sessionToImportInto.getId(), clientUUID);
        clientSessionTx.addTask(key, Tasks.addIfAbsentSync(), entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        sessionToImportInto.getEntity().getClientSessions().add(clientUUID);
        sessionTx.registerClientSession(sessionToImportInto.getId(), clientUUID, true);

        return new AuthenticatedClientSessionAdapter(session, entity, clientSession.getClient(), sessionToImportInto, this, key, true);
    }

    public SessionEntityWrapper<UserSessionEntity> wrapPersistentEntity(RealmModel realm, boolean offline, UserSessionModel persistentUserSession) {
        UserSessionEntity userSessionEntity = createUserSessionEntityInstance(persistentUserSession);

        if (isUserSessionExpired(realm, userSessionEntity, offline)) {
            return null;
        }

        sessionTx.addTask(userSessionEntity.getId(), null, userSessionEntity, UserSessionModel.SessionPersistenceState.PERSISTENT);
        if (!offline) {
            migrateRememberMe(persistentUserSession);
        }

        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionEntity clientSession = createAuthenticatedClientSessionInstance(persistentUserSession.getId(), userSessionEntity.getUser(), entry.getValue(),
                    userSessionEntity.getRealmId(), clientUUID, offline);

            if (offline) {
                // Update timestamp to the same value as userSession. LastSessionRefresh of userSession from DB will have a correct value.
                // This is an optimization with the old code before persistent user sessions existed, and is probably valid as an offline user session is supposed to have only one client session.
                // Remove this code once this once the persistent sessions is the only way to handle sessions, and the old client sessions have been migrated to have an updated timestamp.
                clientSession.setTimestamp(userSessionEntity.getLastSessionRefresh());
            }

            ClientModel client = session.clients().getClientById(realm, clientSession.getClientId());
            if (isClientSessionExpired(realm, client, clientSession, offline)) {
                continue;
            }

            var key = new EmbeddedClientSessionKey(userSessionEntity.getId(), clientUUID);

            // Update userSession entity with the clientSession
            userSessionEntity.getClientSessions().add(key.clientId());
            clientSessionTx.addTask(key, null, clientSession, UserSessionModel.SessionPersistenceState.PERSISTENT);
        }

        return sessionTx.get(userSessionEntity.getId(), offline);

    }

    private void migrateRememberMe(UserSessionModel persistentUserSession) {
        if (persistentUserSession instanceof PersistentUserSessionAdapter pusa && pusa.requiresRememberMeMigration()) {
            final boolean rememberMe = pusa.isRememberMe();
            sessionTx.addTask(persistentUserSession.getId(), new PersistentSessionUpdateTask<>() {
                @Override
                public boolean isOffline() {
                    return false;
                }

                @Override
                public void runUpdate(UserSessionEntity entity) {
                    // update database column
                    entity.setRememberMe(rememberMe);
                }

                @Override
                public CacheOperation getOperation() {
                    return CacheOperation.REPLACE;
                }
            });
        }
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
            var stage = CompletionStages.aggregateCompletionStage();
            Stream.of(USER_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME)
                    .map(s -> {
                        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                        if (provider != null) {
                            return provider.getCache(s, false);
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(AsyncCache::clearAsync)
                    .forEach(stage::dependsOn);
            CompletionStages.join(stage.freeze());
        }
    }

    /**
     * Copy over all sessions in Infinispan to the persistent user sessions in the database.
     * This method is public so people can use it to build their custom migrations or re-import sessions when necessary
     * in a future version of Keycloak.
     */
    @Deprecated(since = "26.4", forRemoval = true)
    public void migrateNonPersistentSessionsToPersistentSessions() {
        var sessionCache = sessionTx.getCache(false);
        var clientSessionCache = clientSessionTx.getCache(false);
        JpaChangesPerformer<String, UserSessionEntity> userSessionPerformer = new JpaChangesPerformer<>(sessionCache.getName(), null);
        JpaChangesPerformer<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> clientSessionPerformer = new JpaChangesPerformer<>(clientSessionCache.getName(), null);
        AtomicInteger currentBatch = new AtomicInteger(0);
        var persistence = ComponentRegistry.componentOf(sessionCache, PersistenceManager.class);
        if (persistence != null && !persistence.getStoresAsString().isEmpty()) {
            ByRef<Throwable> ref = ByRef.create(null);
            Flowable.fromPublisher(persistence.<String, SessionEntityWrapper<UserSessionEntity>>publishEntries(true, false))
                    .blockingSubscribe(e -> processEntryFromCache(e.getValue(), userSessionPerformer, clientSessionPerformer, currentBatch), ref::set);
            if (ref.get() != null) {
                throw new RuntimeException("Unable to migrate sessions", ref.get());
            }
        } else {
            // Usually we assume sessions are stored in a persistence. To be extra safe, iterate over local sessions if no persistent is available.
            sessionCache.forEach((key, value) -> processEntryFromCache(value, userSessionPerformer, clientSessionPerformer, currentBatch));
        }
        flush(userSessionPerformer, clientSessionPerformer);
        // Clear existing sessions as the IDs of the client sessions have changed.
        sessionCache.clear();
        clientSessionCache.clear();
        // Even though offline sessions haven't been migrated, they are cleared as the IDs of the client sessions have changed. It is safe to clear them as they are already stored in the database.
        sessionTx.getCache(true).clear();
        clientSessionTx.getCache(true).clear();
        log.infof("Migrated %d user sessions total.", currentBatch.intValue());
    }

    /**
     * When calling this, ensure that the cache doesn't contain entries for user or client sessions that are already contained in the database.
     * Such entries should first be cleared from the cache before this is being called.
     * As this is assumed to run once during the upgrade to Keycloak 25, this should be safe to assume.
     */
    private void processEntryFromCache(SessionEntityWrapper<UserSessionEntity> sessionEntityWrapper, JpaChangesPerformer<String, UserSessionEntity> userSessionPerformer, JpaChangesPerformer<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> clientSessionPerformer, AtomicInteger count) {
        RealmModel realm = session.realms().getRealm(sessionEntityWrapper.getEntity().getRealmId());
        if (realm == null) {
            // ignoring old and unknown realm found in the session
            return;
        }
        var clientSessionCache = clientSessionTx.getCache(false);
        sessionEntityWrapper.getEntity().getClientSessions().forEach(clientId-> {
            var key = new EmbeddedClientSessionKey(sessionEntityWrapper.getEntity().getId(), clientId);
            SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSession = clientSessionCache.get(key);
            if (clientSession != null) {
                // TODO [pruivo] [KC27] Remove!
                // This is necessary because client sessions created by a KC version < 22 do not have clientId set within the entity.
                if (clientSession.getEntity().getClientId() == null) {
                    clientSession.getEntity().setClientId(clientId);
                }
                clientSession.getEntity().setUserSessionId(sessionEntityWrapper.getEntity().getId());
                clientSession.getEntity().setUserId(sessionEntityWrapper.getEntity().getUser());
                MergedUpdate<AuthenticatedClientSessionEntity> merged = MergedUpdate.computeUpdate(Collections.singletonList(Tasks.addIfAbsentSync()), clientSession, 1, 1);
                clientSessionPerformer.registerChange(Map.entry(key, new SessionUpdatesList<>(realm, clientSession)), merged);
            }
        });
        MergedUpdate<UserSessionEntity> merged = MergedUpdate.computeUpdate(Collections.singletonList(Tasks.addIfAbsentSync()), sessionEntityWrapper, 1, 1);
        userSessionPerformer.registerChange(Map.entry(sessionEntityWrapper.getEntity().getId(), new SessionUpdatesList<>(realm, sessionEntityWrapper)), merged);
        if (count.incrementAndGet() % 100 == 0) {
            flush(userSessionPerformer, clientSessionPerformer);
        }
        if (count.intValue() % 1000 == 0) {
            log.infof("Migrated %d user sessions total, continuing...", count.intValue());
        }
    }

    private <E extends SessionEntity, K> void flush(JpaChangesPerformer<K, E> userSessionsPerformer, JpaChangesPerformer<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> clientSessionPerformer) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(),
                s -> {
                    userSessionsPerformer.write(s);
                    clientSessionPerformer.write(s);
                });
        userSessionsPerformer.clear();
        clientSessionPerformer.clear();
    }

    @Override
    public void addChange(EmbeddedClientSessionKey key, PersistentSessionUpdateTask<AuthenticatedClientSessionEntity> task) {
        clientSessionTx.addTask(key, task);
    }

    @Override
    public void restartEntity(EmbeddedClientSessionKey key, PersistentSessionUpdateTask<AuthenticatedClientSessionEntity> task) {
        clientSessionTx.restartEntity(key, task);
        addClientSessionToUserSession(key, task.isOffline());
    }

    private void addClientSessionToUserSession(EmbeddedClientSessionKey cacheKey, boolean offline) {
        sessionTx.registerClientSession(cacheKey.userSessionId(), cacheKey.clientId(), offline);
    }

    private void removeCachedUserAndClientSessionForUser(String realmId, String userId, boolean offline) {
        if (getCache(offline) == null) {
            // caching disabled
            return;
        }
        try (var stream = getCache(offline).getAdvancedCache()
                .entrySet()
                .stream()
                .filter(UserSessionPredicate.create(realmId).user(userId))
                .map(MapEntryToKeyMapper.getInstance())) {
            stream.forEach(RemoveKeyConsumer.getInstance());
        }
        try (var stream = getClientSessionCache(offline) .getAdvancedCache()
                .entrySet()
                .stream()
                .filter(new ClientSessionFilterByUser(realmId, userId))
                .map(MapEntryToKeyMapper.getInstance())) {
            stream.forEach(RemoveKeyConsumer.getInstance());
        }
    }
}
