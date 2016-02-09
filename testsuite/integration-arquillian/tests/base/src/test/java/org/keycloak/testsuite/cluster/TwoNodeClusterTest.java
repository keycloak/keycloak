package org.keycloak.testsuite.cluster;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public class TwoNodeClusterTest extends AbstractClusterTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Before
    public void beforeTwoNodeClusterTest() {
        startBackendNodes(2);
        pause(3000);
    }

    @Test
    public void testRealmCRUDWithoutFailover() {
        testRealm(TEST + "_wofo", false);
    }

    @Test
    public void testRealmCRUDWithFailover() {
        testRealm(TEST + "_wfo", true);
    }

    public void testRealm(String realm, boolean containerFailover) {
        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setRealm(realm);
        testRealm.setEnabled(true);

        // CREATE on node1
        backend1AdminClient().realms().create(testRealm);

        // check if created on node1
        RealmRepresentation testRealmOnBackend1 = backend1AdminClient().realms().realm(realm).toRepresentation();
        assertEquals(testRealmOnBackend1.getRealm(), testRealm.getRealm());
        if (containerFailover) {
            stopBackend1();
        }

        // check if created on node2
        RealmRepresentation testRealmOnBackend2 = backend2AdminClient().realms().realm(realm).toRepresentation();
        assertEquals(testRealmOnBackend1.getId(), testRealmOnBackend2.getId());
        assertEquals(testRealmOnBackend1.getRealm(), testRealmOnBackend2.getRealm());

        failback();
        pause(1000);

        // UPDATE on node2
        String realmUpdated = realm + "_updated";
        testRealmOnBackend2.setRealm(realmUpdated);
        backend2AdminClient().realms().realm(realm).update(testRealmOnBackend2);
        if (containerFailover) {
            stopBackend2();
        }
        // check if updated on node1
        testRealmOnBackend1 = backend1AdminClient().realms().realm(realmUpdated).toRepresentation();
        assertEquals(testRealmOnBackend1.getId(), testRealmOnBackend2.getId());
        assertEquals(testRealmOnBackend1.getRealm(), testRealmOnBackend2.getRealm());

        failback();

        // DELETE on node1
        backend1AdminClient().realms().realm(realmUpdated).remove();
        if (containerFailover) {
            stopBackend1();
        }
        // check if deleted on node2
        boolean testRealmOnBackend2Exists = false;
        for (RealmRepresentation realmOnBackend2 : backend2AdminClient().realms().findAll()) {
            if (realmUpdated.equals(realmOnBackend2.getRealm())
                    || testRealmOnBackend1.getId().equals(realmOnBackend2.getId())) {
                testRealmOnBackend2Exists = true;
                break;
            }
        }
        assertFalse(testRealmOnBackend2Exists);
    }

    protected void listRealms(int i) {
        log.info(String.format("Node %s: AccessTokenString: %s", i + 1, backendAdminClients.get(i).tokenManager().getAccessTokenString()));
        for (RealmRepresentation r : backendAdminClients.get(i).realms().findAll()) {
            log.info(String.format("Node %s: Realm: %s, Id: %s", i + 1, r.getRealm(), r.getId()));
        }
    }

    protected ContainerInfo backend1Info() {
        return backendNode(0);
    }

    protected ContainerInfo backend2Info() {
        return backendNode(1);
    }

    protected Keycloak backend1AdminClient() {
        return backendAdminClients.get(0);
    }

    protected Keycloak backend2AdminClient() {
        return backendAdminClients.get(1);
    }

    protected void startBackend1() {
        startBackendNode(0);
    }

    protected void startBackend2() {
        startBackendNode(1);
    }

    protected void failback() {
        startBackend1();
        startBackend2();
    }

    protected void stopBackend1() {
        killBackendNode(0);
    }

    protected void stopBackend2() {
        killBackendNode(1);
    }

}
