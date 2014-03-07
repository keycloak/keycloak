package org.keycloak.adapters.as7;

import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;

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
public class UserSessionManagement implements SessionListener {
    private static final Logger log = Logger.getLogger(UserSessionManagement.class);
    protected ConcurrentHashMap<String, UserSessions> userSessionMap = new ConcurrentHashMap<String, UserSessions>();

    public static class UserSessions {
        protected Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();
        protected long loggedIn = System.currentTimeMillis();


        public Map<String, Session> getSessions() {
            return sessions;
        }

        public long getLoggedIn() {
            return loggedIn;
        }
    }

    public int getActiveSessions() {
        int active = 0;
        synchronized (userSessionMap) {
            for (UserSessions sessions : userSessionMap.values()) {
                active += sessions.getSessions().size();
            }

        }
        return active;
    }

    /**
     *
     * @param username
     * @return null if user not logged in
     */
    public Long getUserLoginTime(String username) {
        UserSessions sessions = userSessionMap.get(username);
        if (sessions == null) return null;
        return sessions.getLoggedIn();
    }

    public Set<String> getActiveUsers() {
        HashSet<String> set = new HashSet<String>();
        set.addAll(userSessionMap.keySet());
        return set;
    }


    protected void login(Session session, String username) {
        synchronized (userSessionMap) {
            UserSessions userSessions = userSessionMap.get(username);
            if (userSessions == null) {
                userSessions = new UserSessions();
                userSessionMap.put(username, userSessions);
            }
            userSessions.getSessions().put(session.getId(), session);
        }
        session.addSessionListener(this);
    }

    public void logoutAll() {
        List<String> users = new ArrayList<String>();
        users.addAll(userSessionMap.keySet());
        for (String user : users) logout(user);
    }

    public void logout(String user) {
        log.debug("logoutUser: " + user);
        UserSessions sessions = null;
        synchronized (userSessionMap) {
            sessions = userSessionMap.remove(user);

        }
        if (sessions == null) {
            log.debug("no session for user: " + user);
            return;

        }
        log.debug("found session for user");
        for (Session session : sessions.getSessions().values()) {
            session.setPrincipal(null);
            session.setAuthType(null);
            session.getSession().invalidate();
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
        synchronized (userSessionMap) {
            UserSessions sessions = userSessionMap.get(username);
            if (sessions != null) {
                sessions.getSessions().remove(session.getId());
                if (sessions.getSessions().isEmpty()) {
                    userSessionMap.remove(username);
                }
            }
        }
    }
}
