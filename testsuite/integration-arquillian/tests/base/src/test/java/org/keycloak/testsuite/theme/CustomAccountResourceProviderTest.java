package org.keycloak.testsuite.theme;

import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.theme.Theme;
import org.keycloak.representations.idm.RealmRepresentation;
import org.junit.Test;

public class CustomAccountResourceProviderTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void testProviderOverride() {
        testingClient.server().run(session -> {
            try {
              AccountResourceProvider arp = session.getProvider(AccountResourceProvider.class, "ext-custom-account-provider");
              Assert.assertTrue(arp instanceof CustomAccountResourceProviderFactory);
            } catch (IOException e) {
              Assert.fail(e.getMessage());
            }
        });
    }

}
