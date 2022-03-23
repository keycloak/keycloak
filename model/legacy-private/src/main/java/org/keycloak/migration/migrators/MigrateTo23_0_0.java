package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Arrays;
import java.util.List;

public class MigrateTo23_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("23.0.0");
    private static final List<String> DEFAULT_CLAIMS_SUPPORTED = Arrays.asList("aud", "sub", "iss", IDToken.AUTH_TIME, IDToken.NAME, IDToken.GIVEN_NAME, IDToken.FAMILY_NAME, IDToken.PREFERRED_USERNAME, IDToken.EMAIL, IDToken.ACR);

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> realm.setClaimsSupported(DEFAULT_CLAIMS_SUPPORTED));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        realm.setClaimsSupported(DEFAULT_CLAIMS_SUPPORTED);
    }


    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
