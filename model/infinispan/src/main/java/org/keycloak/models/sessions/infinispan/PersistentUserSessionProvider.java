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

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Flowable;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.util.ByRef;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.persistence.manager.PersistenceManager;
import org.jboss.logging.Logger;
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
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.ClientSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.JpaChangesPerformer;
import org.keycloak.models.sessions.infinispan.changes.MergedUpdate;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdatesList;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.changes.UserSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionWrapperPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.models.sessions.infinispan.util.FuturesHelper;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;

import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;
import static org.keycloak.models.sessions.infinispan.changes.ClientSessionPersistentChangelogBasedTransaction.createAuthenticatedClientSessionInstance;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PersistentUserSessionProvider implements UserSessionProvider, SessionRefreshStore {

    private static final Logger log = Logger.getLogger(PersistentUserSessionProvider.class);

    protected final KeycloakSession session;

    protected final UserSessionPersistentChangelogBasedTransaction sessionTx;
    protected final ClientSessionPersistentChangelogBasedTransaction clientSessionTx;

    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    protected final InfinispanKeyGenerator keyGenerator;

    public PersistentUserSessionProvider(KeycloakSession session,
                                         InfinispanKeyGenerator keyGenerator,
                                         UserSessionPersistentChangelogBasedTransaction sessionTx,
                                         ClientSessionPersistentChangelogBasedTransaction clientSessionTx) {
        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
            throw new IllegalStateException("Persistent user sessions are not enabled");
        }

        this.session = session;
        this.sessionTx = sessionTx;
        this.clientSessionTx = clientSessionTx;
        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);
        this.keyGenerator = keyGenerator;

        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
    }

    protected Cache<String, SessionEntityWrapper<UserSessionEntity>> getCache(boolean offline) {
        return sessionTx.getCache(offline);
    }

    protected Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> getClientSessionCache(boolean offline) {
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

        AuthenticatedClientSessionAdapter adapter = new AuthenticatedClientSessionAdapter(session, entity, client, userSession, clientSessionTx, false);

        if (userSession.isOffline()) {
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
        clientSessionTx.addTask(clientSessionId, createClientSessionTask, entity, persistenceState);
        sessionTx.registerClientSession(userSession.getId(), client.getId(), clientSessionId, userSession.isOffline());

        return adapter;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                              String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        if (id == null) {
            id = keyGenerator.generateKeyString(session, sessionTx.getCache(false));
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
    public AuthenticatedClientSessionAdapter getClientSession(UserSessionModel userSession, ClientModel client, String clientSessionId, boolean offline) {
        if (clientSessionId == null) {
            log.debugf("Client-session id is null. userSessionId=%s, clientId=%s, offline=%s",
                    userSession.getId(), client.getId(), offline);
            return null;
        }

        UUID clientSessionUUID = UUID.fromString(clientSessionId);

        SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSessionEntity = clientSessionTx.get(client.getRealm(), client, userSession, clientSessionUUID, offline);
        if (clientSessionEntity != null) {
            return new AuthenticatedClientSessionAdapter(session, clientSessionEntity.getEntity(), client, userSession, clientSessionTx, offline);
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
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = getClientSessionCache(offline);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> localClientSessionCache = CacheDecorators.localCache(clientSessionCache);
        final AtomicInteger userSessionsSize = new AtomicInteger();

        removeEntriesByRealm(realmId, localCache, userSessionsSize, localClientSessionCache);
        log.debugf("Removed %d sessions in realm %s. Offline: %b", (Object) userSessionsSize.get(), realmId, offline);
    }

    private static void removeEntriesByRealm(String realmId, Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionsCache, AtomicInteger userSessionsSize, Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessions) {
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
                    userSessionEntity.getAuthenticatedClientSessions().forEach((clientUUID, clientSessionId) -> {
                        Future<SessionEntityWrapper<AuthenticatedClientSessionEntity>> f = clientSessions.removeAsync(clientSessionId);
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
        sessionEntity.getAuthenticatedClientSessions().forEach((clientUUID, clientSessionId) -> clientSessionTx.addTask(clientSessionId, Tasks.removeSync(offline)));
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
        offlineClientSession.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(offlineClientSession.getTimestamp()));
        offlineClientSession.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(offlineUserSession.getStarted()));

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

        Map<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsById = new HashMap<>();

        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionModel clientSession = entry.getValue();
            AuthenticatedClientSessionEntity clientSessionToImport = createAuthenticatedClientSessionInstance(sessionId, clientSession,
                    realmId, clientUUID, offline);
            clientSessionToImport.setUserSessionId(sessionId);

            if (offline) {
                // Update timestamp to the same value as userSession. LastSessionRefresh of userSession from DB will have a correct value.
                // This is an optimization with the old code before persistent user sessions existed, and is probably valid as an offline user session is supposed to have only one client session.
                // Remove this code once this once the persistent sessions is the only way to handle sessions, and the old client sessions have been migrated to have an updated timestamp.
                clientSessionToImport.setTimestamp(userSessionEntityToImport.getLastSessionRefresh());
            }

            clientSessionsById.put(clientSessionToImport.getId(), new SessionEntityWrapper<>(clientSessionToImport));

            // Update userSession entity with the clientSession
            AuthenticatedClientSessionStore clientSessions = userSessionEntityToImport.getAuthenticatedClientSessions();
            clientSessions.put(clientUUID, clientSessionToImport.getId());
        }

        SessionEntityWrapper<UserSessionEntity>  wrappedUserSessionEntity = new SessionEntityWrapper<>(userSessionEntityToImport);

        SessionEntityWrapper<UserSessionEntity> existingSession = sessionTx.importSession(realm, sessionId, wrappedUserSessionEntity, offline, lifespan, maxIdle);
        if (existingSession != null) {
            // skip import the client sessions, they should have been imported too.
            log.debugf("The user-session already imported by another transaction for sessionId=%s offline=%s", sessionId, offline);
            return existingSession;
        }

        // Import client sessions
        clientSessionTx.importSessionsConcurrently(realm, clientSessionsById, offline);
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
        entity.setAuthenticatedClientSessions(new AuthenticatedClientSessionStore());
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
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(sessionToImportInto.getId(), clientSession,
                sessionToImportInto.getRealm().getId(), clientSession.getClient().getId(), true);
        entity.setUserSessionId(sessionToImportInto.getId());

        // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
        entity.setTimestamp(sessionToImportInto.getLastSessionRefresh());

        final UUID clientSessionId = entity.getId();

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        clientSessionTx.addTask(entity.getId(), createClientSessionTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        AuthenticatedClientSessionStore clientSessions = sessionToImportInto.getEntity().getAuthenticatedClientSessions();
        clientSessions.put(clientSession.getClient().getId(), clientSessionId);
        sessionTx.registerClientSession(sessionToImportInto.getId(), clientSession.getClient().getId(), clientSessionId, true);

        return new AuthenticatedClientSessionAdapter(session, entity, clientSession.getClient(), sessionToImportInto, clientSessionTx, true);
    }

    public SessionEntityWrapper<UserSessionEntity> wrapPersistentEntity(RealmModel realm, boolean offline, UserSessionModel persistentUserSession) {
        UserSessionEntity userSessionEntity = createUserSessionEntityInstance(persistentUserSession);

        if (isUserSessionExpired(realm, userSessionEntity, offline)) {
            return null;
        }

        sessionTx.addTask(userSessionEntity.getId(), null, userSessionEntity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : persistentUserSession.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionEntity clientSession = createAuthenticatedClientSessionInstance(persistentUserSession.getId(), entry.getValue(),
                    userSessionEntity.getRealmId(), clientUUID, offline);
            clientSession.setUserSessionId(userSessionEntity.getId());

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

            // Update userSession entity with the clientSession
            AuthenticatedClientSessionStore clientSessions = userSessionEntity.getAuthenticatedClientSessions();
            clientSessions.put(clientUUID, clientSession.getId());
            clientSessionTx.addTask(clientSession.getId(), null, clientSession, UserSessionModel.SessionPersistenceState.PERSISTENT);
        }

        return sessionTx.get(userSessionEntity.getId(), offline);

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

    public static UUID createClientSessionUUID(String userSessionId, String clientId) {
        // This allows creating a UUID that is constant even if the entry is reloaded from the database
        return UUID.nameUUIDFromBytes((userSessionId + clientId).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void migrate(String modelVersion) {
        // Changed encoding from JBoss Marshalling to ProtoStream.
        // Unable to read the cached data.
        if ("26.0.0".equals(modelVersion)) {
            log.debug("Clear caches to migrate to Infinispan Protostream");
            CompletionStages.join(session.getProvider(InfinispanConnectionProvider.class).migrateToProtoStream());
        }
    }

    /**
     * Copy over all sessions in Infinispan to the persistent user sessions in the database.
     * This method is public so people can use it to build their custom migrations or re-import sessions when necessary
     * in a future version of Keycloak.
     */
    public void migrateNonPersistentSessionsToPersistentSessions() {
        var sessionCache = sessionTx.getCache(false);
        var clientSessionCache = clientSessionTx.getCache(false);
        JpaChangesPerformer<String, UserSessionEntity> userSessionPerformer = new JpaChangesPerformer<>(sessionCache.getName(), null);
        JpaChangesPerformer<UUID, AuthenticatedClientSessionEntity> clientSessionPerformer = new JpaChangesPerformer<>(clientSessionCache.getName(), null);
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
    private void processEntryFromCache(SessionEntityWrapper<UserSessionEntity> sessionEntityWrapper, JpaChangesPerformer<String, UserSessionEntity> userSessionPerformer, JpaChangesPerformer<UUID, AuthenticatedClientSessionEntity> clientSessionPerformer, AtomicInteger count) {
        RealmModel realm = session.realms().getRealm(sessionEntityWrapper.getEntity().getRealmId());
        if (realm == null) {
            // ignoring old and unknown realm found in the session
            return;
        }
        var clientSessionCache = clientSessionTx.getCache(false);
        sessionEntityWrapper.getEntity().getAuthenticatedClientSessions().forEach((clientId, uuid) -> {
            SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSession = clientSessionCache.get(uuid);
            if (clientSession != null) {
                // This is necessary because client sessions created by a KC version < 22 do not have clientId set within the entity.
                if (clientSession.getEntity().getClientId() == null) {
                    clientSession.getEntity().setClientId(clientId);
                }
                clientSession.getEntity().setUserSessionId(sessionEntityWrapper.getEntity().getId());
                MergedUpdate<AuthenticatedClientSessionEntity> merged = MergedUpdate.computeUpdate(Collections.singletonList(Tasks.addIfAbsentSync()), clientSession, 1, 1);
                clientSessionPerformer.registerChange(Map.entry(uuid, new SessionUpdatesList<>(realm, clientSession)), merged);
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

    private <E extends SessionEntity, K> void flush(JpaChangesPerformer<K, E> userSessionsPerformer, JpaChangesPerformer<UUID, AuthenticatedClientSessionEntity> clientSessionPerformer) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(),
                s -> {
                    userSessionsPerformer.write(s);
                    clientSessionPerformer.write(s);
                });
        userSessionsPerformer.clear();
        clientSessionPerformer.clear();
    }

}
