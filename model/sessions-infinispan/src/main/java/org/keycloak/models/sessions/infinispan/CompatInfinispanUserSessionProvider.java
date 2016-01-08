package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.*;
import org.keycloak.models.sessions.infinispan.mapreduce.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;

import java.util.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CompatInfinispanUserSessionProvider extends InfinispanUserSessionProvider {

    private static final Logger log = Logger.getLogger(CompatInfinispanUserSessionProvider.class);

    public CompatInfinispanUserSessionProvider(KeycloakSession session, Cache<String, SessionEntity> sessionCache, Cache<String, SessionEntity> offlineSessionCache,
                                         Cache<LoginFailureKey, LoginFailureEntity> loginFailureCache) {
        super(session, sessionCache, offlineSessionCache, loginFailureCache);
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
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, false);
    }

    protected long getUserSessionsCount(RealmModel realm, ClientModel client, boolean offline) {
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
    public void removeExpired(RealmModel realm) {
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

        // Remove expired client initial access
        map = new MapReduceTask(sessionCache)
                .mappedWith(ClientInitialAccessMapper.create(realm.getId()).expired(Time.currentTime()).emitKey())
                .reducedWith(new FirstResultReducer())
                .execute();

        for (String id : map.keySet()) {
            tx.remove(sessionCache, id);
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

    @Override
    public void removeOfflineUserSession(RealmModel realm, String userSessionId) {
        removeUserSession(realm, userSessionId, true);
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
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        return getUserSessionsCount(realm, client, true);
    }

    @Override
    public List<UserSessionModel> getOfflineUserSessions(RealmModel realm, ClientModel client, int first, int max) {
        return getUserSessions(realm, client, first, max, true);
    }

    @Override
    public List<ClientInitialAccessModel> listClientInitialAccess(RealmModel realm) {
        Map<String, ClientInitialAccessEntity> entities = new MapReduceTask(sessionCache)
                .mappedWith(ClientInitialAccessMapper.create(realm.getId()))
                .reducedWith(new FirstResultReducer())
                .execute();
        return wrapClientInitialAccess(realm, entities.values());
    }

}
