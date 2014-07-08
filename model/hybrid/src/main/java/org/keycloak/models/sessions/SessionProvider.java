package org.keycloak.models.sessions;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface SessionProvider extends Provider {

    LoginFailure getUserLoginFailure(String username, String realm);

    LoginFailure addUserLoginFailure(String username, String realm);

    List<LoginFailure> getAllUserLoginFailures(String realm);

    Session createUserSession(String realm, String id, String user, String ipAddress);

    Session getUserSession(String id, String realm);

    List<Session> getUserSessionsByUser(String user, String realm);

    Set<Session> getUserSessionsByClient(String realm, String client);

    int getActiveUserSessions(String realm, String client);

    void removeUserSession(Session session);

    void removeUserSessions(String realm, String user);

    void removeExpiredUserSessions(String realm, long refreshTimeout, long sessionTimeout);

    void removeUserSessions(String realm);

    KeycloakTransaction getTransaction();

    void close();

}
