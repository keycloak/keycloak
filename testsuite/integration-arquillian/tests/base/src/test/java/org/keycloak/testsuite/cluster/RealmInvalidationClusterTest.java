package org.keycloak.testsuite.cluster;

import javax.ws.rs.NotFoundException;
import static org.junit.Assert.assertNull;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;

/**
 *
 * @author tkyjovsk
 */
public class RealmInvalidationClusterTest extends AbstractInvalidationClusterTest<RealmRepresentation> {

    @Override
    protected RealmRepresentation createTestEntityRepresentation() {
        return createTestRealmRepresentation();
    }

    @Override
    protected RealmRepresentation createEntity(RealmRepresentation realm, ContainerInfo node) {
        log.info("Creating realm on : " + getCurrentFailNode());
        getAdminClientFor(getCurrentFailNode()).realms().create(realm);
        // get created entity
        return readEntity(realm, node);
    }

    @Override
    protected RealmRepresentation readEntity(RealmRepresentation realm, ContainerInfo node) {
        RealmRepresentation realmOnNode = null;
        try {
            realmOnNode = getAdminClientFor(node).realm(realm.getRealm()).toRepresentation();
        } catch (NotFoundException nfe) {
            // expected if realm not found
        }
        return realmOnNode;
    }

    @Override
    protected RealmRepresentation updateEntity(RealmRepresentation realm, ContainerInfo node) {
        getAdminClientFor(node).realms().realm(realm.getRealm()).update(realm);
        return readEntity(realm, node);
    }

    @Override
    protected void deleteEntity(RealmRepresentation realm, ContainerInfo node) {
        log.info("Deleting realm on: " + getCurrentFailNode());
        getAdminClientFor(node).realms().realm(realm.getRealm()).remove();
        // check if deleted
        assertNull(readEntity(realm, node));
    }

    @Override
    protected RealmRepresentation testEntityUpdates(RealmRepresentation realm, boolean backendFailover) {

        realm = updateRealmName(realm, realm.getRealm() + "_updated");
        verifyEntityUpdateDuringFailover(realm, backendFailover);

        realm = updateRealmEnabled(realm);
        verifyEntityUpdateDuringFailover(realm, backendFailover);
        
        return realm;
    }

    protected RealmRepresentation updateRealmName(RealmRepresentation realm, String newName) {
        log.info("Updating realm on: " + getCurrentFailNode());
        String originalName = realm.getRealm();
        realm.setRealm(newName);
        
        getAdminClientFor(getCurrentFailNode()).realms().realm(originalName).update(realm);
        return readEntity(realm, getCurrentFailNode());
    }

    protected RealmRepresentation updateRealmEnabled(RealmRepresentation realm) {
        log.info("Updating realm on: " + getCurrentFailNode());
        realm.setEnabled(!realm.isEnabled());
        return updateEntity(realm, getCurrentFailNode());
    }

}
