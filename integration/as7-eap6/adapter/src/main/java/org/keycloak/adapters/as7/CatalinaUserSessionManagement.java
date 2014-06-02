package org.keycloak.adapters.as7;

import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;
import org.keycloak.adapters.UserSessionManagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages relationship to users and sessions so that forced admin logout can be implemented
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaUserSessionManagement implements SessionListener, UserSessionManagement {
    private static final Logger log = Logger.getLogger(CatalinaUserSessionManagement.class);
    protected ConcurrentHashMap<String, UserSessions> userSessionMap = new ConcurrentHashMap<String, UserSessions>();
    protected ConcurrentHashMap<String, UserSessions> keycloakSessionMap = new ConcurrentHashMap<String, UserSessions>();

    public static class UserSessions {
        protected String user;
        protected long loggedIn = System.currentTimeMillis();
        protected Map<String, String>  keycloakSessionToHttpSession = new HashMap<String, String>();
        protected Map<String, String>  httpSessionToKeycloakSession = new HashMap<String, String>();
        protected Map<String, Session> sessions = new HashMap<String, Session>();
        public long getLoggedIn() {
            return loggedIn;
        }
    }

    public synchronized int getActiveSessions() {
        return keycloakSessionMap.size();
    }

    /**
     *
     * @param username
     * @return null if user not logged in
     */
    @Override
    public synchronized Long getUserLoginTime(String username) {
        UserSessions sessions = userSessionMap.get(username);
        if (sessions == null) return null;
        return sessions.getLoggedIn();
    }

    @Override
    public synchronized Set<String> getActiveUsers() {
        HashSet<String> set = new HashSet<String>();
        set.addAll(userSessionMap.keySet());
        return set;
    }


    public synchronized void login(Session session, String username, String keycloakSessionId) {
        String sessionId = session.getId();

        UserSessions sessions = userSessionMap.get(username);
        if (sessions == null) {
            sessions = new UserSessions();
            sessions.user = username;
            userSessionMap.put(username, sessions);
        }
        keycloakSessionMap.put(keycloakSessionId, sessions);
        sessions.httpSessionToKeycloakSession.put(sessionId, keycloakSessionId);
        sessions.keycloakSessionToHttpSession.put(keycloakSessionId, sessionId);
        sessions.sessions.put(sessionId, session);
        session.addSessionListener(this);
    }

    @Override
    public void logoutAll() {
        for (String user : userSessionMap.keySet()) logoutUser(user);
    }

    @Override
    public void logoutUser(String user) {
        log.debug("logoutUser: " + user);
        UserSessions sessions = null;
        sessions = userSessionMap.remove(user);
        if (sessions == null) {
            log.debug("no session for user: " + user);
            return;
        }
        log.debug("found session for user");
        for (Map.Entry<String, String> entry : sessions.httpSessionToKeycloakSession.entrySet()) {
            log.debug("invalidating session for user: " + user);
            String sessionId = entry.getKey();
            String keycloakSessionId = entry.getValue();
            Session session = sessions.sessions.get(sessionId);
            session.setPrincipal(null);
            session.setAuthType(null);
            session.getSession().invalidate();
            keycloakSessionMap.remove(keycloakSessionId);
        }
    }

    public synchronized void logoutKeycloakSession(String keycloakSessionId) {
        log.debug("logoutKeycloakSession: " + keycloakSessionId);
        UserSessions sessions = keycloakSessionMap.remove(keycloakSessionId);
        if (sessions == null) {
            log.debug("no session for keycloak session id: " + keycloakSessionId);
            return;
        }
        String sessionId = sessions.keycloakSessionToHttpSession.remove(keycloakSessionId);
        if (sessionId == null) {
            log.debug("no session for keycloak session id: " + keycloakSessionId);

        }
        sessions.httpSessionToKeycloakSession.remove(sessionId);
        Session session = sessions.sessions.remove(sessionId);
        session.setPrincipal(null);
        session.setAuthType(null);
        session.getSession().invalidate();
        if (sessions.keycloakSessionToHttpSession.size() == 0) {
            userSessionMap.remove(sessions.user);
        }
    }


    public void sessionEvent(SessionEvent event) {
        // We only care about session destroyed events
        if (!Session.SESSION_DESTROYED_EVENT.equals(event.getType())
                && (!Session.SESSION_PASSIVATED_EVENT.equals(event.getType())))
            return;

        // Look up the single session id associated with this session (if any)
        Session session = event.getSession();
        GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
        if (principal == null) return;
        session.setPrincipal(null);
        session.setAuthType(null);

        String username = principal.getUserPrincipal().getName();
        UserSessions userSessions = userSessionMap.get(username);
        if (userSessions == null) {
            return;
        }
        String sessionid = session.getId();
        synchronized (this) {
            String keycloakSessionId = userSessions.httpSessionToKeycloakSession.remove(sessionid);
            if (keycloakSessionId != null) {
                userSessions.keycloakSessionToHttpSession.remove(keycloakSessionId);
                keycloakSessionMap.remove(keycloakSessionId);
            }
            userSessions.sessions.remove(sessionid);
            if (userSessions.httpSessionToKeycloakSession.size() == 0) {
                userSessionMap.remove(username);
            }

        }
    }
}
