package org.keycloak.test.tmp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.realm.ManagedRealm;

@KeycloakIntegrationTest
public class PlaceHolderTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void testHello() {
        Assertions.assertNotNull(realm);
    }


}
