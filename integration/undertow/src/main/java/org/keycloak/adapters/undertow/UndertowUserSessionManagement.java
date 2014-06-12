/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticatedSessionManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
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
public class UndertowUserSessionManagement implements SessionListener {
    private static final Logger log = Logger.getLogger(UndertowUserSessionManagement.class);
    private static final String AUTH_SESSION_NAME = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
    protected ConcurrentHashMap<String, UserSessions> userSessionMap = new ConcurrentHashMap<String, UserSessions>();
    protected ConcurrentHashMap<String, UserSessions> keycloakSessionMap = new ConcurrentHashMap<String, UserSessions>();
    protected volatile boolean registered;


    public static class UserSessions {
        protected String user;
        protected long loggedIn = System.currentTimeMillis();
        protected Map<String, String>  keycloakSessionToHttpSession = new HashMap<String, String>();
        protected Map<String, String>  httpSessionToKeycloakSession = new HashMap<String, String>();
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
    public synchronized Long getUserLoginTime(String username) {
        UserSessions sessions = userSessionMap.get(username);
        if (sessions == null) return null;
        return sessions.getLoggedIn();
    }

    public synchronized Set<String> getActiveUsers() {
        HashSet<String> set = new HashSet<String>();
        set.addAll(userSessionMap.keySet());
        return set;
    }

    public synchronized void login(SessionManager manager, String sessionId, String username, String keycloakSessionId) {
        UserSessions sessions = userSessionMap.get(username);
        if (sessions == null) {
            sessions = new UserSessions();
            sessions.user = username;
            userSessionMap.put(username, sessions);
        }
        sessions.httpSessionToKeycloakSession.put(sessionId, keycloakSessionId);
        sessions.keycloakSessionToHttpSession.put(keycloakSessionId, sessionId);
        keycloakSessionMap.put(keycloakSessionId, sessions);
        if (!registered) {
            manager.registerSessionListener(this);
            registered = true;
        }
    }

    public synchronized void logoutAll(SessionManager manager) {
        for (String user : userSessionMap.keySet()) logoutUser(manager, user);
    }

    public synchronized void logoutUser(SessionManager manager, String user) {
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
            Session session = manager.getSession(sessionId);
            try {
                session.invalidate(null);
            } catch (Exception e) {
                log.warn("Session already invalidated.");
            }
            keycloakSessionMap.remove(keycloakSessionId);
        }
    }

    public synchronized void logoutKeycloakSession(SessionManager manager, String keycloakSessionId) {
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
        Session session = manager.getSession(sessionId);
        try {
            session.invalidate(null);
        } catch (Exception e) {
            log.warn("Session already invalidated.");
        }
        if (sessions.keycloakSessionToHttpSession.size() == 0) {
            userSessionMap.remove(sessions.user);
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
        UserSessions userSessions = userSessionMap.get(username);
        if (userSessions == null) {
            return;
        }
        synchronized (this) {
            String keycloakSessionId = userSessions.httpSessionToKeycloakSession.remove(sessionId);
            if (keycloakSessionId != null) {
                userSessions.keycloakSessionToHttpSession.remove(keycloakSessionId);
                keycloakSessionMap.remove(keycloakSessionId);
            }
            if (userSessions.httpSessionToKeycloakSession.size() == 0) {
                userSessionMap.remove(username);
            }

        }
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
        String sessionId = session.getId();

        UserSessions userSessions = userSessionMap.get(username);
        if (userSessions == null) {
            return;
        }

        synchronized (this) {
            String keycloakSessionId = userSessions.httpSessionToKeycloakSession.remove(oldSessionId);
            if (keycloakSessionId != null) {
                userSessions.keycloakSessionToHttpSession.remove(keycloakSessionId);
                userSessions.keycloakSessionToHttpSession.put(keycloakSessionId, sessionId);
                userSessions.httpSessionToKeycloakSession.put(sessionId, keycloakSessionId);
            }
        }
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
