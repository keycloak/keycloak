package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class RealmManager {

    private static RealmResource realm;

    private RealmManager() {
    }

    public static RealmManager realm(RealmResource realm) {
        RealmManager.realm = realm;
        return new RealmManager();
    }

    public RealmManager accessCodeLifeSpan(Integer accessCodeLifespan) {
        RealmRepresentation realmRepresentation = realm.toRepresentation();
        realmRepresentation.setAccessCodeLifespan(accessCodeLifespan);
        realm.update(realmRepresentation);
        return this;
    }

    public RealmManager verifyEmail(Boolean enabled) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setVerifyEmail(enabled);
        realm.update(rep);
        return this;
    }

    public RealmManager passwordPolicy(String passwordPolicy) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setPasswordPolicy(passwordPolicy);
        realm.update(rep);
        return this;
    }

    public RealmManager accessTokenLifespan(int accessTokenLifespan) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setAccessTokenLifespan(accessTokenLifespan);
        realm.update(rep);
        return this;
    }

    public RealmManager ssoSessionIdleTimeout(int sessionIdleTimeout) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setSsoSessionIdleTimeout(sessionIdleTimeout);
        realm.update(rep);
        return this;

    }

    public RealmManager revokeRefreshToken(boolean enable) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setRevokeRefreshToken(enable);
        realm.update(rep);
        return this;
    }
}