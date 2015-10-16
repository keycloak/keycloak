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
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.compat.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UsernameLoginFailureEntity;
import org.keycloak.models.sessions.infinispan.compat.entities.UsernameLoginFailureKey;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.common.util.Time;

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

    private final ConcurrentHashMap<String, UserSessionEntity> offlineUserSessions;
    private final ConcurrentHashMap<String, ClientSessionEntity> offlineClientSessions;

    public MemUserSessionProvider(KeycloakSession session, ConcurrentHashMap<String, UserSessionEntity> userSessions, ConcurrentHashMap<String, String> userSessionsByBrokerSessionId,
                                  ConcurrentHashMap<String, Set<String>> userSessionsByBrokerUserId, ConcurrentHashMap<String, ClientSessionEntity> clientSessions,
                                  ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures,
                                  ConcurrentHashMap<String, UserSessionEntity> offlineUserSessions, ConcurrentHashMap<String, ClientSessionEntity> offlineClientSessions) {
        this.session = session;
        this.userSessions = userSessions;
        this.clientSessions = clientSessions;
        this.loginFailures = loginFailures;
        this.userSessionsByBrokerSessionId = userSessionsByBrokerSessionId;
        this.userSessionsByBrokerUserId = userSessionsByBrokerUserId;
        this.offlineUserSessions = offlineUserSessions;
        this.offlineClientSessions = offlineClientSessions;
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
        return getUserSessions(realm, client, false);
    }

    protected List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, boolean offline) {
        ConcurrentHashMap<String, ClientSessionEntity> clientSessions = offline ? this.offlineClientSessions : this.clientSessions;

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
        return getUserSessions(realm, client, firstResult, maxResults, false);
    }

    protected List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults, boolean offline) {
        List<UserSessionModel> userSessions = getUserSessions(realm, client, offline);
        if (firstResult > userSessions.size()) {
            return Collections.emptyList();
        }

        int toIndex = (firstResult + maxResults) < userSessions.size() ? firstResult + maxResults : userSessions.size();
        return userSessions.subList(firstResult, toIndex);
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, false).size();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        UserSessionEntity entity = getUserSessionEntity(realm, session.getId());
        if (entity != null) {
            userSessions.remove(entity.getId());
            remove(entity, false);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user, false);
    }

    protected void removeUserSessions(RealmModel realm, UserModel user, boolean offline) {
        Iterator<UserSessionEntity> itr = offline ? offlineUserSessions.values().iterator() : userSessions.values().iterator();

        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && s.getUser().equals(user.getId())) {
                itr.remove();
                remove(s, offline);
            }
        }
    }

    protected void remove(UserSessionEntity s, boolean offline) {
        if (offline) {
            for (ClientSessionEntity clientSession : s.getClientSessions()) {
                offlineClientSessions.remove(clientSession.getId());
            }
        } else {
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
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

        Iterator<UserSessionEntity> itr = userSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && (s.getLastSessionRefresh() < Time.currentTime() - realm.getSsoSessionIdleTimeout() || s.getStarted() < Time.currentTime() - realm.getSsoSessionMaxLifespan())) {
                itr.remove();

                remove(s, false);
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

        // Remove expired offline user sessions
        itr = offlineUserSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId()) && (s.getLastSessionRefresh() < Time.currentTime() - realm.getOfflineSessionIdleTimeout())) {
                itr.remove();
                remove(s, true);

                // propagate to persister
                persister.removeUserSession(s.getId(), true);
            }
        }

        // Remove expired offline client sessions
        citr = offlineClientSessions.values().iterator();
        while (citr.hasNext()) {
            ClientSessionEntity s = citr.next();
            if (s.getRealmId().equals(realm.getId()) && (s.getTimestamp() < Time.currentTime() - realm.getOfflineSessionIdleTimeout())) {
                citr.remove();

                // propagate to persister
                persister.removeClientSession(s.getId(), true);
            }
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        removeUserSessions(realm, false);
    }

    protected void removeUserSessions(RealmModel realm, boolean offline) {
        Iterator<UserSessionEntity> itr = offline ? offlineUserSessions.values().iterator() : userSessions.values().iterator();
        while (itr.hasNext()) {
            UserSessionEntity s = itr.next();
            if (s.getRealm().equals(realm.getId())) {
                itr.remove();
                remove(s, offline);
            }
        }
        Iterator<ClientSessionEntity> citr = offline ? offlineClientSessions.values().iterator() : clientSessions.values().iterator();
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
        ConcurrentHashMap<String, ClientSessionEntity> clientSessionsMap = offline ? offlineClientSessions : clientSessions;

        for (ClientSessionEntity e : clientSessionsMap.values()) {
            if (e.getRealmId().equals(realm.getId()) && e.getClientId().equals(client.getId())) {
                clientSessionsMap.remove(e.getId());
                e.getSession().removeClientSession(e);
            }
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user, true);
        removeUserSessions(realm, user, false);

        loginFailures.remove(new UsernameLoginFailureKey(realm.getId(), user.getUsername()));
        loginFailures.remove(new UsernameLoginFailureKey(realm.getId(), user.getEmail()));
    }


    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        UserSessionAdapter importedUserSession = importUserSession(userSession, true);

        // started and lastSessionRefresh set to current time
        int currentTime = Time.currentTime();
        importedUserSession.getEntity().setStarted(currentTime);
        importedUserSession.setLastSessionRefresh(currentTime);

        return importedUserSession;
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
        if (userSession.getNotes() != null) {
            entity.getNotes().putAll(userSession.getNotes());
        }
        entity.setRememberMe(userSession.isRememberMe());
        entity.setState(userSession.getState());
        entity.setUser(userSession.getUser().getId());

        entity.setStarted(userSession.getStarted());
        entity.setLastSessionRefresh(userSession.getLastSessionRefresh());

        ConcurrentHashMap<String, UserSessionEntity> sessionsMap = offline ? offlineUserSessions : userSessions;
        sessionsMap.put(userSession.getId(), entity);
        return new UserSessionAdapter(session, this, userSession.getRealm(), entity);
    }

    @Override
    public UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId) {
        UserSessionEntity entity = offlineUserSessions.get(userSessionId);
        if (entity != null && entity.getRealm().equals(realm.getId())) {
            return new UserSessionAdapter(session, this, realm, entity);
        } else {
            return null;
        }
    }

    @Override
    public void removeOfflineUserSession(RealmModel realm, String userSessionId) {
        UserSessionEntity entity = offlineUserSessions.get(userSessionId);
        if (entity != null && entity.getRealm().equals(realm.getId())) {
            offlineUserSessions.remove(entity);
            remove(entity, true);
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
    public ClientSessionAdapter importClientSession(ClientSessionModel clientSession, boolean offline) {

        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(clientSession.getId());
        entity.setRealmId(clientSession.getRealm().getId());

        entity.setAction(clientSession.getAction());
        entity.setAuthenticatorStatus(clientSession.getExecutionStatus());
        entity.setAuthMethod(clientSession.getAuthMethod());
        if (clientSession.getAuthenticatedUser() != null) {
            entity.setAuthUserId(clientSession.getAuthenticatedUser().getId());
        }
        entity.setClientId(clientSession.getClient().getId());
        if (clientSession.getNotes() != null) {
            entity.getNotes().putAll(clientSession.getNotes());
        }
        entity.setProtocolMappers(clientSession.getProtocolMappers());
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setRoles(clientSession.getRoles());
        entity.setTimestamp(clientSession.getTimestamp());

        if (clientSession.getUserSessionNotes() != null) {
            entity.getUserSessionNotes().putAll(clientSession.getUserSessionNotes());
        }

        ConcurrentHashMap<String, ClientSessionEntity> clientSessionsMap = offline ? offlineClientSessions : clientSessions;
        clientSessionsMap.put(clientSession.getId(), entity);
        return new ClientSessionAdapter(session, this, clientSession.getRealm(), entity);
    }

    @Override
    public ClientSessionModel getOfflineClientSession(RealmModel realm, String clientSessionId) {
        ClientSessionEntity entity = offlineClientSessions.get(clientSessionId);
        if (entity != null && entity.getRealmId().equals(realm.getId())) {
            return new ClientSessionAdapter(session, this, realm, entity);
        } else {
            return null;
        }
    }

    @Override
    public List<ClientSessionModel> getOfflineClientSessions(RealmModel realm, UserModel user) {
        List<ClientSessionModel> clientSessions = new LinkedList<>();
        for (UserSessionEntity s : this.offlineUserSessions.values()) {
            if (s.getRealm().equals(realm.getId()) && s.getUser().equals(user.getId())) {
                for (ClientSessionEntity cls : s.getClientSessions()) {
                    ClientSessionAdapter clAdapter = new ClientSessionAdapter(session, this, realm, cls);
                    clientSessions.add(clAdapter);
                }
            }
        }
        return clientSessions;
    }

    @Override
    public void removeOfflineClientSession(RealmModel realm, String clientSessionId) {
        ClientSessionEntity entity = offlineClientSessions.get(clientSessionId);
        if (entity != null && entity.getRealmId().equals(realm.getId())) {
            offlineClientSessions.remove(entity.getId());
            UserSessionEntity userSession = entity.getSession();
            userSession.removeClientSession(entity);
        }
    }

    @Override
    public int getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, true).size();
    }

    @Override
    public List<UserSessionModel> getOfflineUserSessions(RealmModel realm, ClientModel client, int first, int max) {
        return getUserSessions(realm, client, first, max, true);
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
