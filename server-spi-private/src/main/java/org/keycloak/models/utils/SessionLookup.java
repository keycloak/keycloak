package org.keycloak.models.utils;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 * Provides convenient entity lookups within a {@link KeycloakSession},
 * resolving entities by their IDs against the session's realm context.
 *
 * <p>Primarily used inside {@link KeycloakModelUtils#enlistAfterRollback}
 * to re-lookup entities from the outer session in a new independent transaction.</p>
 */
public class SessionLookup {

    private final KeycloakSession session;
    private final RealmModel realm;

    public SessionLookup(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    public KeycloakSession session() {
        return session;
    }

    public RealmModel realm() {
        return realm;
    }

    public UserSessionModel findUserSession(UserSessionModel outerSession) {
        if (outerSession == null) return null;
        if (outerSession.isOffline()) {
            return session.sessions().getOfflineUserSession(realm, outerSession.getId());
        }
        return session.sessions().getUserSession(realm, outerSession.getId());
    }

    public AuthenticatedClientSessionModel findClientSession(AuthenticatedClientSessionModel outerSession) {
        if (outerSession == null) return null;
        UserSessionModel us = findUserSession(outerSession.getUserSession());
        return us != null ? us.getAuthenticatedClientSessionByClient(outerSession.getClient().getId()) : null;
    }

    public RootAuthenticationSessionModel findRootAuthSession(AuthenticationSessionModel outerSession) {
        if (outerSession == null) return null;
        return session.authenticationSessions().getRootAuthenticationSession(realm, outerSession.getParentSession().getId());
    }
}
