package org.keycloak.adapters;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserSessionManagement {
    int getActiveSessions();

    Long getUserLoginTime(String username);

    Set<String> getActiveUsers();

    void logoutAll();

    void logoutUser(String user);

    void logoutKeycloakSession(String id);
}
