package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionManager;
import org.keycloak.adapters.UserSessionManagement;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SessionManagementBridge implements UserSessionManagement {

    protected UndertowUserSessionManagement userSessionManagement;
    protected SessionManager sessionManager;

    public SessionManagementBridge(UndertowUserSessionManagement userSessionManagement, SessionManager sessionManager) {
        this.userSessionManagement = userSessionManagement;
        this.sessionManager = sessionManager;
    }

    @Override
    public int getActiveSessions() {
        return userSessionManagement.getActiveSessions();
    }

    @Override
    public Long getUserLoginTime(String username) {
        return userSessionManagement.getUserLoginTime(username);
    }

    @Override
    public Set<String> getActiveUsers() {
        return userSessionManagement.getActiveUsers();
    }

    @Override
    public void logoutAll() {
        userSessionManagement.logoutAll(sessionManager);
    }

    @Override
    public void logout(String user) {
        userSessionManagement.logout(sessionManager, user);
    }
}
