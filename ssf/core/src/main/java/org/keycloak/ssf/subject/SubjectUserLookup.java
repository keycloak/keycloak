package org.keycloak.ssf.subject;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;

import org.jboss.logging.Logger;

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

        // iss = current realm issuer
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        String realmIssuer = Urls.realmIssuer(frontendUriInfo.getBaseUri(), session.getContext().getRealm().getName());
        if (realmIssuer.equals(iss)) {
            // Find realm user
            return getUserById(session, realm, sub);
        }

        if (session.identityProviders().count() == 0) {
            log.warnf("No identity providers configured for realm. realm=%s", realm.getName());
            return null;
        }

        // Find identity provider whose issuer matches the iss claim
        IdentityProviderModel idp = session.identityProviders().getAllStream(IdentityProviderQuery.userAuthentication())
                .filter(i -> iss.equals(i.getConfig().get(IdentityProviderModel.ISSUER)))
                .findFirst()
                .orElse(null);

        if (idp == null) {
            log.warnf("No identity provider found for issuer. iss=%s", iss);
            return null;
        }

        // Lookup user by federated identity link: the sub claim is the user ID at the external IdP
        FederatedIdentityModel federatedIdentity = new FederatedIdentityModel(idp.getAlias(), sub, null);
        UserModel user = session.users().getUserByFederatedIdentity(realm, federatedIdentity);
        if (user == null) {
            log.debugf("No user found for federated identity. idpAlias=%s sub=%s", idp.getAlias(), sub);
        }
        return user;
    }

    private static UserModel getUserById(KeycloakSession session, RealmModel realm, String userId) {
        return session.users().getUserById(realm, userId);
    }

    private static UserModel getUserByEmail(KeycloakSession session, RealmModel realm, String email) {
        return session.users().getUserByEmail(realm, email);
    }
}
