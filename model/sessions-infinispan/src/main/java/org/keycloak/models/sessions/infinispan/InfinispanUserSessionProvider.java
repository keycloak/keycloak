package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.manager.DefaultCacheManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.mapreduce.ClientSessionMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.FirstResultReducer;
import org.keycloak.models.sessions.infinispan.mapreduce.LargestResultReducer;
import org.keycloak.models.sessions.infinispan.mapreduce.UserSessionMapper;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private KeycloakSession session;
    private DefaultCacheManager cacheManager;

    public InfinispanUserSessionProvider(KeycloakSession session, DefaultCacheManager cacheManager) {
        this.session = session;
        this.cacheManager = cacheManager;
    }

    @Override
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, String redirectUri, String state, Set<String> roles) {
        String id = KeycloakModelUtils.generateId();

        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(id);
        entity.setTimestamp(Time.currentTime());
        entity.setClient(client.getId());
        entity.setUserSession(userSession.getId());
        entity.setRedirectUri(redirectUri);
        entity.setState(state);
        entity.setRoles(roles);

        clientCache(realm).put(id, entity);

        return wrap(realm, entity);
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe) {
        String id = KeycloakModelUtils.generateId();

        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
        entity.setUser(user.getId());
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        userCache(realm).put(id, entity);

        return wrap(realm, entity);
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        ClientSessionEntity entity = clientCache(realm).get(id);
        return wrap(realm, entity);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        UserSessionEntity entity = userCache(realm).get(id);
        return wrap(realm, entity);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        Cache<String, UserSessionEntity> userCache = userCache(realm);

        Map<String, UserSessionEntity> sessions = new MapReduceTask(userCache)
                .mappedWith(UserSessionMapper.create().user(user.getId()))
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
        Cache<String, ClientSessionEntity> clientCache = clientCache(realm);
        Cache<String, UserSessionEntity> userCache = userCache(realm);

        Map<String, Integer> map = new MapReduceTask(clientCache)
                .mappedWith(ClientSessionMapper.create().client(client.getId()).emitUserSessionAndTimestamp())
                .reducedWith(new LargestResultReducer())
                .execute();

        List<Map.Entry<String, Integer>> sessionTimestamps = new LinkedList<Map.Entry<String, Integer>>(map.entrySet());

        sessionTimestamps.sort(new Comparator<Map.Entry<String, Integer>>() {
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
            UserSessionEntity userSessionEntity = userCache.get(e.getKey());
            if (userSessionEntity != null) {
                userSessions.add(wrap(realm, userSessionEntity));
            }
        }

        return userSessions;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        Cache<String, ClientSessionEntity> clientCache = clientCache(realm);

        Map map = new MapReduceTask(clientCache)
                .mappedWith(ClientSessionMapper.create().client(client.getId()).emitUserSessionAndTimestamp())
                .reducedWith(new LargestResultReducer()).execute();

        return map.size();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        Cache<String, UserSessionEntity> userCache = userCache(realm);
        Cache<String, ClientSessionEntity> clientCache = clientCache(realm);

        removeUserSession(userCache, clientCache, session.getId());
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        Cache<String, UserSessionEntity> userCache = userCache(realm);
        Cache<String, ClientSessionEntity> clientCache = clientCache(realm);

        Map<String, String> sessions = new MapReduceTask(userCache)
                .mappedWith(UserSessionMapper.create().user(user.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : sessions.keySet()) {
            removeUserSession(userCache, clientCache, id);
        }
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        Cache<String, UserSessionEntity> userCache = userCache(realm);
        Cache<String, ClientSessionEntity> clientCache = clientCache(realm);

        int expired = Time.currentTime() - realm.getSsoSessionMaxLifespan();
        int expiredRefresh = Time.currentTime() - realm.getSsoSessionIdleTimeout();

        Map<String, String> map = new MapReduceTask(userCache)
                .mappedWith(UserSessionMapper.create().expired(expired, expiredRefresh).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            removeUserSession(userCache, clientCache, id);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        cacheManager.removeCache(realm.getId() + ":userSessions");
        cacheManager.removeCache(realm.getId() + ":clientSessions");
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username) {
        // TODO
        return null;
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username) {
        // TODO
        return null;
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        // TODO
        return null;
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        Cache<String, ClientSessionEntity> clientCache = clientCache(realm);

        Map<String, String> map = new MapReduceTask(clientCache)
                .mappedWith(ClientSessionMapper.create().client(client.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            clientCache.remove(id);
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);
    }

    @Override
    public void close() {
    }

    void removeUserSession(Cache<String, UserSessionEntity> userCache, Cache<String, ClientSessionEntity> clientCache, String userSessionId) {
        userCache.remove(userSessionId);

        Map<String, String> map = new MapReduceTask(clientCache)
                .mappedWith(ClientSessionMapper.create().userSession(userSessionId).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            clientCache.remove(id);
        }
    }

    UserSessionModel wrap(RealmModel realm, UserSessionEntity entity) {
        return entity != null ? new UserSessionAdapter(session, this, realm, entity) : null;
    }

    List<UserSessionModel> wrapUserSessions(RealmModel realm, Collection<UserSessionEntity> entities) {
        List<UserSessionModel> models = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : entities) {
            models.add(wrap(realm, e));
        }
        return models;
    }

    ClientSessionModel wrap(RealmModel realm, ClientSessionEntity entity) {
        return entity != null ? new ClientSessionAdapter(session, this, realm, entity) : null;
    }

    List<ClientSessionModel> wrapClientSessions(RealmModel realm, Collection<ClientSessionEntity> entities) {
        List<ClientSessionModel> models = new LinkedList<ClientSessionModel>();
        for (ClientSessionEntity e : entities) {
            models.add(wrap(realm, e));
        }
        return models;
    }

    Cache<String, UserSessionEntity> userCache(RealmModel realm) {
        return cacheManager.getCache(realm.getId() + ":userSessions");
    }

    Cache<String, ClientSessionEntity> clientCache(RealmModel realm) {
        return cacheManager.getCache(realm.getId() + ":clientSessions");
    }

}
