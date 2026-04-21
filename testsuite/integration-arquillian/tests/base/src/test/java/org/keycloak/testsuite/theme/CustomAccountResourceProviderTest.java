package org.keycloak.testsuite.theme;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class CustomAccountResourceProviderTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void testProviderOverride() {
        testingClient.server().run(session -> {
            AccountResourceProvider arp = session.getProvider(AccountResourceProvider.class, "ext-custom-account-console");
            Assertions.assertTrue(arp instanceof CustomAccountResourceProviderFactory);
        });
    }

}
