package org.keycloak.tests.cluster;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author tkyjovsk
 */
@KeycloakIntegrationTest
public abstract class AbstractInvalidationClusterTestWithTestRealm<T, TR> extends AbstractInvalidationClusterTest<T, TR> {

    @InjectRealm
    ManagedRealm managedRealm;

    protected String testRealmName = null;
    
    @BeforeEach
    public void createTestRealm() {
        createTestRealm(frontendNode());
    }
    
    protected void createTestRealm(ContainerInfo node) {
        RealmRepresentation r = createTestRealmRepresentation();
        getAdminClientFor(node).realms().create(r);
        testRealmName = r.getRealm();
    }
    
}
