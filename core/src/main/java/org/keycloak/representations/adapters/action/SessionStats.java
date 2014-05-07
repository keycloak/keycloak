package org.keycloak.representations.adapters.action;

import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SessionStats {
    protected int activeSessions;
    protected int activeUsers;
    protected Map<String, UserStats> users;

    public int getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Map<String, UserStats> getUsers() {
        return users;
    }

    public void setUsers(Map<String, UserStats> users) {
        this.users = users;
    }
}
