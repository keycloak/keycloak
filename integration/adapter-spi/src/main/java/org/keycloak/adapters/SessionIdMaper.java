package org.keycloak.adapters;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SessionIdMaper {
    Set<String> getUserSessions(String principal);

    String getSessionFromSSO(String sso);

    void map(String sso, String principal, String session);

    void removeSession(String session);
}
