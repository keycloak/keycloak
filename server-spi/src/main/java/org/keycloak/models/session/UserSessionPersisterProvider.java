package org.keycloak.models.session;

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserSessionPersisterProvider extends Provider {

    // Persist just userSession. Not it's clientSessions
    void createUserSession(UserSessionModel userSession, boolean offline);

    // Assuming that corresponding userSession is already persisted
    void createClientSession(ClientSessionModel clientSession, boolean offline);

    void updateUserSession(UserSessionModel userSession, boolean offline);

    // Called during logout (for online session) or during periodic expiration. It will remove all corresponding clientSessions too
    void removeUserSession(String userSessionId, boolean offline);

    // Called during revoke. It will remove userSession too if this was last clientSession attached to it
    void removeClientSession(String clientSessionId, boolean offline);

    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);
    void onUserRemoved(RealmModel realm, UserModel user);

    // Called at startup to remove userSessions without any clientSession
    void clearDetachedUserSessions();

    // Update "lastSessionRefresh" of all userSessions and "timestamp" of all clientSessions to specified time
    void updateAllTimestamps(int time);

    // Called during startup. For each userSession, it loads also clientSessions
    List<UserSessionModel> loadUserSessions(int firstResult, int maxResults, boolean offline);

    int getUserSessionsCount(boolean offline);

}
