package org.keycloak.models.sessions.mem;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.mem.entities.ClientSessionEntity;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;
import org.keycloak.models.sessions.mem.entities.UsernameLoginFailureEntity;
import org.keycloak.models.sessions.mem.entities.UsernameLoginFailureKey;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemUserSessionProvider implements UserSessionProvider {

    private final KeycloakSession session;
    private final ConcurrentHashMap<String, UserSessionEntity> userSessions;
    private final ConcurrentHashMap<String, ClientSessionEntity> clientSessions;
    private final ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures;

    public MemUserSessionProvider(KeycloakSession session, ConcurrentHashMap<String, UserSessionEntity> userSessions, ConcurrentHashMap<String, ClientSessionEntity> clientSessions, ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures) {
        this.session = session;
        this.userSessions = userSessions;
        this.clientSessions = clientSessions;
        this.loginFailures = loginFailures;
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

        userSessions.put(id, entity);

        return new UserSessionAdapter(session, this, realm, entity);
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
            for (ClientSessionEntity clientSession : entity.getClientSessions()) {
                clientSessions.remove(clientSession.getId());
            }
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        Iterator<UserSessionEntity> itr = userSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && s.getUser().equals(user.getId())) {
                itr.remove();

                for (ClientSessionEntity clientSession : s.getClientSessions()) {
                    clientSessions.remove(clientSession.getId());
                }
            }
        }
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        Iterator<UserSessionEntity> itr = userSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && (s.getLastSessionRefresh() < Time.currentTime() - realm.getSsoSessionIdleTimeout() || s.getStarted() < Time.currentTime() - realm.getSsoSessionMaxLifespan())) {
                itr.remove();

                for (ClientSessionEntity clientSession : s.getClientSessions()) {
                    clientSessions.remove(clientSession.getId());
                }
            }
        }
        Iterator<ClientSessionEntity> citr = clientSessions.values().iterator();
        while (citr.hasNext()) {
            ClientSessionEntity c = citr.next();
            if (c.getSession() == null && c.getRealmId().equals(realm.getId()) && c.getTimestamp() < Time.currentTime() - realm.getSsoSessionIdleTimeout()) {
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

                for (ClientSessionEntity clientSession : s.getClientSessions()) {
                    clientSessions.remove(clientSession.getId());
                }
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
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);

        Iterator<UsernameLoginFailureEntity> itr = loginFailures.values().iterator();
        while (itr.hasNext()) {
            if (itr.next().getRealm().equals(realm.getId())) {
                itr.remove();
            }
        }
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
