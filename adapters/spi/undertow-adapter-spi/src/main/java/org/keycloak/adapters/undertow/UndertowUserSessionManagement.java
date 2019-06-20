/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages relationship to users and sessions so that forced admin logout can be implemented
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowUserSessionManagement implements SessionListener {
    private static final Logger log = Logger.getLogger(UndertowUserSessionManagement.class);
    protected volatile boolean registered;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void login(SessionManager manager) {
        if (!registered) {
            manager.registerSessionListener(this);
            registered = true;
        }
    }

    /**
     * This method runs the given runnable in the current thread if the session manager does not use distributed sessions,
     * or in a separate thread if it does. This is to work around:
     * <pre>
     *   org.infinispan.util.concurrent.TimeoutException: ISPN000299: Unable to acquire lock after 15 seconds for key SessionCreationMetaDataKey
     * </pre>
     * See https://issues.jboss.org/browse/KEYCLOAK-9822
     * @param r
     */
    private void workaroundIspnDeadlock(final SessionManager manager, Runnable r) {
        if (manager.getClass().getName().equals("org.wildfly.clustering.web.undertow.session.DistributableSessionManager")) {
            executor.submit(r);
        } else {
            r.run();
        }
    }

    public void logoutAll(final SessionManager manager) {
        final Set<String> allSessions = manager.getAllSessions();
        workaroundIspnDeadlock(manager, new Runnable() {
            @Override
            public void run() {
                for (String sessionId : allSessions) logoutSession(manager, sessionId);
            }
        });
    }

    public void logoutHttpSessions(final SessionManager manager, final List<String> sessionIds) {
        log.debugf("logoutHttpSessions: %s", sessionIds);

        workaroundIspnDeadlock(manager, new Runnable() {
            @Override
            public void run() {
                for (String sessionId : sessionIds) {
                    logoutSession(manager, sessionId);
                }
            }
        });
    }

    protected void logoutSession(SessionManager manager, String httpSessionId) {
        log.debugf("logoutHttpSession: %s", httpSessionId);
        Session session = getSessionById(manager, httpSessionId);
        try {
            if (session != null) session.invalidate(null);
        } catch (Exception e) {
            log.warnf("Session %s not present or already invalidated.", httpSessionId);
        }
    }

    protected Session getSessionById(SessionManager manager, final String sessionId) {
        // TODO: Workaround for WFLY-3345. Remove this once we move to wildfly 8.2
        if (manager.getClass().getName().equals("org.wildfly.clustering.web.undertow.session.DistributableSessionManager")) {
            return manager.getSession(null, new SessionConfig() {

                @Override
                public void setSessionId(HttpServerExchange exchange, String sessionId) {
                }

                @Override
                public void clearSession(HttpServerExchange exchange, String sessionId) {
                }

                @Override
                public String findSessionId(HttpServerExchange exchange) {
                    return sessionId;
                }

                @Override
                public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
                    return null;
                }

                @Override
                public String rewriteUrl(String originalUrl, String sessionId) {
                    return null;
                }

            });

        } else {
            return manager.getSession(sessionId);
        }
    }


    @Override
    public void sessionCreated(Session session, HttpServerExchange exchange) {
    }

    @Override
    public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
    }


    @Override
    public void sessionIdChanged(Session session, String oldSessionId) {
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
