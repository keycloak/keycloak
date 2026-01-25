package org.keycloak.tests.admin.realm;

import org.junit.jupiter.api.Test;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmSearchTest extends AbstractRealmTest {

    @Test
    public void testSearchRealmByName() {
        String realmName1 = "testSearchRealmA";
        String realmName2 = "testSearchRealmB";
        String realmName3 = "anotherRealmC";

        // Create realms
        RealmRepresentation rep1 = new RealmRepresentation();
        rep1.setRealm(realmName1);
        adminClient.realms().create(rep1);

        RealmRepresentation rep2 = new RealmRepresentation();
        rep2.setRealm(realmName2);
        adminClient.realms().create(rep2);

        RealmRepresentation rep3 = new RealmRepresentation();
        rep3.setRealm(realmName3);
        adminClient.realms().create(rep3);

        try {
            runOnServer.run(session -> {
                RealmProvider realmProvider = session.realms();

                List<String> realmNames = realmProvider.getRealmsStream("testSearch")
                        .map(RealmModel::getName)
                        .collect(Collectors.toList());

                assertTrue(realmNames.contains(realmName1), "Should find realm1");
                assertTrue(realmNames.contains(realmName2), "Should find realm2");
                assertFalse(realmNames.contains(realmName3), "Should not find realm3");

                List<String> realmNames2 = realmProvider.getRealmsStream("anotherRealmC")
                        .map(RealmModel::getName)
                        .collect(Collectors.toList());

                assertTrue(realmNames2.contains(realmName3), "Should find realm3");
            });
        } finally {
            adminClient.realm(realmName1).remove();
            adminClient.realm(realmName2).remove();
            adminClient.realm(realmName3).remove();
        }
    }

    @Test
    public void testSearchRealmByDisplayName() {
        String realmName1 = "testSearchRealm1";
        String realmName2 = "testSearchRealm2";
        String realmName3 = "testSearchRealm3";

        String displayName1 = "Unique Display Name ABC";
        String displayName2 = "Different Name XYZ";
        String displayName3 = "Another Unique Display";

        RealmRepresentation rep1 = new RealmRepresentation();
        rep1.setRealm(realmName1);
        rep1.setDisplayName(displayName1);
        adminClient.realms().create(rep1);

        RealmRepresentation rep2 = new RealmRepresentation();
        rep2.setRealm(realmName2);
        rep2.setDisplayName(displayName2);
        adminClient.realms().create(rep2);

        RealmRepresentation rep3 = new RealmRepresentation();
        rep3.setRealm(realmName3);
        rep3.setDisplayName(displayName3);
        adminClient.realms().create(rep3);

        try {
            runOnServer.run(session -> {
                RealmProvider realmProvider = session.realms();

                List<String> realmNames = realmProvider.getRealmsStream("Unique")
                        .map(RealmModel::getName)
                        .collect(Collectors.toList());

                assertTrue(realmNames.contains(realmName1));
                assertFalse(realmNames.contains(realmName2));
                assertTrue(realmNames.contains(realmName3));

                List<String> realmNames2 = realmProvider.getRealmsStream("XYZ")
                        .map(RealmModel::getName)
                        .collect(Collectors.toList());

                assertFalse(realmNames2.contains(realmName1));
                assertTrue(realmNames2.contains(realmName2));
                assertFalse(realmNames2.contains(realmName3));

                List<String> realmNames3 = realmProvider.getRealmsStream(realmName1)
                        .map(RealmModel::getName)
                        .collect(Collectors.toList());

                assertTrue(realmNames3.contains(realmName1));
            });
        } finally {
            adminClient.realm(realmName1).remove();
            adminClient.realm(realmName2).remove();
            adminClient.realm(realmName3).remove();
        }
    }
}
