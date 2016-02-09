package org.keycloak.testsuite.cluster;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 *
 * @author tkyjovsk
 */
public class EntityInvalidationClusterTest extends AbstractTwoNodeClusterTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void realmCRUDWithoutFailover() {
        realmCRUD(TEST + "_wofo", false);
    }

    @Test
    public void realmCRUDWithFailover() {
        realmCRUD(TEST + "_wfo", true);
    }

    public void realmCRUD(String realm, boolean containerFailover) {
        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setRealm(realm);
        testRealm.setEnabled(true);

        // CREATE on node1
        backend1AdminClient().realms().create(testRealm);

        // check if created on node1
        RealmRepresentation testRealmOnBackend1 = backend1AdminClient().realms().realm(realm).toRepresentation();
        assertEquals(testRealmOnBackend1.getRealm(), testRealm.getRealm());
        if (containerFailover) {
            killBackend1();
        }

        // check if created on node2
        RealmRepresentation testRealmOnBackend2 = backend2AdminClient().realms().realm(realm).toRepresentation();
        assertEquals(testRealmOnBackend1.getId(), testRealmOnBackend2.getId());
        assertEquals(testRealmOnBackend1.getRealm(), testRealmOnBackend2.getRealm());

        failback();

        // UPDATE on node2
        String realmUpdated = realm + "_updated";
        testRealmOnBackend2.setRealm(realmUpdated);
        backend2AdminClient().realms().realm(realm).update(testRealmOnBackend2);
        if (containerFailover) {
            killBackend2();
        }
        // check if updated on node1
        testRealmOnBackend1 = backend1AdminClient().realms().realm(realmUpdated).toRepresentation();
        assertEquals(testRealmOnBackend1.getId(), testRealmOnBackend2.getId());
        assertEquals(testRealmOnBackend1.getRealm(), testRealmOnBackend2.getRealm());

        failback();

        // DELETE on node1
        backend1AdminClient().realms().realm(realmUpdated).remove();
        if (containerFailover) {
            killBackend1();
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

    @Test
    public void createRealmViaFrontend() {
        String realm = TEST + "_fe";

        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setRealm(realm);
        testRealm.setEnabled(true);

        // CREATE on frontend
        adminClient.realms().create(testRealm);

        // check if created on frontend
        RealmRepresentation testRealmOnFrontend = adminClient.realms().realm(realm).toRepresentation();
        assertEquals(testRealmOnFrontend.getRealm(), testRealm.getRealm());

        // check if created on node1
        RealmRepresentation testRealmOnBackend1 = backend1AdminClient().realms().realm(realm).toRepresentation();
        assertEquals(testRealmOnBackend1.getId(), testRealmOnFrontend.getId());
        assertEquals(testRealmOnBackend1.getRealm(), testRealmOnFrontend.getRealm());

        // check if created on node2
        RealmRepresentation testRealmOnBackend2 = backend2AdminClient().realms().realm(realm).toRepresentation();
        assertEquals(testRealmOnBackend2.getId(), testRealmOnFrontend.getId());
        assertEquals(testRealmOnBackend2.getRealm(), testRealmOnFrontend.getRealm());
    }
    
}
