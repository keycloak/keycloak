package org.keycloak.testsuite.authz;

import org.junit.BeforeClass;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;

/**
 * @author mhajas
 */
public abstract class AbstractAuthzTest extends AbstractKeycloakTest {

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumePreview();
    }
}
