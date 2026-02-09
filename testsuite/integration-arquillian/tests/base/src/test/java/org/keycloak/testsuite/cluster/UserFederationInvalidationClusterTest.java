package org.keycloak.testsuite.cluster;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;

import static org.junit.Assert.assertNull;

public class UserFederationInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<ComponentRepresentation, ComponentResource> {

    @Before
    public void setExcludedComparisonFields() {
    }

    @Override
    protected ComponentRepresentation createTestEntityRepresentation() {
        ComponentRepresentation comp = new ComponentRepresentation();
        comp.setName("comp_" + RandomStringUtils.randomAlphabetic(5));

        // The provider needs to implement ImportSynchronization to trigger UserStorageSyncManager#notifyToRefreshPeriodicSync
        comp.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
        comp.setProviderType(UserStorageProvider.class.getName());
        return comp;
    }

    protected ComponentsResource components(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).components();
    }

    @Override
    protected ComponentResource entityResource(ComponentRepresentation comp, ContainerInfo node) {
        return entityResource(comp.getId(), node);
    }

    @Override
    protected ComponentResource entityResource(String id, ContainerInfo node) {
        return components(node).component(id);
    }

    @Override
    protected ComponentRepresentation createEntity(ComponentRepresentation comp, ContainerInfo node) {
        comp.setParentId(getAdminClientFor(node).realm(testRealmName).toRepresentation().getId());
        try (Response response = components(node).add(comp)) {
            String id = ApiUtil.getCreatedId(response);
            comp.setId(id);
        }
        return readEntity(comp, node);
    }

    @Override
    protected ComponentRepresentation readEntity(ComponentRepresentation comp, ContainerInfo node) {
        ComponentRepresentation u = null;
        try {
            u = entityResource(comp, node).toRepresentation();
        } catch (NotFoundException nfe) {
            // expected when component doesn't exist
        }
        return u;
    }

    @Override
    protected ComponentRepresentation updateEntity(ComponentRepresentation comp, ContainerInfo node) {
        entityResource(comp, node).update(comp);
        return readEntity(comp, node);
    }

    @Override
    protected void deleteEntity(ComponentRepresentation comp, ContainerInfo node) {
        entityResource(comp, node).remove();
        assertNull(readEntity(comp, node));
    }

    @Override
    protected ComponentRepresentation testEntityUpdates(ComponentRepresentation comp, boolean backendFailover) {
        comp.setName(comp.getName() + "_updated");
        comp = updateEntityOnCurrentFailNode(comp, "name");
        verifyEntityUpdateDuringFailover(comp, backendFailover);

        return comp;
    }

    @Override
    protected void assertEntityOnSurvivorNodesEqualsTo(ComponentRepresentation testEntityOnFailNode) {
        super.assertEntityOnSurvivorNodesEqualsTo(testEntityOnFailNode);
    }

}
