package org.keycloak.protocol.ssf.event.subjects;

import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;

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

        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        String realmIssuer = Urls.realmIssuer(frontendUriInfo.getBaseUri(), session.getContext().getRealm().getName());
        // TODO fixme cannot create current realmIssuer in async call context
        if (realmIssuer.equals(iss)) {
            return getUserById(session, realm, sub);
        }

        // TODO lookup user by identity provider links via session.identityProviders()
        // session.users().getUserByFederatedIdentity(realm, new FederatedIdentityModel())
        return null;
    }

    private static UserModel getUserById(KeycloakSession session, RealmModel realm, String userId) {
        return session.users().getUserById(realm, userId);
    }

    private static UserModel getUserByEmail(KeycloakSession session, RealmModel realm, String email) {
        return session.users().getUserByEmail(realm, email);
    }
}
