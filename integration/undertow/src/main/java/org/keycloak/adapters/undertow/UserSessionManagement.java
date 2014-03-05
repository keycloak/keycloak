package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticatedSessionManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import io.undertow.util.StatusCodes;
import org.jboss.logging.Logger;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.util.JsonSerialization;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final String AUTH_SESSION_NAME = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
    protected ConcurrentHashMap<String, UserSessions> userSessionMap = new ConcurrentHashMap<String, UserSessions>();

    protected RealmConfiguration realmInfo;

    public UserSessionManagement(RealmConfiguration realmInfo) {
        this.realmInfo = realmInfo;
    }

    public static class UserSessions {
        protected Set<String> sessionIds = new HashSet<String>();
        protected long loggedIn = System.currentTimeMillis();

        public Set<String> getSessionIds() {
            return sessionIds;
        }

        public long getLoggedIn() {
            return loggedIn;
        }
    }

    public int getNumUserLogins() {
        return userSessionMap.size();
    }

    public int getActiveSessions() {
        int active = 0;
        synchronized (userSessionMap) {
            for (UserSessions sessions : userSessionMap.values()) {
                active += sessions.getSessionIds().size();
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

    public void remoteLogout(JWSInput token, SessionManager manager, HttpServletResponse response) throws IOException {
        try {
            log.info("->> remoteLogout: ");
            LogoutAction action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            if (action.isExpired()) {
                log.warn("admin request failed, expired token");
                response.sendError(StatusCodes.BAD_REQUEST, "Expired token");
                return;
            }
            if (!realmInfo.getMetadata().getResourceName().equals(action.getResource())) {
                log.warn("Resource name does not match");
                response.sendError(StatusCodes.BAD_REQUEST, "Resource name does not match");
                return;

            }
            String user = action.getUser();
            if (user != null) {
                log.info("logout of session for: " + user);
                logout(manager, user);
            } else {
                log.info("logout of all sessions");
                logoutAll(manager);
            }
        } catch (Exception e) {
            log.warn("failed to logout", e);
            response.sendError(StatusCodes.INTERNAL_SERVER_ERROR, "Failed to logout");
        }
        response.setStatus(StatusCodes.NO_CONTENT);
    }

    public void login(SessionManager manager, HttpSession session, String username) {
        String sessionId = session.getId();
        addAuthenticatedSession(username, sessionId);
        manager.registerSessionListener(this);
    }

    protected void addAuthenticatedSession(String username, String sessionId) {
        synchronized (userSessionMap) {
            UserSessions sessions = userSessionMap.get(username);
            if (sessions == null) {
                sessions = new UserSessions();
                userSessionMap.put(username, sessions);
            }
            sessions.getSessionIds().add(sessionId);
        }
    }

    protected void removeAuthenticatedSession(String sessionId, String username) {
        synchronized (userSessionMap) {
            UserSessions sessions = userSessionMap.get(username);
            if (sessions == null) return;
            sessions.getSessionIds().remove(sessionId);
            if (sessions.getSessionIds().isEmpty()) {
                userSessionMap.remove(username);
            }
        }
    }

    public void logoutAll(SessionManager manager) {
        List<String> users = new ArrayList<String>();
        users.addAll(userSessionMap.keySet());
        for (String user : users) logout(manager, user);
    }

    public void logoutAllBut(SessionManager manager, String but) {
        List<String> users = new ArrayList<String>();
        users.addAll(userSessionMap.keySet());
        for (String user : users) {
            if (!but.equals(user)) logout(manager, user);
        }
    }

    public void logout(SessionManager manager, String user) {
        log.info("logoutUser: " + user);
        UserSessions sessions = null;
        synchronized (userSessionMap) {
            sessions = userSessionMap.remove(user);
        }
        if (sessions == null) {
            log.info("no session for user: " + user);
            return;
        }
        log.info("found session for user");
        for (String id : sessions.getSessionIds()) {
            log.debug("invalidating session for user: " + user);
            Session session = manager.getSession(id);
            try {
                session.invalidate(null);
            } catch (Exception e) {
                log.warn("Session already invalidated.");
            }
        }
    }

    @Override
    public void sessionCreated(Session session, HttpServerExchange exchange) {
    }

    @Override
    public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
        // Look up the single session id associated with this session (if any)
        String username = getUsernameFromSession(session);
        if (username == null) return;
        String sessionId = session.getId();
        removeAuthenticatedSession(sessionId, username);
    }

    protected String getUsernameFromSession(Session session) {
        AuthenticatedSessionManager.AuthenticatedSession authSession = (AuthenticatedSessionManager.AuthenticatedSession) session.getAttribute(AUTH_SESSION_NAME);
        if (authSession == null) return null;
        return authSession.getAccount().getPrincipal().getName();

    }


    @Override
    public void sessionIdChanged(Session session, String oldSessionId) {
        String username = getUsernameFromSession(session);
        if (username == null) return;
        removeAuthenticatedSession(oldSessionId, username);
        addAuthenticatedSession(session.getId(), username);
    }

    @Override
    public void attributeAdded(Session session, String name, Object value) {
    }

    @Override
    public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
    }

    @Override
    public void attributeRemoved(Session session, String name, Object oldValue) {
    }

}
