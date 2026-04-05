package org.keycloak.testsuite.cluster;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author tkyjovsk
 */
public class RealmInvalidationClusterTest extends AbstractInvalidationClusterTest<RealmRepresentation, RealmResource> {

    @Override
    protected RealmRepresentation createTestEntityRepresentation() {
        return createTestRealmRepresentation();
    }

    protected RealmsResource realms(ContainerInfo node) {
        return getAdminClientFor(node).realms();
    }

    @Override
    protected RealmResource entityResource(RealmRepresentation realm, ContainerInfo node) {
        return entityResource(realm.getRealm(), node);
    }

    @Override
    protected RealmResource entityResource(String name, ContainerInfo node) {
        return getAdminClientFor(node).realm(name);
    }

    @Override
    protected RealmRepresentation createEntity(RealmRepresentation realm, ContainerInfo node) {
        realms(node).create(realm);
        return readEntity(realm, node);
    }

    @Override
    protected RealmRepresentation readEntity(RealmRepresentation realm, ContainerInfo node) {
        RealmRepresentation realmOnNode = null;
        try {
            realmOnNode = entityResource(realm, node).toRepresentation();
        } catch (NotFoundException nfe) {
            // expected if realm not found
        }
        return realmOnNode;
    }

    @Override
    protected RealmRepresentation updateEntity(RealmRepresentation realm, ContainerInfo node) {
        return updateEntity(realm.getRealm(), realm, node);
    }

    private RealmRepresentation updateEntity(String realmName, RealmRepresentation realm, ContainerInfo node) {
        entityResource(realmName, node).update(realm);
        return readEntity(realm, node);
    }

    @Override
    protected void deleteEntity(RealmRepresentation realm, ContainerInfo node) {
        entityResource(realm, node).remove();
        // check if deleted
        assertNull(readEntity(realm, node));
    }

    @Override
    protected RealmRepresentation testEntityUpdates(RealmRepresentation realm, boolean backendFailover) {

        // realm name
        String originalName = realm.getRealm();
        realm.setRealm(realm.getRealm() + "_updated");
        realm = updateEntity(originalName, realm, getCurrentFailNode());
        verifyEntityUpdateDuringFailover(realm, backendFailover);

        // enabled
        realm.setEnabled(!realm.isEnabled());
        realm = updateEntityOnCurrentFailNode(realm, "enabled");
        verifyEntityUpdateDuringFailover(realm, backendFailover);

        // public key
        realm.setPublicKey("GENERATE");
        realm = updateEntityOnCurrentFailNode(realm, "public key");
        assertNotEquals("GENERATE", realm.getPublicKey());
        verifyEntityUpdateDuringFailover(realm, backendFailover);

        // require ssl
        realm.setSslRequired("all");
        realm = updateEntityOnCurrentFailNode(realm, "require ssl");
        verifyEntityUpdateDuringFailover(realm, backendFailover);

        // brute force detection
        realm.setBruteForceProtected(!realm.isBruteForceProtected());
        realm = updateEntityOnCurrentFailNode(realm, "brute force");
        verifyEntityUpdateDuringFailover(realm, backendFailover);

        // brute force detection - failure factor
        realm.setBruteForceProtected(true);
        realm.setFailureFactor(realm.getFailureFactor() + 1);
        realm = updateEntityOnCurrentFailNode(realm, "brute force failure factor");
        verifyEntityUpdateDuringFailover(realm, backendFailover);

        return realm;
    }

}
