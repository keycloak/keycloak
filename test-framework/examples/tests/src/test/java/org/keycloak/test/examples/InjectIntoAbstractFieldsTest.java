package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest
public class InjectIntoAbstractFieldsTest extends AbstractTest {

    @Test
    public void testManagedRealm() {
        Assertions.assertNotNull(realm);
    }

}
