package org.keycloak.models.sessions.mem;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;
import org.keycloak.models.sessions.mem.entities.UserSessionKey;
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
    private final ConcurrentHashMap<UserSessionKey, UserSessionEntity> sessions;
    private final ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures;

    public MemUserSessionProvider(KeycloakSession session, ConcurrentHashMap<UserSessionKey, UserSessionEntity> sessions, ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures) {
        this.session = session;
        this.sessions = sessions;
        this.loginFailures = loginFailures;
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress) {
        String id = KeycloakModelUtils.generateId();

        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setUser(user.getId());
        entity.setIpAddress(ipAddress);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        sessions.put(new UserSessionKey(realm.getId(), id), entity);

        return new UserSessionAdapter(session, realm, entity);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        UserSessionEntity entity = sessions.get(new UserSessionKey(realm.getId(), id));
        return entity != null ? new UserSessionAdapter(session, realm, entity) : null;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        List<UserSessionModel> userSessions = new LinkedList<UserSessionModel>();
        for (UserSessionEntity s : sessions.values()) {
            if (s.getRealm().equals(realm.getId()) && s.getUser().equals(user.getId())) {
                userSessions.add(new UserSessionAdapter(session, realm, s));
            }
        }
        return userSessions;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        List<UserSessionModel> clientSessions = new LinkedList<UserSessionModel>();
        for (UserSessionEntity s : sessions.values()) {
            if (s.getRealm().equals(realm.getId()) && s.getClients().contains(client.getClientId())) {
                clientSessions.add(new UserSessionAdapter(session, realm, s));
            }
        }
        Collections.sort(clientSessions, new UserSessionSort());
        return clientSessions;
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
        int count = 0;
        for (UserSessionEntity s : sessions.values()) {
            if (s.getRealm().equals(realm.getId()) && s.getClients().contains(client.getClientId())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        sessions.remove(new UserSessionKey(realm.getId(), session.getId()));
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        Iterator<UserSessionEntity> itr = sessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && s.getUser().equals(user.getId())) {
                itr.remove();
            }
        }
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        Iterator<UserSessionEntity> itr = sessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && (s.getLastSessionRefresh() < Time.currentTime() - realm.getSsoSessionIdleTimeout() || s.getStarted() < Time.currentTime() - realm.getSsoSessionMaxLifespan())) {
                itr.remove();
            }
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        Iterator<UserSessionEntity> itr = sessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId())) {
                itr.remove();
            }
        }
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username) {
        UsernameLoginFailureEntity entity = loginFailures.get(new UsernameLoginFailureKey(username, realm.getId()));
        return entity != null ? new UsernameLoginFailureAdapter(entity) : null;
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username) {
        UsernameLoginFailureKey key = new UsernameLoginFailureKey(username, realm.getId());
        return new UsernameLoginFailureAdapter(loginFailures.putIfAbsent(key, new UsernameLoginFailureEntity(username, realm.getId())));
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        List<UsernameLoginFailureModel> failures = new LinkedList<UsernameLoginFailureModel>();
        for (UsernameLoginFailureEntity entity : loginFailures.values()) {
            if (entity.getRealm().equals(realm.getId())) {
                failures.add(new UsernameLoginFailureAdapter(entity));
            }
        }
        return failures;
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
        Iterator<UserSessionEntity> itr = sessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId())) {
                itr.remove();
            }
        }

        for (UserSessionEntity s : sessions.values()) {
            if (s.getRealm().equals(realm.getId())) {
                s.getClients().remove(client.getClientId());
            }
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);

        loginFailures.remove(new UsernameLoginFailureKey(realm.getId(), user.getUsername()));
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
