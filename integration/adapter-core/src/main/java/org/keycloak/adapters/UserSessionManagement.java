package org.keycloak.adapters;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserSessionManagement {

    void logoutAll();

    void logoutHttpSessions(List<String> ids);
}
