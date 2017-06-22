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
import org.infinispan.CacheStream;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.stream.Comparators;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserLoginFailurePredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProvider.class);

    protected final KeycloakSession session;
    protected final Cache<String, SessionEntity> sessionCache;
    protected final Cache<String, SessionEntity> offlineSessionCache;
    protected final Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache;
    protected final InfinispanKeycloakTransaction tx;

    public InfinispanUserSessionProvider(KeycloakSession session, Cache<String, SessionEntity> sessionCache, Cache<String, SessionEntity> offlineSessionCache,
                                         Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache) {
        this.session = session;
        this.sessionCache = sessionCache;
        this.offlineSessionCache = offlineSessionCache;
        this.loginFailureCache = loginFailureCache;
        this.tx = new InfinispanKeycloakTransaction();

        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    protected Cache<String, SessionEntity> getCache(boolean offline) {
        return offline ? offlineSessionCache : sessionCache;
    }

    @Override
    public AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();

        AuthenticatedClientSessionAdapter adapter = new AuthenticatedClientSessionAdapter(entity, client, (UserSessionAdapter) userSession, this, sessionCache);
        adapter.setUserSession(userSession);
        return adapter;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);

        updateSessionEntity(entity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

        tx.putIfAbsent(sessionCache, id, entity);

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
        Cache<String, SessionEntity> cache = getCache(offline);
        UserSessionEntity entity = (UserSessionEntity) tx.get(cache, id); // Chance created in this transaction

        if (entity == null) {
            entity = (UserSessionEntity) cache.get(id);
        }

        return wrap(realm, entity, offline);
    }

    protected List<UserSessionModel> getUserSessions(RealmModel realm, Predicate<Map.Entry<String, SessionEntity>> predicate, boolean offline) {
        CacheStream<Map.Entry<String, SessionEntity>> cacheStream = getCache(offline).entrySet().stream();
        Iterator<Map.Entry<String, SessionEntity>> itr = cacheStream.filter(predicate).iterator();
        List<UserSessionModel> sessions = new LinkedList<>();
        while (itr.hasNext()) {
            UserSessionEntity e = (UserSessionEntity) itr.next().getValue();
            sessions.add(wrap(realm, e, offline));
        }
        return sessions;
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
        final Cache<String, SessionEntity> cache = getCache(offline);

        Stream<UserSessionEntity> stream = cache.entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()).client(client.getId()))
                .map(Mappers.userSessionEntity())
                .sorted(Comparators.userSessionLastSessionRefresh());

        // Doesn't work due to ISPN-6575 . TODO Fix once infinispan upgraded to 8.2.2.Final or 9.0
