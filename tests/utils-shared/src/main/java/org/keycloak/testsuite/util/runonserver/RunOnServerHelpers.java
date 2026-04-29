package org.keycloak.testsuite.util.runonserver;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;

public final class RunOnServerHelpers {

    public static RunOnServer removeUserSession(String realmName, String sessionId) {
        return session -> {
            RealmModel realm = getRealmByName(session, realmName);

            UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
            if (sessionModel == null) {
                throw new NotFoundException("Session not found");
            }

            session.sessions().removeUserSession(realm, sessionModel);
        };
    }

    public static RunOnServer removeUserSessions(String realmName) {
        return session -> {
            RealmModel realm = getRealmByName(session, realmName);

            session.sessions().removeUserSessions(realm);
        };
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
