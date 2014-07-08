package org.keycloak.models.sessions.mem;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.sessions.LoginFailure;
import org.keycloak.models.sessions.Session;
import org.keycloak.models.sessions.SessionProvider;
import org.keycloak.util.Time;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemSessionProvider implements SessionProvider {

    private final ConcurrentHashMap<SessionKey, SessionAdapter> sessions;
    private final ConcurrentHashMap<LoginFailureKey, LoginFailureAdapter> loginFailures;
    private DummyKeycloakTransaction tx;

    public MemSessionProvider(ConcurrentHashMap<SessionKey, SessionAdapter> sessions, ConcurrentHashMap<LoginFailureKey, LoginFailureAdapter> loginFailures) {
        this.sessions = sessions;
        this.loginFailures = loginFailures;
    }

    @Override
    public LoginFailure getUserLoginFailure(String username, String realm) {
        return loginFailures.get(new LoginFailureKey(username, realm));
    }

    @Override
    public LoginFailure addUserLoginFailure(String username, String realm) {
        LoginFailureKey key = new LoginFailureKey(username, realm);
        return loginFailures.putIfAbsent(key, new LoginFailureAdapter(username, realm));
    }

    @Override
    public List<LoginFailure> getAllUserLoginFailures(String realm) {
        List<LoginFailure> failures = new LinkedList<LoginFailure>();
        for (LoginFailureAdapter failure : loginFailures.values()) {
            if (failure.getRealm().equals(realm)) {
                failures.add(failure);
            }
        }
        return failures;
    }

    @Override
    public Session createUserSession(String realm, String id, String user, String ipAddress) {
        SessionAdapter adapter = new SessionAdapter();
        adapter.setRealm(realm);
        adapter.setId(id);
        adapter.setUser(user);
        adapter.setIpAddress(ipAddress);

        int currentTime = Time.currentTime();

        adapter.setStarted(currentTime);
        adapter.setLastSessionRefresh(currentTime);

        sessions.put(new SessionKey(realm, id), adapter);
        return adapter;
    }

    @Override
    public Session getUserSession(String id, String realm) {
        return sessions.get(new SessionKey(realm, id));
    }

    @Override
    public List<Session> getUserSessionsByUser(String user, String realm) {
        List<Session> userSessions = new LinkedList<Session>();
        for (SessionAdapter s : sessions.values()) {
            if (s.getRealm().equals(realm) && s.getUser().equals(user)) {
                userSessions.add(s);
            }
        }
        return userSessions;
    }

    @Override
    public Set<Session> getUserSessionsByClient(String realm, String client) {
        Set<Session> clientSessions = new HashSet<Session>();
        for (SessionAdapter s : sessions.values()) {
            if (s.getRealm().equals(realm) && s.getClientAssociations().contains(client)) {
                clientSessions.add(s);
            }
        }
        return clientSessions;
    }

    @Override
    public int getActiveUserSessions(String realm, String client) {
        int count = 0;
        for (SessionAdapter s : sessions.values()) {
            if (s.getRealm().equals(realm) && s.getClientAssociations().contains(client)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void removeUserSession(Session session) {
        String realm = ((SessionAdapter) session).getRealm();
        sessions.remove(new SessionKey(realm, session.getId()));
    }

    @Override
    public void removeUserSessions(String realm, String user) {
        Iterator<SessionAdapter> itr = sessions.values().iterator();
        while (itr.hasNext()) {
            SessionAdapter s = itr.next();
            if (s.getRealm().equals(realm) && s.getUser().equals(user)) {
                itr.remove();
            }
        }
    }

    @Override
    public void removeExpiredUserSessions(String realm, long refreshTimeout, long sessionTimeout) {
        Iterator<SessionAdapter> itr = sessions.values().iterator();
        while (itr.hasNext()) {
            SessionAdapter s = itr.next();
            if (s.getLastSessionRefresh() < refreshTimeout || s.getStarted() < sessionTimeout) {
                itr.remove();
            }
        }
    }

    @Override
    public void removeUserSessions(String realm) {
        Iterator<SessionAdapter> itr = sessions.values().iterator();
        while (itr.hasNext()) {
            SessionAdapter s = itr.next();
            if (s.getRealm().equals(realm)) {
                itr.remove();
            }
        }
    }

    @Override
    public KeycloakTransaction getTransaction() {
        if (tx == null) {
            tx = new DummyKeycloakTransaction();
        }
        return tx;
    }

    @Override
    public void close() {
    }

    public static class DummyKeycloakTransaction implements KeycloakTransaction {

        public boolean rollBackOnly;
        public boolean active;

        @Override
        public void begin() {
            this.active = true;
        }

        @Override
        public void commit() {
        }

        @Override
        public void rollback() {
        }

        @Override
        public void setRollbackOnly() {
            this.rollBackOnly = true;
        }

        @Override
        public boolean getRollbackOnly() {
            return rollBackOnly;
        }

        @Override
        public boolean isActive() {
            return active;
        }


    }

}
