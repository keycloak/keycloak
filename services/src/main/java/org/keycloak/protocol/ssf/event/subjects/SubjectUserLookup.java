package org.keycloak.protocol.ssf.event.subjects;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class SubjectUserLookup {

    protected static final Logger log = Logger.getLogger(SubjectUserLookup.class);

    public static UserModel lookupUser(KeycloakSession session, RealmModel realm, SubjectId subjectId) {

        if (subjectId instanceof EmailSubjectId) {
            return getUserByEmail(session, realm, ((EmailSubjectId) subjectId).getEmail());
        }

        if (subjectId instanceof OpaqueSubjectId) {
            return getUserById(session, realm, ((OpaqueSubjectId) subjectId).getId());
        }

        if (subjectId instanceof IssuerSubjectId) {
            var issuerSubjectId = (IssuerSubjectId) subjectId;
            return getUserByIssuerSub(session, realm, issuerSubjectId.getIss(), issuerSubjectId.getSub());
        }

        log.warnf("Lookup failed for unknown subject id type. subjectId=%s", subjectId);
        return null;
    }

    private static UserModel getUserByIssuerSub(KeycloakSession session, RealmModel realm, String iss, String sub) {

        String realmIssuer = "http://localhost:18080/auth/realms/ssf-demo";
        // TODO fixme cannot create current realmIssuer in async call context
        // Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
        if (realmIssuer.equals(iss)) {
            return getUserById(session, realm, sub);
        }

        // TODO lookup user by identity provider links
        return null;
    }

    private static UserModel getUserById(KeycloakSession session, RealmModel realm, String userId) {
        return session.users().getUserById(realm, userId);
    }

    private static UserModel getUserByEmail(KeycloakSession session, RealmModel realm, String email) {
        return session.users().getUserByEmail(realm, email);
    }
}
