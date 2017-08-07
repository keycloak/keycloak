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
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.sessions.LastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.ClientRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveAllUserLoginFailuresEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.events.SessionEventsSenderTransaction;
import org.keycloak.models.sessions.infinispan.stream.Comparators;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserLoginFailurePredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProvider.class);

    protected final KeycloakSession session;

    protected final Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache;
    protected final Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache;
    protected final Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache;

    protected final InfinispanChangelogBasedTransaction<UserSessionEntity> sessionTx;
    protected final InfinispanChangelogBasedTransaction<UserSessionEntity> offlineSessionTx;
    protected final InfinispanKeycloakTransaction tx;

    protected final SessionEventsSenderTransaction clusterEventsSenderTx;

    protected final LastSessionRefreshStore lastSessionRefreshStore;
    protected final LastSessionRefreshStore offlineLastSessionRefreshStore;

    public InfinispanUserSessionProvider(KeycloakSession session,
                                         RemoteCacheInvoker remoteCacheInvoker,
                                         LastSessionRefreshStore lastSessionRefreshStore,
                                         LastSessionRefreshStore offlineLastSessionRefreshStore,
                                         Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache,
                                         Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache,
                                         Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache) {
        this.session = session;

        this.sessionCache = sessionCache;
        this.offlineSessionCache = offlineSessionCache;
        this.loginFailureCache = loginFailureCache;

        this.sessionTx = new InfinispanChangelogBasedTransaction<>(session, InfinispanConnectionProvider.SESSION_CACHE_NAME, sessionCache, remoteCacheInvoker);
        this.offlineSessionTx = new InfinispanChangelogBasedTransaction<>(session, InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, offlineSessionCache, remoteCacheInvoker);

        this.tx = new InfinispanKeycloakTransaction();

        this.clusterEventsSenderTx = new SessionEventsSenderTransaction(session);

        this.lastSessionRefreshStore = lastSessionRefreshStore;
        this.offlineLastSessionRefreshStore = offlineLastSessionRefreshStore;

        session.getTransactionManager().enlistAfterCompletion(tx);
        session.getTransactionManager().enlistAfterCompletion(clusterEventsSenderTx);
        session.getTransactionManager().enlistAfterCompletion(sessionTx);
        session.getTransactionManager().enlistAfterCompletion(offlineSessionTx);
    }

    protected Cache<String, SessionEntityWrapper<UserSessionEntity>> getCache(boolean offline) {
        return offline ? offlineSessionCache : sessionCache;
    }

    protected InfinispanChangelogBasedTransaction<UserSessionEntity> getTransaction(boolean offline) {
        return offline ? offlineSessionTx : sessionTx;
    }

    protected LastSessionRefreshStore getLastSessionRefreshStore() {
        return lastSessionRefreshStore;
    }

    protected LastSessionRefreshStore getOfflineLastSessionRefreshStore() {
        return offlineLastSessionRefreshStore;
    }

    @Override
    public AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();

        InfinispanChangelogBasedTransaction<UserSessionEntity> updateTx = getTransaction(false);
        AuthenticatedClientSessionAdapter adapter = new AuthenticatedClientSessionAdapter(entity, client, (UserSessionAdapter) userSession, this, updateTx);
        adapter.setUserSession(userSession);
        return adapter;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
        updateSessionEntity(entity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

        SessionUpdateTask<UserSessionEntity> createSessionTask = new SessionUpdateTask<UserSessionEntity>() {

            @Override
            public void runUpdate(UserSessionEntity session) {

            }

            @Override
            public CacheOperation getOperation(UserSessionEntity session) {
                return CacheOperation.ADD_IF_ABSENT;
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                return CrossDCMessageStatus.SYNC;
            }

        };

        sessionTx.addTask(id, createSessionTask, entity);

        return wrap(realm, entity, false);
    }

    void updateSessionEntity(UserSessionEntity entity, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        entity.setRealm(realm.getId());
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
        UserSessionEntity entity = getUserSessionEntity(id, offline);
        return wrap(realm, entity, offline);
    }

    private UserSessionEntity getUserSessionEntity(String id, boolean offline) {
        InfinispanChangelogBasedTransaction<UserSessionEntity> tx = getTransaction(offline);
        SessionEntityWrapper<UserSessionEntity> entityWrapper = tx.get(id);
        return entityWrapper==null ? null : entityWrapper.getEntity();
    }


    protected List<UserSessionModel> getUserSessions(RealmModel realm, Predicate<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>> predicate, boolean offline) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);

        cache = CacheDecorators.skipCacheLoaders(cache);

        Stream<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>> cacheStream = cache.entrySet().stream();

        List<UserSessionModel> resultSessions = new LinkedList<>();

        Iterator<UserSessionEntity> itr = cacheStream.filter(predicate)
                .map(Mappers.userSessionEntity())
                .iterator();

        while (itr.hasNext()) {
            UserSessionEntity userSessionEntity = itr.next();
            resultSessions.add(wrap(realm, userSessionEntity, offline));
        }

        return resultSessions;
    }

    @Override
    public List<UserSessionModel> getUserSessions(final RealmModel realm, UserModel user) {
        return getUserSessions(realm, UserSessionPredicate.create(realm.getId()).user(user.getId()), false);
    }

    @Override
    public List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId) {
        return getUserSessions(realm, UserSessionPredicate.create(realm.getId()).brokerUserId(brokerUserId), false);
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        List<UserSessionModel> userSessions = getUserSessions(realm, UserSessionPredicate.create(realm.getId()).brokerSessionId(brokerSessionId), false);
        return userSessions.isEmpty() ? null : userSessions.get(0);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, -1, -1);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        return getUserSessions(realm, client, firstResult, maxResults, false);
    }

    protected List<UserSessionModel> getUserSessions(final RealmModel realm, ClientModel client, int firstResult, int maxResults, final boolean offline) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);

        cache = CacheDecorators.skipCacheLoaders(cache);

        Stream<UserSessionEntity> stream = cache.entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()).client(client.getId()))
                .map(Mappers.userSessionEntity())
                .sorted(Comparators.userSessionLastSessionRefresh());

        if (firstResult > 0) {
            stream = stream.skip(firstResult);
        }

        if (maxResults > 0) {
            stream = stream.limit(maxResults);
        }

        final List<UserSessionModel> sessions = new LinkedList<>();
        Iterator<UserSessionEntity> itr = stream.iterator();

        while (itr.hasNext()) {
            UserSessionEntity userSessionEntity = itr.next();
            sessions.add(wrap(realm, userSessionEntity, offline));
        }


        return sessions;
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
            UserSessionEntity remoteSessionEntity = (UserSessionEntity) remoteCache.get(id);
            if (remoteSessionEntity != null) {
                log.debugf("getUserSessionWithPredicate(%s): remote cache contains session entity %s", id, remoteSessionEntity);

                UserSessionModel remoteSessionAdapter = wrap(realm, remoteSessionEntity, offline);
                if (predicate.test(remoteSessionAdapter)) {

                    InfinispanChangelogBasedTransaction<UserSessionEntity> tx = getTransaction(offline);

                    // Remote entity contains our predicate. Update local cache with the remote entity
                    SessionEntityWrapper<UserSessionEntity> sessionWrapper = remoteSessionEntity.mergeRemoteEntityWithLocalEntity(tx.get(id));

                    // Replace entity just in ispn cache. Skip remoteStore
                    cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD, Flag.IGNORE_RETURN_VALUES)
                            .replace(id, sessionWrapper);

                    tx.reloadEntityInCurrentTransaction(realm, id, sessionWrapper);

                    // Recursion. We should have it locally now
                    return getUserSessionWithPredicate(realm, id, offline, predicate);
                }
            }
        }

        log.debugf("getUserSessionWithPredicate(%s): not found", id);

        return null;
    }


    @Override
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, false);
    }

    protected long getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        cache = CacheDecorators.skipCacheLoaders(cache);

        return cache.entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()).client(client.getId()))
                .count();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        UserSessionEntity entity = getUserSessionEntity(session, false);
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

    @Override
    public void removeExpired(RealmModel realm) {
        log.debugf("Removing expired sessions");
        removeExpiredUserSessions(realm);
        removeExpiredOfflineUserSessions(realm);
    }

    private void removeExpiredUserSessions(RealmModel realm) {
        int expired = Time.currentTime() - realm.getSsoSessionMaxLifespan();
        int expiredRefresh = Time.currentTime() - realm.getSsoSessionIdleTimeout();

        // Each cluster node cleanups just local sessions, which are those owned by himself (+ few more taking l1 cache into account)
        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCache = CacheDecorators.localCache(sessionCache);

        int[] counter = { 0 };

        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCacheStoreIgnore = CacheDecorators.skipCacheLoaders(localCache);

        // Ignore remoteStore for stream iteration. But we will invoke remoteStore for userSession removal propagate
        localCacheStoreIgnore
                .entrySet()
                .stream()
                .filter(UserSessionPredicate.create(realm.getId()).expired(expired, expiredRefresh))
                .map(Mappers.sessionId())
                .forEach(new Consumer<String>() {

                    @Override
                    public void accept(String sessionId) {
                        counter[0]++;
                        tx.remove(localCache, sessionId);
                    }

                });


        log.debugf("Removed %d expired user sessions for realm '%s'", counter[0], realm.getName());
    }

    private void removeExpiredOfflineUserSessions(RealmModel realm) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        int expiredOffline = Time.currentTime() - realm.getOfflineSessionIdleTimeout();

        // Each cluster node cleanups just local sessions, which are those owned by himself (+ few more taking l1 cache into account)
        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCache = CacheDecorators.localCache(offlineSessionCache);

        UserSessionPredicate predicate = UserSessionPredicate.create(realm.getId()).expired(null, expiredOffline);

        final int[] counter = { 0 };

        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCacheStoreIgnore = CacheDecorators.skipCacheLoaders(localCache);

        // Ignore remoteStore for stream iteration. But we will invoke remoteStore for userSession removal propagate
        localCacheStoreIgnore
                .entrySet()
                .stream()
                .filter(predicate)
                .map(Mappers.userSessionEntity())
                .forEach(new Consumer<UserSessionEntity>() {

                    @Override
                    public void accept(UserSessionEntity userSessionEntity) {
                        counter[0]++;
                        tx.remove(localCache, userSessionEntity.getId());

                        // TODO:mposolda can be likely optimized to delete all expired at one step
                        persister.removeUserSession( userSessionEntity.getId(), true);

                        // TODO can be likely optimized to delete all at one step
                        for (String clientUUID : userSessionEntity.getAuthenticatedClientSessions().keySet()) {
                            persister.removeClientSession(userSessionEntity.getId(), clientUUID, true);
                        }
                    }
                });

        log.debugf("Removed %d expired offline user sessions for realm '%s'", counter, realm.getName());
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        // Don't send message to all DCs, just to all cluster nodes in current DC. The remoteCache will notify client listeners for removed userSessions. This assumes that 2nd DC contains same userSessions like current one.
        clusterEventsSenderTx.addEvent(InfinispanUserSessionProviderFactory.REMOVE_USER_SESSIONS_EVENT, RemoveUserSessionsEvent.create(realm.getId()), false);
    }

    protected void onRemoveUserSessionsEvent(String realmId) {
        removeLocalUserSessions(realmId, false);
    }

    private void removeLocalUserSessions(String realmId, boolean offline) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCache = CacheDecorators.localCache(cache);

        Cache<String, SessionEntityWrapper<UserSessionEntity>> localCacheStoreIgnore = CacheDecorators.skipCacheLoaders(localCache);

        localCacheStoreIgnore
                .entrySet()
                .stream()
                .filter(SessionPredicate.create(realmId))
                .map(Mappers.sessionId())
                .forEach(new Consumer<String>() {

                    @Override
                    public void accept(String sessionId) {
                        localCache.remove(sessionId);
                    }

                });
    }

    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        LoginFailureKey key = new LoginFailureKey(realm.getId(), userId);
        return wrap(key, loginFailureCache.get(key));
    }

    @Override
    public UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId) {
        LoginFailureKey key = new LoginFailureKey(realm.getId(), userId);
        LoginFailureEntity entity = new LoginFailureEntity();
        entity.setRealm(realm.getId());
        entity.setUserId(userId);
        tx.put(loginFailureCache, key, entity);
        return wrap(key, entity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String userId) {
        tx.remove(loginFailureCache, new LoginFailureKey(realm.getId(), userId));
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        clusterEventsSenderTx.addEvent(InfinispanUserSessionProviderFactory.REMOVE_ALL_LOGIN_FAILURES_EVENT, RemoveAllUserLoginFailuresEvent.create(realm.getId()), false);
    }

    protected void onRemoveAllUserLoginFailuresEvent(String realmId) {
        removeAllLocalUserLoginFailuresEvent(realmId);
    }

    private void removeAllLocalUserLoginFailuresEvent(String realmId) {
        Cache<LoginFailureKey, LoginFailureEntity> localCache = CacheDecorators.localCache(loginFailureCache);

        Cache<LoginFailureKey, LoginFailureEntity> localCacheStoreIgnore = CacheDecorators.skipCacheLoaders(localCache);

        Iterator<LoginFailureKey> itr = localCacheStoreIgnore
                .entrySet()
                .stream()
                .filter(UserLoginFailurePredicate.create(realmId))
                .map(Mappers.loginFailureId())
                .iterator();

        while (itr.hasNext()) {
            LoginFailureKey key = itr.next();
            localCache.remove(key);
        }
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        clusterEventsSenderTx.addEvent(InfinispanUserSessionProviderFactory.REALM_REMOVED_SESSION_EVENT, RealmRemovedSessionEvent.create(realm.getId()), false);
    }

    protected void onRealmRemovedEvent(String realmId) {
        removeLocalUserSessions(realmId, true);
        removeLocalUserSessions(realmId, false);
        removeAllLocalUserLoginFailuresEvent(realmId);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        clusterEventsSenderTx.addEvent(InfinispanUserSessionProviderFactory.CLIENT_REMOVED_SESSION_EVENT, ClientRemovedSessionEvent.create(realm.getId(), client.getId()), false);
    }

    protected void onClientRemovedEvent(String realmId, String clientUuid) {
        // Nothing for now. userSession.getAuthenticatedClientSessions() will check lazily if particular client exists and update userSession on-the-fly.
    }


    protected void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user, true);
        removeUserSessions(realm, user, false);

        loginFailureCache.remove(new LoginFailureKey(realm.getId(), user.getUsername()));
        loginFailureCache.remove(new LoginFailureKey(realm.getId(), user.getEmail()));
    }

    @Override
    public void close() {
    }

    protected void removeUserSession(UserSessionEntity sessionEntity, boolean offline) {
        InfinispanChangelogBasedTransaction<UserSessionEntity> tx = getTransaction(offline);

        SessionUpdateTask<UserSessionEntity> removeTask = new SessionUpdateTask<UserSessionEntity>() {

            @Override
            public void runUpdate(UserSessionEntity entity) {

            }

            @Override
            public CacheOperation getOperation(UserSessionEntity entity) {
                return CacheOperation.REMOVE;
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                return CrossDCMessageStatus.SYNC;
            }

        };

        tx.addTask(sessionEntity.getId(), removeTask);
    }

    InfinispanKeycloakTransaction getTx() {
        return tx;
    }

    UserSessionAdapter wrap(RealmModel realm, UserSessionEntity entity, boolean offline) {
        InfinispanChangelogBasedTransaction<UserSessionEntity> tx = getTransaction(offline);
        return entity != null ? new UserSessionAdapter(session, this, tx, realm, entity, offline) : null;
    }

    UserLoginFailureModel wrap(LoginFailureKey key, LoginFailureEntity entity) {
        return entity != null ? new UserLoginFailureAdapter(this, loginFailureCache, key, entity) : null;
    }

    UserSessionEntity getUserSessionEntity(UserSessionModel userSession, boolean offline) {
        if (userSession instanceof UserSessionAdapter) {
            return ((UserSessionAdapter) userSession).getEntity();
        } else {
            return getUserSessionEntity(userSession.getId(), offline);
        }
    }


    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        UserSessionAdapter offlineUserSession = importUserSession(userSession, true, false);

        // started and lastSessionRefresh set to current time
        int currentTime = Time.currentTime();
        offlineUserSession.getEntity().setStarted(currentTime);
        offlineUserSession.setLastSessionRefresh(currentTime);

        return offlineUserSession;
    }

    @Override
    public UserSessionAdapter getOfflineUserSession(RealmModel realm, String userSessionId) {
        return getUserSession(realm, userSessionId, true);
    }

    @Override
    public void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession) {
        UserSessionEntity userSessionEntity = getUserSessionEntity(userSession, true);
        if (userSessionEntity != null) {
            removeUserSession(userSessionEntity, true);
        }
    }



    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        UserSessionAdapter userSessionAdapter = (offlineUserSession instanceof UserSessionAdapter) ? (UserSessionAdapter) offlineUserSession :
                getOfflineUserSession(offlineUserSession.getRealm(), offlineUserSession.getId());

        AuthenticatedClientSessionAdapter offlineClientSession = importClientSession(userSessionAdapter, clientSession, getTransaction(true));

        // update timestamp to current time
        offlineClientSession.setTimestamp(Time.currentTime());

        return offlineClientSession;
    }

    @Override
    public List<UserSessionModel> getOfflineUserSessions(RealmModel realm, UserModel user) {
        List<UserSessionModel> userSessions = new LinkedList<>();

        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = CacheDecorators.skipCacheLoaders(offlineSessionCache);

        Iterator<UserSessionEntity> itr = cache.entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()).user(user.getId()))
                .map(Mappers.userSessionEntity())
                .iterator();

        while (itr.hasNext()) {
            UserSessionEntity userSessionEntity = itr.next();
            UserSessionModel userSession = wrap(realm, userSessionEntity, true);
            userSessions.add(userSession);
        }

        return userSessions;
    }

    @Override
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, true);
    }

    @Override
    public List<UserSessionModel> getOfflineUserSessions(RealmModel realm, ClientModel client, int first, int max) {
        return getUserSessions(realm, client, first, max, true);
    }

    @Override
    public UserSessionAdapter importUserSession(UserSessionModel userSession, boolean offline, boolean importAuthenticatedClientSessions) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(userSession.getId());
        entity.setRealm(userSession.getRealm().getId());

        entity.setAuthMethod(userSession.getAuthMethod());
        entity.setBrokerSessionId(userSession.getBrokerSessionId());
        entity.setBrokerUserId(userSession.getBrokerUserId());
        entity.setIpAddress(userSession.getIpAddress());
        entity.setLoginUsername(userSession.getLoginUsername());
        entity.setNotes(userSession.getNotes() == null ? new ConcurrentHashMap<>() : userSession.getNotes());
        entity.setAuthenticatedClientSessions(new ConcurrentHashMap<>());
        entity.setRememberMe(userSession.isRememberMe());
        entity.setState(userSession.getState());
        entity.setUser(userSession.getUser().getId());

        entity.setStarted(userSession.getStarted());
        entity.setLastSessionRefresh(userSession.getLastSessionRefresh());


        InfinispanChangelogBasedTransaction<UserSessionEntity> tx = getTransaction(offline);

        SessionUpdateTask importTask = new SessionUpdateTask<UserSessionEntity>() {

            @Override
            public void runUpdate(UserSessionEntity session) {

            }

            @Override
            public CacheOperation getOperation(UserSessionEntity session) {
                return CacheOperation.ADD_IF_ABSENT;
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                return CrossDCMessageStatus.SYNC;
            }

        };
        tx.addTask(userSession.getId(), importTask, entity);

        UserSessionAdapter importedSession = wrap(userSession.getRealm(), entity, offline);

        // Handle client sessions
        if (importAuthenticatedClientSessions) {
            for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
                importClientSession(importedSession, clientSession, tx);
            }
        }

        return importedSession;
    }


    private AuthenticatedClientSessionAdapter importClientSession(UserSessionAdapter importedUserSession, AuthenticatedClientSessionModel clientSession,
                                                                  InfinispanChangelogBasedTransaction<UserSessionEntity> updateTx) {
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes() == null ? new ConcurrentHashMap<>() : clientSession.getNotes());
        entity.setProtocolMappers(clientSession.getProtocolMappers());
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setRoles(clientSession.getRoles());
        entity.setTimestamp(clientSession.getTimestamp());


        Map<String, AuthenticatedClientSessionEntity> clientSessions = importedUserSession.getEntity().getAuthenticatedClientSessions();

        clientSessions.put(clientSession.getClient().getId(), entity);

        SessionUpdateTask importTask = new SessionUpdateTask<UserSessionEntity>() {

            @Override
            public void runUpdate(UserSessionEntity session) {
                Map<String, AuthenticatedClientSessionEntity> clientSessions = session.getAuthenticatedClientSessions();
                clientSessions.put(clientSession.getClient().getId(), entity);
            }

            @Override
            public CacheOperation getOperation(UserSessionEntity session) {
                return CacheOperation.REPLACE;
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                return CrossDCMessageStatus.SYNC;
            }

        };
        updateTx.addTask(importedUserSession.getId(), importTask);

        return new AuthenticatedClientSessionAdapter(entity, clientSession.getClient(), importedUserSession, this, updateTx);
    }

}
