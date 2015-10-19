package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.mapreduce.ClientSessionMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.FirstResultReducer;
import org.keycloak.models.sessions.infinispan.mapreduce.LargestResultReducer;
import org.keycloak.models.sessions.infinispan.mapreduce.SessionMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.UserLoginFailureMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.UserSessionMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.UserSessionNoteMapper;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.common.util.Time;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProvider.class);

    private final KeycloakSession session;
    private final Cache<String, SessionEntity> sessionCache;
    private final Cache<String, SessionEntity> offlineSessionCache;
    private final Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache;
    private final InfinispanKeycloakTransaction tx;

    public InfinispanUserSessionProvider(KeycloakSession session, Cache<String, SessionEntity> sessionCache, Cache<String, SessionEntity> offlineSessionCache,
                                         Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache) {
        this.session = session;
        this.sessionCache = sessionCache;
        this.offlineSessionCache = offlineSessionCache;
        this.loginFailureCache = loginFailureCache;
        this.tx = new InfinispanKeycloakTransaction();

        session.getTransaction().enlistAfterCompletion(tx);
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

        return wrap(realm, entity, false);
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

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        Map<String, UserSessionEntity> sessions = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).user(user.getId()))
                .reducedWith(new FirstResultReducer())
                .execute();

        return wrapUserSessions(realm, sessions.values(), false);
    }

    @Override
    public List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId) {
        Map<String, UserSessionEntity> sessions = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).brokerUserId(brokerUserId))
                .reducedWith(new FirstResultReducer())
                .execute();

        return wrapUserSessions(realm, sessions.values(), false);
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        Map<String, UserSessionEntity> sessions = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).brokerSessionId(brokerSessionId))
                .reducedWith(new FirstResultReducer())
                .execute();

        List<UserSessionModel> userSessionModels = wrapUserSessions(realm, sessions.values(), false);
        if (userSessionModels.isEmpty()) return null;
        return userSessionModels.get(0);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, -1, -1);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        return getUserSessions(realm, client, firstResult, maxResults, false);
    }

    protected List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        Map<String, Integer> map = new MapReduceTask(cache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).client(client.getId()).emitUserSessionAndTimestamp())
                .reducedWith(new LargestResultReducer())
                .execute();

        List<Map.Entry<String, Integer>> sessionTimestamps = new LinkedList<Map.Entry<String, Integer>>(map.entrySet());

        Collections.sort(sessionTimestamps, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                return e1.getValue().compareTo(e2.getValue());
            }
        });

        if (firstResult != -1 || maxResults == -1) {
            if (firstResult == -1) {
                firstResult = 0;
            }

            if (maxResults == -1) {
                maxResults = Integer.MAX_VALUE;
            }

            if (firstResult > sessionTimestamps.size()) {
                return Collections.emptyList();
            }

            int toIndex = (firstResult + maxResults) < sessionTimestamps.size() ? firstResult + maxResults : sessionTimestamps.size();
            sessionTimestamps = sessionTimestamps.subList(firstResult, toIndex);
        }

        List<UserSessionModel> userSessions = new LinkedList<UserSessionModel>();
        for (Map.Entry<String, Integer> e : sessionTimestamps) {
            UserSessionEntity userSessionEntity = (UserSessionEntity) cache.get(e.getKey());
            if (userSessionEntity != null) {
                userSessions.add(wrap(realm, userSessionEntity, offline));
            }
        }

        return userSessions;
    }

    @Override
    public List<UserSessionModel> getUserSessionsByNote(RealmModel realm, String noteName, String noteValue) {
        HashMap<String, String> notes = new HashMap<>();
        notes.put(noteName, noteValue);
        return getUserSessionsByNotes(realm, notes);
    }

    public List<UserSessionModel> getUserSessionsByNotes(RealmModel realm, Map<String, String> notes) {
        Map<String, UserSessionEntity> sessions = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionNoteMapper.create(realm.getId()).notes(notes))
                .reducedWith(new FirstResultReducer())
                .execute();

        return wrapUserSessions(realm, sessions.values(), false);

    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, false);
    }

    protected int getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        Map map = new MapReduceTask(cache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).client(client.getId()).emitUserSessionAndTimestamp())
                .reducedWith(new LargestResultReducer()).execute();

        return map.size();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        removeUserSession(realm, session.getId());
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user, false);
    }

    protected void removeUserSessions(RealmModel realm, UserModel user, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        Map<String, String> sessions = new MapReduceTask(cache)
                .mappedWith(UserSessionMapper.create(realm.getId()).user(user.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : sessions.keySet()) {
            removeUserSession(realm, id, offline);
        }
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

        int expired = Time.currentTime() - realm.getSsoSessionMaxLifespan();
        int expiredRefresh = Time.currentTime() - realm.getSsoSessionIdleTimeout();
        int expiredOffline = Time.currentTime() - realm.getOfflineSessionIdleTimeout();
        int expiredDettachedClientSession = Time.currentTime() - RealmInfoUtil.getDettachedClientSessionLifespan(realm);

        Map<String, String> map = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).expired(expired, expiredRefresh).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            removeUserSession(realm, id);
        }

        map = new MapReduceTask(sessionCache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).expiredRefresh(expiredDettachedClientSession).requireNullUserSession(true).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            tx.remove(sessionCache, id);
        }

        // Remove expired offline user sessions
        Map<String, SessionEntity> map2 = new MapReduceTask(offlineSessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).expired(null, expiredOffline))
                .reducedWith(new FirstResultReducer())
                .execute();

        for (Map.Entry<String, SessionEntity> entry : map2.entrySet()) {
            String userSessionId = entry.getKey();
            tx.remove(offlineSessionCache, userSessionId);
            // Propagate to persister
            persister.removeUserSession(userSessionId, true);

            UserSessionEntity entity = (UserSessionEntity) entry.getValue();
            for (String clientSessionId : entity.getClientSessions()) {
                tx.remove(offlineSessionCache, clientSessionId);
            }
        }

        // Remove expired offline client sessions
        map = new MapReduceTask(offlineSessionCache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).expiredRefresh(expiredOffline).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String clientSessionId : map.keySet()) {
            tx.remove(offlineSessionCache, clientSessionId);
            persister.removeClientSession(clientSessionId, true);
        }

    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        removeUserSessions(realm, false);
    }

    protected void removeUserSessions(RealmModel realm, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        Map<String, String> ids = new MapReduceTask(cache)
                .mappedWith(SessionMapper.create(realm.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : ids.keySet()) {
            cache.remove(id);
        }
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username) {
        LoginFailureKey key = new LoginFailureKey(realm.getId(), username);
        return wrap(key, loginFailureCache.get(key));
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username) {
        LoginFailureKey key = new LoginFailureKey(realm.getId(), username);
        LoginFailureEntity entity = new LoginFailureEntity();
        entity.setRealm(realm.getId());
        entity.setUsername(username);
        tx.put(loginFailureCache, key, entity);
        return wrap(key, entity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String username) {
        LoginFailureKey key = new LoginFailureKey(realm.getId(), username);
        tx.remove(loginFailureCache, key);
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        Map<LoginFailureKey, Object> sessions = new MapReduceTask(loginFailureCache)
                .mappedWith(UserLoginFailureMapper.create(realm.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (LoginFailureKey id : sessions.keySet()) {
            tx.remove(loginFailureCache, id);
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

        Map<String, ClientSessionEntity> map = new MapReduceTask(cache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).client(client.getId()))
                .reducedWith(new FirstResultReducer())
                .execute();

        for (Map.Entry<String, ClientSessionEntity> entry : map.entrySet()) {

            // detach from userSession
            ClientSessionAdapter adapter = wrap(realm, entry.getValue(), offline);
            adapter.setUserSession(null);

            tx.remove(cache, entry.getKey());
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
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

    protected void removeUserSession(RealmModel realm, String userSessionId) {
        removeUserSession(realm, userSessionId, false);
    }

    protected void removeUserSession(RealmModel realm, String userSessionId, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);

        tx.remove(cache, userSessionId);

        // TODO: Isn't more effective to retrieve from userSessionEntity directly?
        Map<String, String> map = new MapReduceTask(cache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).userSession(userSessionId).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            tx.remove(cache, id);
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
        List<UserSessionModel> models = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : entities) {
            models.add(wrap(realm, e, offline));
        }
        return models;
    }

    ClientSessionAdapter wrap(RealmModel realm, ClientSessionEntity entity, boolean offline) {
        Cache<String, SessionEntity> cache = getCache(offline);
        return entity != null ? new ClientSessionAdapter(session, this, cache, realm, entity, offline) : null;
    }


    UsernameLoginFailureModel wrap(LoginFailureKey key, LoginFailureEntity entity) {
        return entity != null ? new UsernameLoginFailureAdapter(this, loginFailureCache, key, entity) : null;
    }

    List<ClientSessionModel> wrapClientSessions(RealmModel realm, Collection<ClientSessionEntity> entities, boolean offline) {
        List<ClientSessionModel> models = new LinkedList<ClientSessionModel>();
        for (ClientSessionEntity e : entities) {
            models.add(wrap(realm, e, offline));
        }
        return models;
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
    public void removeOfflineUserSession(RealmModel realm, String userSessionId) {
        removeUserSession(realm, userSessionId, true);
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
        Map<String, UserSessionEntity> sessions = new MapReduceTask(offlineSessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).user(user.getId()))
                .reducedWith(new FirstResultReducer())
                .execute();

        List<ClientSessionEntity> clientSessions = new LinkedList<>();
        for (UserSessionEntity userSession : sessions.values()) {
            Set<String> currClientSessions = userSession.getClientSessions();
            for (String clientSessionId : currClientSessions) {
                ClientSessionEntity cls = (ClientSessionEntity) offlineSessionCache.get(clientSessionId);
                if (cls != null) {
                    clientSessions.add(cls);
                }
            }
        }

        return wrapClientSessions(realm, clientSessions, true);
    }

    @Override
    public void removeOfflineClientSession(RealmModel realm, String clientSessionId) {
        ClientSessionModel clientSession = getOfflineClientSession(realm, clientSessionId);
        removeClientSession(realm, clientSession, true);
    }

    @Override
    public int getOfflineSessionsCount(RealmModel realm, ClientModel client) {
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

    class InfinispanKeycloakTransaction implements KeycloakTransaction {

        private boolean active;
        private boolean rollback;
        private Map<Object, CacheTask> tasks = new HashMap<Object, CacheTask>();

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
