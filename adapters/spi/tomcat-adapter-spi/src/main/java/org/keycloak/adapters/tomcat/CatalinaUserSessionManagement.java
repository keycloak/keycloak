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

package org.keycloak.adapters.tomcat;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Manages relationship to users and sessions so that forced admin logout can be implemented
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaUserSessionManagement implements SessionListener {
    private static final Logger log = Logger.getLogger(CatalinaUserSessionManagement.class);

    public void login(Session session) {
        session.addSessionListener(this);
    }

    public void logoutAll(Manager sessionManager) {
        Session[] allSessions = sessionManager.findSessions();
        for (Session session : allSessions) {
            logoutSession(session);
        }
    }

    public void logoutHttpSessions(Manager sessionManager, List<String> sessionIds) {
        log.debug("logoutHttpSessions: " + sessionIds);

        for (String sessionId : sessionIds) {
            logoutSession(sessionManager, sessionId);
        }
    }

    protected void logoutSession(Manager manager, String httpSessionId) {
        log.debug("logoutHttpSession: " + httpSessionId);

        Session session;
        try {
            session = manager.findSession(httpSessionId);
        } catch (IOException ioe) {
            log.warn("IO exception when looking for session " + httpSessionId, ioe);
            return;
        }

        logoutSession(session);
    }

    protected void logoutSession(Session session) {
        try {
            if (session != null) session.expire();
        } catch (Exception e) {
            log.debug("Session not present or already invalidated.", e);
        }
    }

    public void sessionEvent(SessionEvent event) {
        // We only care about session destroyed events
        if (!Session.SESSION_DESTROYED_EVENT.equals(event.getType()))
            return;

        // Look up the single session id associated with this session (if any)
        Session session = event.getSession();
        log.debugf("Session %s destroyed", session.getId());

        GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
        if (principal == null) return;
        session.setPrincipal(null);
        session.setAuthType(null);
    }
}
