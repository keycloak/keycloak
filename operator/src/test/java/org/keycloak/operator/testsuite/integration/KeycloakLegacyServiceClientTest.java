package org.keycloak.operator.testsuite.integration;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;

/**
 * Just temporary - once we use the new client service, we can remove this and the {@link KeycloakNewServiceClientTest}
 */
@Tag(BaseOperatorTest.SLOW)
@QuarkusTest
public class KeycloakLegacyServiceClientTest extends KeycloakClientTest {
}
