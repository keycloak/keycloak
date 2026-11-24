package org.keycloak.tests.cluster;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author tkyjovsk
 */
@KeycloakIntegrationTest
public abstract class AbstractInvalidationClusterTestWithTestRealm<T, TR> extends AbstractInvalidationClusterTest<T, TR> {

    protected String testRealmName = null;

    @BeforeEach
    public void createTestRealm() {
        createTestRealm(frontendNode());
    }

    @AfterEach
    public void removeTestRealm() {
        if (testRealmName == null) {
            return;
        }
        try {
            // Ensure a live node before cleanup; clustered server is shared/global across methods.
            failback();
            getAdminClientFor(frontendNode()).realm(testRealmName).remove();
        } catch (Exception e) {
            log.warnf(e, "Failed to remove test realm %s", testRealmName);
        } finally {
            testRealmName = null;
        }
    }

    protected void createTestRealm(ContainerInfo node) {
        RealmRepresentation r = createTestRealmRepresentation();
        getAdminClientFor(node).realms().create(r);
        testRealmName = r.getRealm();
    }

}
