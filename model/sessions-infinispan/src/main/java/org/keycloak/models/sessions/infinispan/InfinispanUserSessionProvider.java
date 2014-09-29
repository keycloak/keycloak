package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProvider implements UserSessionProvider {

    private KeycloakSession session;
    private Cache<String, SessionEntity> cache;

    public InfinispanUserSessionProvider(KeycloakSession session, Cache<String, SessionEntity> cache) {
        this.session = session;
        this.cache = cache;
    }

    @Override
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, String redirectUri, String state, Set<String> roles) {
        String id = KeycloakModelUtils.generateId();

        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setTimestamp(Time.currentTime());
        entity.setClient(client.getId());
        entity.setUserSession(userSession.getId());
        entity.setRedirectUri(redirectUri);
        entity.setState(state);
        entity.setRoles(roles);

        cache.put(id, entity);

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

        cache.put(id, entity);

        return wrap(realm, entity);
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        ClientSessionEntity entity = (ClientSessionEntity) cache.get(id);
        return wrap(realm, entity);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        UserSessionEntity entity = (UserSessionEntity) cache.get(id);
        return wrap(realm, entity);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        Map<String, UserSessionEntity> sessions = new MapReduceTask(cache)
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
                userSessions.add(wrap(realm, userSessionEntity));
            }
        }

        return userSessions;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
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
        Map<String, String> sessions = new MapReduceTask(cache)
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

        Map<String, String> map = new MapReduceTask(cache)
                .mappedWith(UserSessionMapper.create(realm.getId()).expired(expired, expiredRefresh).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            removeUserSession(realm, id);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        // TODO Remove realm

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
        Map<String, String> map = new MapReduceTask(cache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).client(client.getId()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            cache.remove(id);
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);
    }

    @Override
    public void close() {
    }

    void removeUserSession(RealmModel realm, String userSessionId) {
        cache.remove(userSessionId);

        Map<String, String> map = new MapReduceTask(cache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).userSession(userSessionId).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            cache.remove(id);
        }
    }

    UserSessionModel wrap(RealmModel realm, UserSessionEntity entity) {
        return entity != null ? new UserSessionAdapter(session, this, cache, realm, entity) : null;
    }

    List<UserSessionModel> wrapUserSessions(RealmModel realm, Collection<UserSessionEntity> entities) {
        List<UserSessionModel> models = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : entities) {
            models.add(wrap(realm, e));
        }
        return models;
    }

    ClientSessionModel wrap(RealmModel realm, ClientSessionEntity entity) {
        return entity != null ? new ClientSessionAdapter(session, this, cache, realm, entity) : null;
    }

    List<ClientSessionModel> wrapClientSessions(RealmModel realm, Collection<ClientSessionEntity> entities) {
        List<ClientSessionModel> models = new LinkedList<ClientSessionModel>();
        for (ClientSessionEntity e : entities) {
            models.add(wrap(realm, e));
        }
        return models;
    }

}
