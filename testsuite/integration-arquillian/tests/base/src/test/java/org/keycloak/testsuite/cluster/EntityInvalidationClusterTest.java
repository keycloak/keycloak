package org.keycloak.testsuite.cluster;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 *
 * @author tkyjovsk
 */
public class EntityInvalidationClusterTest extends AbstractClusterTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
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

        // check if created on backend nodes
        for (ContainerInfo backend : suiteContext.getAuthServerBackendsInfo()) {
            RealmRepresentation testRealmOnBackend = getAdminClientFor(backend).realms().realm(realm).toRepresentation();
            assertEquals(testRealmOnBackend.getId(), testRealmOnFrontend.getId());
            assertEquals(testRealmOnBackend.getRealm(), testRealmOnFrontend.getRealm());
        }
    }

    @Test
    public void realmCRUDWithoutFailover() {
        realmCRUD(TEST + "_wofo", false);
    }

    @Test
    public void realmCRUDWithFailover() {
        realmCRUD(TEST + "_wfo", true);
    }

    public void realmCRUD(String realmName, boolean backendFailover) {

        // CREATE on current fail node
        log.info("Creating realm on : " + getCurrentFailNode());
        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setRealm(realmName);
        testRealm.setEnabled(true);
        getAdminClientFor(getCurrentFailNode()).realms().create(testRealm);

        // check if created on fail node
        RealmRepresentation testRealmOnFailNode = getAdminClientFor(getCurrentFailNode()).realms().realm(realmName).toRepresentation();
        assertEquals(testRealmOnFailNode.getRealm(), testRealm.getRealm());

        if (backendFailover) {
            failure();
        }

        // check if created on survivor nodes
        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            RealmRepresentation testRealmOnSurvivorNode = getAdminClientFor(survivorNode).realms().realm(realmName).toRepresentation();
            assertEquals(testRealmOnFailNode.getId(), testRealmOnSurvivorNode.getId());
            assertEquals(testRealmOnFailNode.getRealm(), testRealmOnSurvivorNode.getRealm());
            log.info("Creation on survivor: " + survivorNode + " verified");
        }

        failback();
        iterateCurrentFailNode();

        // UPDATE on current fail node
        log.info("Updating realm on: " + getCurrentFailNode());
        String realmBeforeUpdate = realmName;
        realmName += "_updated";
        testRealm = testRealmOnFailNode;
        testRealm.setRealm(realmName);
        getAdminClientFor(getCurrentFailNode()).realms().realm(realmBeforeUpdate).update(testRealm);

        // check if updated on fail node
        testRealmOnFailNode = getAdminClientFor(getCurrentFailNode()).realms().realm(realmName).toRepresentation();
        assertEquals(testRealmOnFailNode.getRealm(), testRealm.getRealm());

        if (backendFailover) {
            failure();
        }

        // check if updated on survivor nodes
        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            RealmRepresentation testRealmOnSurvivorNode = getAdminClientFor(survivorNode).realms().realm(realmName).toRepresentation();
            assertEquals(testRealmOnFailNode.getId(), testRealmOnSurvivorNode.getId());
            assertEquals(testRealmOnFailNode.getRealm(), testRealmOnSurvivorNode.getRealm());
            log.info("Update on survivor: " + survivorNode + " verified");
        }

        failback();
        iterateCurrentFailNode();

        // DELETE on current fail node
        log.info("Deleting realm on: " + getCurrentFailNode());
        getAdminClientFor(getCurrentFailNode()).realms().realm(realmName).remove();

        if (backendFailover) {
            failure();
        }

        // check if deleted from all survivor nodes
        boolean realmStillExists = false;
        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            boolean realmStillExistsOnSurvivor = false;
            for (RealmRepresentation realmOnSurvivorNode : getAdminClientFor(survivorNode).realms().findAll()) {
                if (realmName.equals(realmOnSurvivorNode.getRealm())
                        || testRealmOnFailNode.getId().equals(realmOnSurvivorNode.getId())) {
                    realmStillExistsOnSurvivor = true;
                    realmStillExists = true;
                    break;
                }
            }
            log.error("Deletion on survivor: " + survivorNode + (realmStillExistsOnSurvivor ? " FAILED" : " verified"));
        }
        assertFalse(realmStillExists);
    }

}
