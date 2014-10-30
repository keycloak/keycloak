package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.mapreduce.ClientSessionMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.FirstResultReducer;
import org.keycloak.models.sessions.infinispan.mapreduce.LargestResultReducer;
import org.keycloak.models.sessions.infinispan.mapreduce.SessionMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.UserSessionMapper;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private final KeycloakSession session;
    private final Cache<String, SessionEntity> sessionCache;
    private final Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache;
    private final InfinispanKeycloakTransaction tx;

    public InfinispanUserSessionProvider(KeycloakSession session, Cache<String, SessionEntity> sessionCache, Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache) {
        this.session = session;
        this.sessionCache = sessionCache;
        this.loginFailureCache = loginFailureCache;
        this.tx = new InfinispanKeycloakTransaction();

        session.getTransaction().enlistAfterCompletion(tx);
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

        return wrap(realm, entity);
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe) {
        String id = KeycloakModelUtils.generateId();

        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setUser(user.getId());
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        tx.put(sessionCache, id, entity);

        return wrap(realm, entity);
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        ClientSessionEntity entity = (ClientSessionEntity) sessionCache.get(id);
        return wrap(realm, entity);
    }

    @Override
    public ClientSessionModel getClientSession(String id) {
        ClientSessionEntity entity = (ClientSessionEntity) sessionCache.get(id);
        if (entity != null) {
            RealmModel realm = session.realms().getRealm(entity.getRealm());
            return wrap(realm, entity);
        }
        return null;
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        UserSessionEntity entity = (UserSessionEntity) sessionCache.get(id);
        return wrap(realm, entity);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        Map<String, UserSessionEntity> sessions = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).user(user.getId()))
                .reducedWith(new FirstResultReducer())
                .execute();

        return wrapUserSessions(realm, sessions.values());
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, -1, -1);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        Map<String, Integer> map = new MapReduceTask(sessionCache)
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
            UserSessionEntity userSessionEntity = (UserSessionEntity) sessionCache.get(e.getKey());
            if (userSessionEntity != null) {
                userSessions.add(wrap(realm, userSessionEntity));
            }
        }

        return userSessions;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        Map map = new MapReduceTask(sessionCache)
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
        Map<String, String> sessions = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).user(user.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : sessions.keySet()) {
            removeUserSession(realm, id);
        }
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        int expired = Time.currentTime() - realm.getSsoSessionMaxLifespan();
        int expiredRefresh = Time.currentTime() - realm.getSsoSessionIdleTimeout();

        Map<String, String> map = new MapReduceTask(sessionCache)
                .mappedWith(UserSessionMapper.create(realm.getId()).expired(expired, expiredRefresh).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            removeUserSession(realm, id);
        }

        map = new MapReduceTask(sessionCache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).expiredRefresh(expiredRefresh).requireNullUserSession(true).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            tx.remove(sessionCache, id);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        Map<String, String> ids = new MapReduceTask(sessionCache)
                .mappedWith(SessionMapper.create(realm.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : ids.keySet()) {
            sessionCache.remove(id);
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
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        Map<String, String> map = new MapReduceTask(sessionCache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).client(client.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            tx.remove(sessionCache, id);
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);

        loginFailureCache.remove(new LoginFailureKey(realm.getId(), user.getUsername()));
        loginFailureCache.remove(new LoginFailureKey(realm.getId(), user.getEmail()));
    }

    @Override
    public void close() {
    }

    void attachSession(UserSessionModel userSession, ClientSessionModel clientSession) {
        UserSessionEntity entity = ((UserSessionAdapter) userSession).getEntity();
        String clientSessionId = clientSession.getId();
        if (entity.getClientSessions() == null) {
            entity.setClientSessions(new HashSet<String>());
        }
        if (!entity.getClientSessions().contains(clientSessionId)) {
            entity.getClientSessions().add(clientSessionId);
            tx.replace(sessionCache, entity.getId(), entity);
        }
    }

    void dettachSession(UserSessionModel userSession, ClientSessionModel clientSession) {
        UserSessionEntity entity = ((UserSessionAdapter) userSession).getEntity();
        String clientSessionId = clientSession.getId();
        if (entity.getClientSessions() != null && entity.getClientSessions().contains(clientSessionId)) {
            entity.getClientSessions().remove(clientSessionId);
            if (entity.getClientSessions().isEmpty()) {
                entity.setClientSessions(null);
            }
            tx.replace(sessionCache, entity.getId(), entity);
        }
    }

    void removeUserSession(RealmModel realm, String userSessionId) {
        tx.remove(sessionCache, userSessionId);

        Map<String, String> map = new MapReduceTask(sessionCache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).userSession(userSessionId).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            tx.remove(sessionCache, id);
        }
    }

    InfinispanKeycloakTransaction getTx() {
        return tx;
    }

    UserSessionModel wrap(RealmModel realm, UserSessionEntity entity) {
        return entity != null ? new UserSessionAdapter(session, this, sessionCache, realm, entity) : null;
    }

    List<UserSessionModel> wrapUserSessions(RealmModel realm, Collection<UserSessionEntity> entities) {
        List<UserSessionModel> models = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : entities) {
            models.add(wrap(realm, e));
        }
        return models;
    }

    ClientSessionModel wrap(RealmModel realm, ClientSessionEntity entity) {
        return entity != null ? new ClientSessionAdapter(session, this, sessionCache, realm, entity) : null;
    }


    UsernameLoginFailureModel wrap(LoginFailureKey key, LoginFailureEntity entity) {
        return entity != null ? new UsernameLoginFailureAdapter(this, loginFailureCache, key, entity) : null;
    }

    List<ClientSessionModel> wrapClientSessions(RealmModel realm, Collection<ClientSessionEntity> entities) {
        List<ClientSessionModel> models = new LinkedList<ClientSessionModel>();
        for (ClientSessionEntity e : entities) {
            models.add(wrap(realm, e));
        }
        return models;
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
            if (tasks.containsKey(key)) {
                throw new IllegalStateException("Can't add session: task in progress for session");
            } else {
                tasks.put(key, new CacheTask(cache, CacheOperation.ADD, key, value));
            }
        }

        public void replace(Cache cache, Object key, Object value) {
            CacheTask current = tasks.get(key);
            if (current != null) {
                switch (current.operation) {
                    case ADD:
                    case REPLACE:
                        current.value = value;
                        return;
                    case REMOVE:
                        throw new IllegalStateException("Can't remove session: task in progress for session");
                }
            } else {
                tasks.put(key, new CacheTask(cache, CacheOperation.REPLACE, key, value));
            }
        }

        public void remove(Cache cache, String key) {
            tasks.put(key, new CacheTask(cache, CacheOperation.REMOVE, key, null));
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
