package org.keycloak.adapters.tomcat7;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.realm.GenericPrincipal;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages relationship to users and sessions so that forced admin logout can be implemented
 *
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @version $Revision: 1 $
 */
public class CatalinaUserSessionManagement implements SessionListener {

    private static final Logger log = Logger.getLogger(""+CatalinaUserSessionManagement.class);

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
        log.fine("logoutHttpSessions: " + sessionIds);

        for (String sessionId : sessionIds) {
            logoutSession(sessionManager, sessionId);
        }
    }

    protected void logoutSession(Manager manager, String httpSessionId) {
        log.fine("logoutHttpSession: " + httpSessionId);

        Session session;
        try {
            session = manager.findSession(httpSessionId);
        } catch (IOException ioe) {
            log.warning("IO exception when looking for session " + httpSessionId);
            ioe.printStackTrace();
            return;
        }

        logoutSession(session);
    }

    protected void logoutSession(Session session) {
        try {
            session.expire();
        } catch (Exception e) {
            log.warning("Session not present or already invalidated.");
        }
    }

    public void sessionEvent(SessionEvent event) {
        // We only care about session destroyed events
        if (!Session.SESSION_DESTROYED_EVENT.equals(event.getType())
                && (!Session.SESSION_PASSIVATED_EVENT.equals(event.getType())))
            return;

        // Look up the single session id associated with this session (if any)
        Session session = event.getSession();
        log.fine("Session " + session.getId() + " destroyed");

        GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
        if (principal == null) return;
        session.setPrincipal(null);
        session.setAuthType(null);
    }
}
