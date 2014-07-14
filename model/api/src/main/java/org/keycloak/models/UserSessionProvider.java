package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserSessionProvider extends Provider {

    UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress);
    UserSessionModel getUserSession(RealmModel realm, String id);
    List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults);
    int getActiveUserSessions(RealmModel realm, ClientModel client);
    void removeUserSession(RealmModel realm, UserSessionModel session);
    void removeUserSessions(RealmModel realm, UserModel user);
    void removeExpiredUserSessions(RealmModel realm);
    void removeUserSessions(RealmModel realm);

    UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username);
    UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username);
    List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm);

    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);
    void onUserRemoved(RealmModel realm, UserModel user);

    void close();

}
