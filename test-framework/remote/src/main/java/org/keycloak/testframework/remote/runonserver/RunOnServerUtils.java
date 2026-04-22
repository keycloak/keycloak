package org.keycloak.testframework.remote.runonserver;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserSessionModel;

public final class RunOnServerUtils {

    public static void removeUserSession(KeycloakSession session, String realmName, String sessionId) {
        RealmModel realm = getRealmByName(session, realmName);

        UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        session.sessions().removeUserSession(realm, sessionModel);
    }

    private static RealmModel getRealmByName(KeycloakSession session, String realmName) {
        RealmProvider realmProvider = session.getProvider(RealmProvider.class);
        RealmModel realm = realmProvider.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }
        return realm;
    }
}
