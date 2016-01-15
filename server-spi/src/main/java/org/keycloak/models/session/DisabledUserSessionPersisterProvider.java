package org.keycloak.models.session;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

/**
 * Persistence of userSessions is disabled . Useful just if you never need survive of userSessions/clientSessions
 * among server restart. Offline sessions / offline tokens will be invalid after server restart as well,
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DisabledUserSessionPersisterProvider implements UserSessionPersisterProviderFactory, UserSessionPersisterProvider {

    public static final String ID = "disabled";

    @Override
    public UserSessionPersisterProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void createUserSession(UserSessionModel userSession, boolean offline) {

    }

    @Override
    public void createClientSession(ClientSessionModel clientSession, boolean offline) {

    }

    @Override
    public void updateUserSession(UserSessionModel userSession, boolean offline) {

    }

    @Override
    public void removeUserSession(String userSessionId, boolean offline) {

    }

    @Override
    public void removeClientSession(String clientSessionId, boolean offline) {

    }

    @Override
    public void onRealmRemoved(RealmModel realm) {

    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {

    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {

    }

    @Override
    public void clearDetachedUserSessions() {

    }

    @Override
    public void updateAllTimestamps(int time) {

    }

    @Override
    public List<UserSessionModel> loadUserSessions(int firstResult, int maxResults, boolean offline) {
        return Collections.emptyList();
    }

    @Override
    public int getUserSessionsCount(boolean offline) {
        return 0;
    }
}
