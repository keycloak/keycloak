package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.SessionManager;
import org.jboss.logging.Logger;
import org.keycloak.adapters.UserSessionManagement;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JettyUserSessionManagement implements UserSessionManagement {
    private static final org.jboss.logging.Logger log = Logger.getLogger(JettyUserSessionManagement.class);
    protected SessionManager sessionManager;

    public JettyUserSessionManagement(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void logoutAll() {
        // todo not implemented yet
    }

    @Override
    public void logoutHttpSessions(List<String> ids) {
        log.trace("---> logoutHttpSessions");
        for (String id : ids) {
            HttpSession httpSession = sessionManager.getHttpSession(id);
            if (httpSession != null) httpSession.invalidate();
        }

    }
}
