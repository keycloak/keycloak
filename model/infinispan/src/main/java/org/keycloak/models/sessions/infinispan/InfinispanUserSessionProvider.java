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
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.ClientInitialAccessEntity;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.stream.ClientInitialAccessPredicate;
import org.keycloak.models.sessions.infinispan.stream.ClientSessionPredicate;
import org.keycloak.models.sessions.infinispan.stream.Comparators;
import org.keycloak.models.sessions.infinispan.stream.Mappers;
import org.keycloak.models.sessions.infinispan.stream.SessionPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserLoginFailurePredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client) {
        String id = KeycloakModelUtils.generateId();

        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setTimestamp(Time.currentTime());
        entity.setClient(client.getId());


        tx.put(sessionCache, id, entity);

        ClientSessionAdapter wrap = wrap(realm, entity, false);
        return wrap;
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        String id = KeycloakModelUtils.generateId();

        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
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

        tx.put(sessionCache, id, entity);

        return wrap(realm, entity, false);
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        return getClientSession(realm, id, false);
    }

    protected ClientSessionModel getClientSession(RealmModel realm, String id, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);
        ClientSessionEntity entity = (ClientSessionEntity) cache.get(id);

        // Chance created in this transaction
        if (entity == null) {
            entity = (ClientSessionEntity) tx.get(cache, id);
        }

        return wrap(realm, entity, offline);
    }

    @Override
    public ClientSessionModel getClientSession(String id) {
        ClientSessionEntity entity = (ClientSessionEntity) sessionCache.get(id);

        // Chance created in this transaction
        if (entity == null) {
            entity = (ClientSessionEntity) tx.get(sessionCache, id);
        }

        if (entity != null) {
            RealmModel realm = session.realms().getRealm(entity.getRealm());
            return wrap(realm, entity, false);
        }
        return null;
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        return getUserSession(realm, id, false);
    }

    protected UserSessionAdapter getUserSession(RealmModel realm, String id, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);
        UserSessionEntity entity = (UserSessionEntity) cache.get(id);

        // Chance created in this transaction
        if (entity == null) {
            entity = (UserSessionEntity) tx.get(cache, id);
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

        Iterator<UserSessionTimestamp> itr = cache.entrySet().stream()
                .filter(ClientSessionPredicate.create(realm.getId()).client(client.getId()).requireUserSession())
                .map(Mappers.clientSessionToUserSessionTimestamp())
                .iterator();

        Map<String, UserSessionTimestamp> m = new HashMap<>();
        while(itr.hasNext()) {
            UserSessionTimestamp next = itr.next();
            if (!m.containsKey(next.getUserSessionId()) || m.get(next.getUserSessionId()).getClientSessionTimestamp() < next.getClientSessionTimestamp()) {
                m.put(next.getUserSessionId(), next);
            }
        }

        Stream<UserSessionTimestamp> stream = new LinkedList<>(m.values()).stream().sorted(Comparators.userSessionTimestamp());

        if (firstResult > 0) {
            stream = stream.skip(firstResult);
        }

        if (maxResults > 0) {
            stream = stream.limit(maxResults);
        }

        final List<UserSessionModel> sessions = new LinkedList<>();
        stream.forEach(new Consumer<UserSessionTimestamp>() {
            @Override
            public void accept(UserSessionTimestamp userSessionTimestamp) {
                SessionEntity entity = cache.get(userSessionTimestamp.getUserSessionId());
                if (entity != null) {
                    sessions.add(wrap(realm, (UserSessionEntity) entity, offline));
                }
            }
        });

        return sessions;
    }

    @Override
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, false);
    }

    protected long getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {
        return getCache(offline).entrySet().stream().filter(ClientSessionPredicate.create(realm.getId()).client(client.getId()).requireUserSession()).map(Mappers.clientSessionToUserSessionId()).distinct().count();
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
        removeExpiredClientSessions(realm);
        removeExpiredOfflineUserSessions(realm);
        removeExpiredOfflineClientSessions(realm);
        removeExpiredClientInitialAccess(realm);
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

            if (entity.getClientSessions() != null) {
                for (String clientSessionId : entity.getClientSessions()) {
                    tx.remove(sessionCache, clientSessionId);
                }
            }
        }

        log.debugf("Removed %d expired user sessions for realm '%s'", counter, realm.getName());
    }

    private void removeExpiredClientSessions(RealmModel realm) {
        int expiredDettachedClientSession = Time.currentTime() - RealmInfoUtil.getDettachedClientSessionLifespan(realm);

        // Each cluster node cleanups just local sessions, which are those owned by himself (+ few more taking l1 cache into account)
        Iterator<Map.Entry<String, SessionEntity>> itr = sessionCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL)
                .entrySet().stream().filter(ClientSessionPredicate.create(realm.getId()).expiredRefresh(expiredDettachedClientSession).requireNullUserSession()).iterator();

        int counter = 0;
        while (itr.hasNext()) {
            counter++;
            tx.remove(sessionCache, itr.next().getKey());
        }

        log.debugf("Removed %d expired client sessions for realm '%s'", counter, realm.getName());
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

            for (String clientSessionId : entity.getClientSessions()) {
                tx.remove(offlineSessionCache, clientSessionId);
            }
        }

        log.debugf("Removed %d expired offline user sessions for realm '%s'", counter, realm.getName());
    }

    private void removeExpiredOfflineClientSessions(RealmModel realm) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        int expiredOffline = Time.currentTime() - realm.getOfflineSessionIdleTimeout();

        // Each cluster node cleanups just local sessions, which are those owned by himself (+ few more taking l1 cache into account)
        Iterator<String> itr = offlineSessionCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL)
                .entrySet().stream().filter(ClientSessionPredicate.create(realm.getId()).expiredRefresh(expiredOffline)).map(Mappers.sessionId()).iterator();

        int counter = 0;
        while (itr.hasNext()) {
            counter++;
            String sessionId = itr.next();
            tx.remove(offlineSessionCache, sessionId);
            persister.removeClientSession(sessionId, true);
        }

        log.debugf("Removed %d expired offline client sessions for realm '%s'", counter, realm.getName());
    }

    private void removeExpiredClientInitialAccess(RealmModel realm) {
        Iterator<String> itr = sessionCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL)
                .entrySet().stream().filter(ClientInitialAccessPredicate.create(realm.getId()).expired(Time.currentTime())).map(Mappers.sessionId()).iterator();
        while (itr.hasNext()) {
            tx.remove(sessionCache, itr.next());
        }
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
        onClientRemoved(realm, client, true);
        onClientRemoved(realm, client, false);
    }

    private void onClientRemoved(RealmModel realm, ClientModel client, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        Iterator<Map.Entry<String, SessionEntity>> itr = cache.entrySet().stream().filter(ClientSessionPredicate.create(realm.getId()).client(client.getId())).iterator();
        while (itr.hasNext()) {
            ClientSessionEntity entity = (ClientSessionEntity) itr.next().getValue();
            ClientSessionAdapter adapter = wrap(realm, entity, offline);
            adapter.setUserSession(null);

            tx.remove(cache, entity.getId());
        }
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

    void attachSession(UserSessionAdapter userSession, ClientSessionModel clientSession) {
        UserSessionEntity entity = userSession.getEntity();
        String clientSessionId = clientSession.getId();
        if (entity.getClientSessions() == null) {
            entity.setClientSessions(new HashSet<String>());
        }
        if (!entity.getClientSessions().contains(clientSessionId)) {
            entity.getClientSessions().add(clientSessionId);
            userSession.update();
        }
    }

    @Override
    public void removeClientSession(RealmModel realm, ClientSessionModel clientSession) {
        removeClientSession(realm, clientSession, false);
    }

    protected void removeClientSession(RealmModel realm, ClientSessionModel clientSession, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession != null)  {
            UserSessionEntity entity = ((UserSessionAdapter) userSession).getEntity();
            if (entity.getClientSessions() != null) {
                entity.getClientSessions().remove(clientSession.getId());

            }
            tx.replace(cache, entity.getId(), entity);
        }
        tx.remove(cache, clientSession.getId());
    }


    void dettachSession(UserSessionAdapter userSession, ClientSessionModel clientSession) {
        UserSessionEntity entity = userSession.getEntity();
        String clientSessionId = clientSession.getId();
        if (entity.getClientSessions() != null && entity.getClientSessions().contains(clientSessionId)) {
            entity.getClientSessions().remove(clientSessionId);
            if (entity.getClientSessions().isEmpty()) {
                entity.setClientSessions(null);
            }
            userSession.update();
        }
    }

    protected void removeUserSession(RealmModel realm, UserSessionEntity sessionEntity, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        tx.remove(cache, sessionEntity.getId());

        if (sessionEntity.getClientSessions() != null) {
            for (String clientSessionId : sessionEntity.getClientSessions()) {
                tx.remove(cache, clientSessionId);
            }
        }
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

    List<ClientInitialAccessModel> wrapClientInitialAccess(RealmModel realm, Collection<ClientInitialAccessEntity> entities) {
        List<ClientInitialAccessModel> models = new LinkedList<>();
        for (ClientInitialAccessEntity e : entities) {
            models.add(wrap(realm, e));
        }
        return models;
    }

    ClientSessionAdapter wrap(RealmModel realm, ClientSessionEntity entity, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);
        return entity != null ? new ClientSessionAdapter(session, this, cache, realm, entity, offline) : null;
    }

    ClientInitialAccessAdapter wrap(RealmModel realm, ClientInitialAccessEntity entity) {
        Cache<String, SessionEntity> cache = getCache(false);
        return entity != null ? new ClientInitialAccessAdapter(session, this, cache, realm, entity) : null;
    }

    UserLoginFailureModel wrap(LoginFailureKey key, LoginFailureEntity entity) {
        return entity != null ? new UserLoginFailureAdapter(this, loginFailureCache, key, entity) : null;
    }

    List<ClientSessionModel> wrapClientSessions(RealmModel realm, Collection<ClientSessionEntity> entities, boolean offline) {
        List<ClientSessionModel> models = new LinkedList<>();
        for (ClientSessionEntity e : entities) {
            models.add(wrap(realm, e, offline));
        }
        return models;
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
        UserSessionAdapter offlineUserSession = importUserSession(userSession, true);

        // started and lastSessionRefresh set to current time
        int currentTime = Time.currentTime();
        offlineUserSession.getEntity().setStarted(currentTime);
        offlineUserSession.setLastSessionRefresh(currentTime);

        return offlineUserSession;
    }

    @Override
    public UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId) {
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
    public ClientSessionModel createOfflineClientSession(ClientSessionModel clientSession) {
        ClientSessionAdapter offlineClientSession = importClientSession(clientSession, true);

        // update timestamp to current time
        offlineClientSession.setTimestamp(Time.currentTime());

        return offlineClientSession;
    }

    @Override
    public ClientSessionModel getOfflineClientSession(RealmModel realm, String clientSessionId) {
        return getClientSession(realm, clientSessionId, true);
    }

    @Override
    public List<ClientSessionModel> getOfflineClientSessions(RealmModel realm, UserModel user) {
        Iterator<Map.Entry<String, SessionEntity>> itr = offlineSessionCache.entrySet().stream().filter(UserSessionPredicate.create(realm.getId()).user(user.getId())).iterator();
        List<ClientSessionModel> clientSessions = new LinkedList<>();

        while(itr.hasNext()) {
            UserSessionEntity entity = (UserSessionEntity) itr.next().getValue();
            Set<String> currClientSessions = entity.getClientSessions();

            if (currClientSessions == null) {
                continue;
            }

            for (String clientSessionId : currClientSessions) {
                ClientSessionEntity cls = (ClientSessionEntity) offlineSessionCache.get(clientSessionId);
                if (cls != null) {
                    clientSessions.add(wrap(realm, cls, true));
                }
            }
        }

        return clientSessions;
    }

    @Override
    public void removeOfflineClientSession(RealmModel realm, String clientSessionId) {
        ClientSessionModel clientSession = getOfflineClientSession(realm, clientSessionId);
        removeClientSession(realm, clientSession, true);
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
    public UserSessionAdapter importUserSession(UserSessionModel userSession, boolean offline) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(userSession.getId());
        entity.setRealm(userSession.getRealm().getId());

        entity.setAuthMethod(userSession.getAuthMethod());
        entity.setBrokerSessionId(userSession.getBrokerSessionId());
        entity.setBrokerUserId(userSession.getBrokerUserId());
        entity.setIpAddress(userSession.getIpAddress());
        entity.setLoginUsername(userSession.getLoginUsername());
        entity.setNotes(userSession.getNotes());
        entity.setRememberMe(userSession.isRememberMe());
        entity.setState(userSession.getState());
        entity.setUser(userSession.getUser().getId());

        entity.setStarted(userSession.getStarted());
        entity.setLastSessionRefresh(userSession.getLastSessionRefresh());

        Cache<String, SessionEntity> cache = getCache(offline);
        tx.put(cache, userSession.getId(), entity);
        return wrap(userSession.getRealm(), entity, offline);
    }

    @Override
    public ClientSessionAdapter importClientSession(ClientSessionModel clientSession, boolean offline) {
        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(clientSession.getId());
        entity.setRealm(clientSession.getRealm().getId());

        entity.setAction(clientSession.getAction());
        entity.setAuthenticatorStatus(clientSession.getExecutionStatus());
        entity.setAuthMethod(clientSession.getAuthMethod());
        if (clientSession.getAuthenticatedUser() != null) {
            entity.setAuthUserId(clientSession.getAuthenticatedUser().getId());
        }
        entity.setClient(clientSession.getClient().getId());
        entity.setNotes(clientSession.getNotes());
        entity.setProtocolMappers(clientSession.getProtocolMappers());
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setRoles(clientSession.getRoles());
        entity.setTimestamp(clientSession.getTimestamp());
        entity.setUserSessionNotes(clientSession.getUserSessionNotes());

        Cache<String, SessionEntity> cache = getCache(offline);
        tx.put(cache, clientSession.getId(), entity);
        return wrap(clientSession.getRealm(), entity, offline);
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count) {
        String id = KeycloakModelUtils.generateId();

        ClientInitialAccessEntity entity = new ClientInitialAccessEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setTimestamp(Time.currentTime());
        entity.setExpiration(expiration);
        entity.setCount(count);
        entity.setRemainingCount(count);

        tx.put(sessionCache, id, entity);

        return wrap(realm, entity);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id) {
        Cache<String, SessionEntity> cache = getCache(false);
        ClientInitialAccessEntity entity = (ClientInitialAccessEntity) cache.get(id);

        // If created in this transaction
        if (entity == null) {
            entity = (ClientInitialAccessEntity) tx.get(cache, id);
        }

        return wrap(realm, entity);
    }

    @Override
    public void removeClientInitialAccessModel(RealmModel realm, String id) {
        tx.remove(getCache(false), id);
    }

    @Override
    public List<ClientInitialAccessModel> listClientInitialAccess(RealmModel realm) {
        Iterator<Map.Entry<String, SessionEntity>> itr = sessionCache.entrySet().stream().filter(ClientInitialAccessPredicate.create(realm.getId())).iterator();
        List<ClientInitialAccessModel> list = new LinkedList<>();
        while (itr.hasNext()) {
            list.add(wrap(realm, (ClientInitialAccessEntity) itr.next().getValue()));
        }
        return list;
    }


    class InfinispanKeycloakTransaction implements KeycloakTransaction {

        private boolean active;
        private boolean rollback;
        private Map<Object, CacheTask> tasks = new HashMap<>();

        @Override
        public void begin() {
            active = true;
        }

        @Override
        public void commit() {
            if (rollback) {
                throw new RuntimeException("Rollback only!");
            }

            for (CacheTask task : tasks.values()) {
                task.execute();
            }
        }

        @Override
        public void rollback() {
            tasks.clear();
        }

        @Override
        public void setRollbackOnly() {
            rollback = true;
        }

        @Override
        public boolean getRollbackOnly() {
            return rollback;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        public void put(Cache cache, Object key, Object value) {
            log.tracev("Adding cache operation: {0} on {1}", CacheOperation.ADD, key);

            Object taskKey = getTaskKey(cache, key);
            if (tasks.containsKey(taskKey)) {
                throw new IllegalStateException("Can't add session: task in progress for session");
            } else {
                tasks.put(taskKey, new CacheTask(cache, CacheOperation.ADD, key, value));
            }
        }

        public void replace(Cache cache, Object key, Object value) {
            log.tracev("Adding cache operation: {0} on {1}", CacheOperation.REPLACE, key);

            Object taskKey = getTaskKey(cache, key);
            CacheTask current = tasks.get(taskKey);
            if (current != null) {
                switch (current.operation) {
                    case ADD:
                    case REPLACE:
                        current.value = value;
                        return;
                    case REMOVE:
                        return;
                }
            } else {
                tasks.put(taskKey, new CacheTask(cache, CacheOperation.REPLACE, key, value));
            }
        }

        public void remove(Cache cache, Object key) {
            log.tracev("Adding cache operation: {0} on {1}", CacheOperation.REMOVE, key);

            Object taskKey = getTaskKey(cache, key);
            tasks.put(taskKey, new CacheTask(cache, CacheOperation.REMOVE, key, null));
        }

        // This is for possibility to lookup for session by id, which was created in this transaction
        public Object get(Cache cache, Object key) {
            Object taskKey = getTaskKey(cache, key);
            CacheTask current = tasks.get(taskKey);
            if (current != null) {
                switch (current.operation) {
                    case ADD:
                    case REPLACE:
                        return current.value;                 }
            }

            return null;
        }

        private Object getTaskKey(Cache cache, Object key) {
            if (key instanceof String) {
                return new StringBuilder(cache.getName())
                        .append("::")
                        .append(key.toString()).toString();
            } else {
                // loginFailure cache
                return key;
            }
        }

        public class CacheTask {
            private Cache cache;
            private CacheOperation operation;
            private Object key;
            private Object value;

            public CacheTask(Cache cache, CacheOperation operation, Object key, Object value) {
                this.cache = cache;
                this.operation = operation;
                this.key = key;
                this.value = value;
            }

            public void execute() {
                log.tracev("Executing cache operation: {0} on {1}", operation, key);

                switch (operation) {
                    case ADD:
                        cache.put(key, value);
                        break;
                    case REMOVE:
                        cache.remove(key);
                        break;
                    case REPLACE:
                        cache.replace(key, value);
                        break;
                }
            }
        }

    }

    public enum CacheOperation {
        ADD, REMOVE, REPLACE
    }

}