//        if (firstResult > 0) {
//            stream = stream.skip(firstResult);
//        }
//
//        if (maxResults > 0) {
//            stream = stream.limit(maxResults);
//        }
//
//      List<UserSessionEntity> entities = stream.collect(Collectors.toList());


        // Workaround for ISPN-6575 TODO Fix once infinispan upgraded to 8.2.2.Final or 9.0 and replace with the more effective code above
        if (firstResult < 0) {
            firstResult = 0;
        }
        if (maxResults < 0) {
            maxResults = Integer.MAX_VALUE;
        }

        int count = firstResult + maxResults;
        if (count > 0) {
            stream = stream.limit(count);
        }
        List<UserSessionEntity> entities = stream.collect(Collectors.toList());

        if (firstResult > entities.size()) {
            return Collections.emptyList();
        }

        maxResults = Math.min(maxResults, entities.size() - firstResult);
        entities = entities.subList(firstResult, firstResult + maxResults);


        final List<UserSessionModel> sessions = new LinkedList<>();
        entities.stream().forEach(new Consumer<UserSessionEntity>() {
            @Override
            public void accept(UserSessionEntity userSessionEntity) {
                sessions.add(wrap(realm, userSessionEntity, offline));
            }
        });

        return sessions;
    }

    @Override
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, false);
    }

    protected long getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {
        return getCache(offline).entrySet().stream()
                .filter(UserSessionPredicate.create(realm.getId()).client(client.getId()))
                .count();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        UserSessionEntity entity = getUserSessionEntity(session, false);
        if (entity != null) {
            removeUserSession(realm, entity, false);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user, false);
    }

    protected void removeUserSessions(RealmModel realm, UserModel user, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        Iterator<SessionEntity> itr = cache.entrySet().stream().filter(UserSessionPredicate.create(realm.getId()).user(user.getId())).map(Mappers.sessionEntity()).iterator();
        while (itr.hasNext()) {
            UserSessionEntity userSessionEntity = (UserSessionEntity) itr.next();
            removeUserSession(realm, userSessionEntity, offline);
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
        Iterator<Map.Entry<String, SessionEntity>> itr = sessionCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL)
                .entrySet().stream().filter(UserSessionPredicate.create(realm.getId()).expired(expired, expiredRefresh)).iterator();

        int counter = 0;
        while (itr.hasNext()) {
            counter++;
            UserSessionEntity entity = (UserSessionEntity) itr.next().getValue();
            tx.remove(sessionCache, entity.getId());
        }

        log.debugf("Removed %d expired user sessions for realm '%s'", counter, realm.getName());
    }

    private void removeExpiredOfflineUserSessions(RealmModel realm) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        int expiredOffline = Time.currentTime() - realm.getOfflineSessionIdleTimeout();

        // Each cluster node cleanups just local sessions, which are those owned by himself (+ few more taking l1 cache into account)
        UserSessionPredicate predicate = UserSessionPredicate.create(realm.getId()).expired(null, expiredOffline);
        Iterator<Map.Entry<String, SessionEntity>> itr = offlineSessionCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL)
                .entrySet().stream().filter(predicate).iterator();

        int counter = 0;
        while (itr.hasNext()) {
            counter++;
            UserSessionEntity entity = (UserSessionEntity) itr.next().getValue();
            tx.remove(offlineSessionCache, entity.getId());

            persister.removeUserSession(entity.getId(), true);

            for (String clientUUID : entity.getAuthenticatedClientSessions().keySet()) {
                persister.removeClientSession(entity.getId(), clientUUID, true);
            }
        }

        log.debugf("Removed %d expired offline user sessions for realm '%s'", counter, realm.getName());
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        removeUserSessions(realm, false);
    }

    protected void removeUserSessions(RealmModel realm, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        Iterator<String> itr = cache.entrySet().stream().filter(SessionPredicate.create(realm.getId())).map(Mappers.sessionId()).iterator();
        while (itr.hasNext()) {
            cache.remove(itr.next());
        }
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
        Iterator<LoginFailureKey> itr = loginFailureCache.entrySet().stream().filter(UserLoginFailurePredicate.create(realm.getId())).map(Mappers.loginFailureId()).iterator();
        while (itr.hasNext()) {
            LoginFailureKey key = itr.next();
            tx.remove(loginFailureCache, key);
        }
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm, true);
        removeUserSessions(realm, false);
        removeAllUserLoginFailures(realm);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
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

    protected void removeUserSession(RealmModel realm, UserSessionEntity sessionEntity, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        tx.remove(cache, sessionEntity.getId());
    }

    InfinispanKeycloakTransaction getTx() {
        return tx;
    }

    UserSessionAdapter wrap(RealmModel realm, UserSessionEntity entity, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);
        return entity != null ? new UserSessionAdapter(session, this, cache, realm, entity, offline) : null;
    }

    List<UserSessionModel> wrapUserSessions(RealmModel realm, Collection<UserSessionEntity> entities, boolean offline) {
        List<UserSessionModel> models = new LinkedList<>();
        for (UserSessionEntity e : entities) {
            models.add(wrap(realm, e, offline));
        }
        return models;
    }

    UserLoginFailureModel wrap(LoginFailureKey key, LoginFailureEntity entity) {
        return entity != null ? new UserLoginFailureAdapter(this, loginFailureCache, key, entity) : null;
    }

    UserSessionEntity getUserSessionEntity(UserSessionModel userSession, boolean offline) {
        if (userSession instanceof UserSessionAdapter) {
            return ((UserSessionAdapter) userSession).getEntity();
        } else {
            Cache<String, SessionEntity> cache = getCache(offline);
            return cache != null ? (UserSessionEntity) cache.get(userSession.getId()) : null;
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
            removeUserSession(realm, userSessionEntity, true);
        }
    }



    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        UserSessionAdapter userSessionAdapter = (offlineUserSession instanceof UserSessionAdapter) ? (UserSessionAdapter) offlineUserSession :
                getOfflineUserSession(offlineUserSession.getRealm(), offlineUserSession.getId());

        AuthenticatedClientSessionAdapter offlineClientSession = importClientSession(userSessionAdapter, clientSession);

        // update timestamp to current time
        offlineClientSession.setTimestamp(Time.currentTime());

        return offlineClientSession;
    }

    @Override
    public List<UserSessionModel> getOfflineUserSessions(RealmModel realm, UserModel user) {
        Iterator<Map.Entry<String, SessionEntity>> itr = offlineSessionCache.entrySet().stream().filter(UserSessionPredicate.create(realm.getId()).user(user.getId())).iterator();
        List<UserSessionModel> userSessions = new LinkedList<>();

        while(itr.hasNext()) {
            UserSessionEntity entity = (UserSessionEntity) itr.next().getValue();
            UserSessionModel userSession = wrap(realm, entity, true);
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
        entity.setNotes(userSession.getNotes()== null ? new ConcurrentHashMap<>() : userSession.getNotes());
        entity.setAuthenticatedClientSessions(new ConcurrentHashMap<>());
        entity.setRememberMe(userSession.isRememberMe());
        entity.setState(userSession.getState());
        entity.setUser(userSession.getUser().getId());

        entity.setStarted(userSession.getStarted());
        entity.setLastSessionRefresh(userSession.getLastSessionRefresh());


        Cache<String, SessionEntity> cache = getCache(offline);
        tx.put(cache, userSession.getId(), entity);
        UserSessionAdapter importedSession = wrap(userSession.getRealm(), entity, offline);

        // Handle client sessions
        if (importAuthenticatedClientSessions) {
            for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
                importClientSession(importedSession, clientSession);
            }
        }

        return importedSession;
    }


    private AuthenticatedClientSessionAdapter importClientSession(UserSessionAdapter importedUserSession, AuthenticatedClientSessionModel clientSession) {
        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes());
        entity.setProtocolMappers(clientSession.getProtocolMappers());
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setRoles(clientSession.getRoles());
        entity.setTimestamp(clientSession.getTimestamp());

        Map<String, AuthenticatedClientSessionEntity> clientSessions = importedUserSession.getEntity().getAuthenticatedClientSessions();

        clientSessions.put(clientSession.getClient().getId(), entity);

        importedUserSession.update();

        return new AuthenticatedClientSessionAdapter(entity, clientSession.getClient(), importedUserSession, this, importedUserSession.getCache());
    }

}
