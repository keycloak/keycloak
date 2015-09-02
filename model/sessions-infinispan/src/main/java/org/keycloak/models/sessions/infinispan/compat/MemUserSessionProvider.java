package org.keycloak.models.sessions.infinispan.compat;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.infinispan.compat.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UsernameLoginFailureEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UsernameLoginFailureKey;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.util.Time;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemUserSessionProvider implements UserSessionProvider {

    private final KeycloakSession session;
    private final ConcurrentHashMap<String, UserSessionEntity> userSessions;
    private final ConcurrentHashMap<String, String> userSessionsByBrokerSessionId;
    private final ConcurrentHashMap<String, Set<String>> userSessionsByBrokerUserId;
    private final ConcurrentHashMap<String, ClientSessionEntity> clientSessions;
    private final ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures;

    public MemUserSessionProvider(KeycloakSession session, ConcurrentHashMap<String, UserSessionEntity> userSessions, ConcurrentHashMap<String, String> userSessionsByBrokerSessionId, ConcurrentHashMap<String, Set<String>> userSessionsByBrokerUserId, ConcurrentHashMap<String, ClientSessionEntity> clientSessions, ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures) {
        this.session = session;
        this.userSessions = userSessions;
        this.clientSessions = clientSessions;
        this.loginFailures = loginFailures;
        this.userSessionsByBrokerSessionId = userSessionsByBrokerSessionId;
        this.userSessionsByBrokerUserId = userSessionsByBrokerUserId;
    }

    @Override
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client) {
        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setTimestamp(Time.currentTime());
        entity.setClientId(client.getId());
        entity.setRealmId(realm.getId());
        clientSessions.put(entity.getId(), entity);
        return new ClientSessionAdapter(session, this, realm, entity);
    }

    @Override
    public void removeClientSession(RealmModel realm, ClientSessionModel clientSession) {
        ClientSessionEntity entity = ((ClientSessionAdapter)clientSession).getEntity();
        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession != null) {
            UserSessionEntity userSessionEntity = ((UserSessionAdapter)userSession).getEntity();
            userSessionEntity.getClientSessions().remove(entity);
        }
        clientSessions.remove(clientSession.getId());
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        ClientSessionEntity entity = clientSessions.get(id);
        return entity != null ? new ClientSessionAdapter(session, this, realm, entity) : null;
    }

    @Override
    public ClientSessionModel getClientSession(String id) {
        ClientSessionEntity entity = clientSessions.get(id);
        if (entity != null) {
            RealmModel realm = session.realms().getRealm(entity.getRealmId());
            return  new ClientSessionAdapter(session, this, realm, entity);
        }
        return null;
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

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);
        entity.setBrokerSessionId(brokerSessionId);
        entity.setBrokerUserId(brokerUserId);

        userSessions.put(id, entity);
        if (brokerSessionId != null) {
            userSessionsByBrokerSessionId.put(brokerSessionId, id);
        }
        if (brokerUserId != null) {
            while (true) {  // while loop gets around a race condition when a user session is removed
                Set<String> set = userSessionsByBrokerUserId.get(brokerUserId);
                if (set == null) {
                    Set<String> value = new HashSet<>();
                    set = userSessionsByBrokerUserId.putIfAbsent(brokerUserId, value);
                    if (set == null) {
                        set = value;
                    }
                }
                synchronized (set) {
                    set.add(id);
                }
                if (userSessionsByBrokerUserId.get(brokerUserId) == set) {
                    // we are ensured set isn't deleted before the new id is added
                    break;
                }
            }
        }

        return new UserSessionAdapter(session, this, realm, entity);
    }

    @Override
    public List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId) {
        Set<String> sessions = userSessionsByBrokerUserId.get(brokerUserId);
        if (sessions == null) return Collections.emptyList();
        List<UserSessionModel> userSessions = new LinkedList<UserSessionModel>();
        for (String id : sessions) {
            UserSessionModel userSession = getUserSession(realm, id);
            if (userSession != null) userSessions.add(userSession);
        }
        return userSessions;
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        String id = userSessionsByBrokerSessionId.get(brokerSessionId);
        if (id == null) return null;
        return getUserSession(realm, id);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        UserSessionEntity entity = getUserSessionEntity(realm, id);
        return entity != null ? new UserSessionAdapter(session, this, realm, entity) : null;
    }

    UserSessionEntity getUserSessionEntity(RealmModel realm, String id) {
        UserSessionEntity entity = userSessions.get(id);
        if (entity != null && entity.getRealm().equals(realm.getId())) {
            return entity;
        }
        return null;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        List<UserSessionModel> userSessions = new LinkedList<UserSessionModel>();
        for (UserSessionEntity s : this.userSessions.values()) {
            if (s.getRealm().equals(realm.getId()) && s.getUser().equals(user.getId())) {
                userSessions.add(new UserSessionAdapter(session, this, realm, s));
            }
        }
        return userSessions;
    }

    @Override
    public List<UserSessionModel> getUserSessionsByNote(RealmModel realm, String noteName, String noteValue) {
        List<UserSessionModel> userSessions = new LinkedList<UserSessionModel>();
        for (UserSessionEntity s : this.userSessions.values()) {
            if (s.getRealm().equals(realm.getId()) && noteValue.equals(s.getNotes().get(noteName))) {
                userSessions.add(new UserSessionAdapter(session, this, realm, s));
            }
        }
        return userSessions;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        List<UserSessionEntity> userSessionEntities = new LinkedList<UserSessionEntity>();
        for (ClientSessionEntity s : clientSessions.values()) {
            String realmId = realm.getId();
            String clientId = client.getId();
            if (s.getSession() != null && s.getSession().getRealm().equals(realmId) && s.getClientId().equals(clientId)) {
                if (!userSessionEntities.contains(s.getSession())) {
                    userSessionEntities.add(s.getSession());
                }
            }
        }

        List<UserSessionModel> userSessions = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : userSessionEntities) {
            userSessions.add(new UserSessionAdapter(session, this, realm, e));
        }
        Collections.sort(userSessions, new UserSessionSort());
        return userSessions;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        List<UserSessionModel> userSessions = getUserSessions(realm, client);
        if (firstResult > userSessions.size()) {
            return Collections.emptyList();
        }

        int toIndex = (firstResult + maxResults) < userSessions.size() ? firstResult + maxResults : userSessions.size();
        return userSessions.subList(firstResult, toIndex);
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client).size();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        UserSessionEntity entity = getUserSessionEntity(realm, session.getId());
        if (entity != null) {
            userSessions.remove(entity.getId());
            remove(entity);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        Iterator<UserSessionEntity> itr = userSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && s.getUser().equals(user.getId())) {
                itr.remove();
                remove(s);
            }
        }
    }

    protected void remove(UserSessionEntity s) {
        if (s.getBrokerSessionId() != null) {
            userSessionsByBrokerSessionId.remove(s.getBrokerSessionId());
        }
        if (s.getBrokerUserId() != null) {
            Set<String> set = userSessionsByBrokerUserId.get(s.getBrokerUserId());
            if (set != null) {
                synchronized (set) {
                    set.remove(s.getId());
                    // this is a race condition :(
                    // Since it will be very rare for a user to have concurrent sessions, I'm hoping we never hit this
                    if (set.isEmpty()) userSessionsByBrokerUserId.remove(s.getBrokerUserId());
                }
            }
        }
        for (ClientSessionEntity clientSession : s.getClientSessions()) {
           clientSessions.remove(clientSession.getId());
       }
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        Iterator<UserSessionEntity> itr = userSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && (s.getLastSessionRefresh() < Time.currentTime() - realm.getSsoSessionIdleTimeout() || s.getStarted() < Time.currentTime() - realm.getSsoSessionMaxLifespan())) {
                itr.remove();

                remove(s);
            }
        }
        int expired = Time.currentTime() - RealmInfoUtil.getDettachedClientSessionLifespan(realm);
        Iterator<ClientSessionEntity> citr = clientSessions.values().iterator();
        while (citr.hasNext()) {
            ClientSessionEntity c = citr.next();
            if (c.getSession() == null && c.getRealmId().equals(realm.getId()) && c.getTimestamp() < expired) {
                citr.remove();
            }
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        Iterator<UserSessionEntity> itr = userSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId())) {
                itr.remove();

                remove(s);
            }
        }
        Iterator<ClientSessionEntity> citr = clientSessions.values().iterator();
        while (citr.hasNext()) {
            ClientSessionEntity c = citr.next();
            if (c.getSession() == null && c.getRealmId().equals(realm.getId())) {
                citr.remove();
            }
        }
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username) {
        UsernameLoginFailureEntity entity = loginFailures.get(new UsernameLoginFailureKey(realm.getId(), username));
        return entity != null ? new UsernameLoginFailureAdapter(entity) : null;
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username) {
        UsernameLoginFailureKey key = new UsernameLoginFailureKey(realm.getId(), username);
        UsernameLoginFailureEntity entity = new UsernameLoginFailureEntity(username, realm.getId());
        if (loginFailures.putIfAbsent(key, entity) != null) {
            throw new ModelDuplicateException();
        }
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String username) {
        loginFailures.remove(new UsernameLoginFailureKey(realm.getId(), username));
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        Iterator<UsernameLoginFailureEntity> itr = loginFailures.values().iterator();
        while (itr.hasNext()) {
            if (itr.next().getRealm().equals(realm.getId())) {
                itr.remove();
            }
        }

    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);
        removeAllUserLoginFailures(realm);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        for (ClientSessionEntity e : clientSessions.values()) {
            if (e.getRealmId().equals(realm.getId()) && e.getClientId().equals(client.getId())) {
                clientSessions.remove(e.getId());
                e.getSession().removeClientSession(e);
            }
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);

        loginFailures.remove(new UsernameLoginFailureKey(realm.getId(), user.getUsername()));
        loginFailures.remove(new UsernameLoginFailureKey(realm.getId(), user.getEmail()));
    }

    @Override
    public void close() {
    }

    private class UserSessionSort implements Comparator<UserSessionModel> {

        @Override
        public int compare(UserSessionModel o1, UserSessionModel o2) {
            int r = o1.getStarted() - o2.getStarted();
            if (r == 0) {
                return o1.getId().compareTo(o2.getId());
            } else {
                return r;
            }
        }
    }

}
