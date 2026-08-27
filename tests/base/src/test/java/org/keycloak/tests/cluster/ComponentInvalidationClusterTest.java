package org.keycloak.tests.cluster;

import java.util.Arrays;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.components.TestComponentProvider;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author tkyjovsk
 */
@KeycloakIntegrationTest(config = ClusterCustomProvidersServerConfig.class)
public class ComponentInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<ComponentRepresentation, ComponentResource> {

    @InjectRealm
    ManagedRealm managedRealm;

    @BeforeEach
    public void setExcludedComparisonFields() {
    }

    @Override
    protected ComponentRepresentation createTestEntityRepresentation() {
        ComponentRepresentation comp = new ComponentRepresentation();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        comp.setName("comp_" + RandomStringUtils.randomAlphabetic(5));

        comp.setProviderId("test-component");
        comp.setProviderType(TestComponentProvider.class.getName());

        config.putSingle("secret", "Secret");
        config.putSingle("required", "required-value");
        config.putSingle("number", "2");
        config.put("val1", Arrays.asList(new String[]{"val1 value"}));
        config.put("val2", Arrays.asList(new String[]{"val2 value"}));
        comp.setConfig(config);
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
            if (response.getStatus() != 201) {
                String body = response.readEntity(String.class);
                assertEquals(201, response.getStatus(), "Unable to create component: " + body);
            }
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

        // config - add new
        comp.getConfig().putSingle("val3", "val3 value");
        comp = updateEntityOnCurrentFailNode(comp, "config - adding");
        verifyEntityUpdateDuringFailover(comp, backendFailover);

        // config - remove
        comp.getConfig().remove("val3");
        comp = updateEntityOnCurrentFailNode(comp, "config - removing");
        verifyEntityUpdateDuringFailover(comp, backendFailover);

        // config - update 1
        comp.getConfig().get("val1").set(0,
                comp.getConfig().get("val1").get(0) + " - updated");
        comp = updateEntityOnCurrentFailNode(comp, "config");
        verifyEntityUpdateDuringFailover(comp, backendFailover);

        return comp;
    }

    @Test
    public void testComponentUpdating() {
        ComponentRepresentation testEntity = createTestEntityRepresentation();

        // CREATE
        log.info("(1) createEntityOnCurrentFailNode");
        ComponentRepresentation comp = createEntityOnCurrentFailNode(testEntity);

        for (int i = 0; i < getClusterSize(); i++) {
            assertComponentHasCorrectConfig(comp, backendNode(i));
        }

        iterateCurrentFailNode();

        // config - add new
        comp.getConfig().putSingle("val3", "val3 value");
        comp = updateEntityOnCurrentFailNode(comp, "config - adding");

        for (int i = 0; i < getClusterSize(); i++) {
            assertComponentHasCorrectConfig(comp, backendNode(i));
        }

        iterateCurrentFailNode();

        // config - remove
        comp.getConfig().remove("val3");
        comp = updateEntityOnCurrentFailNode(comp, "config - removing");

        for (int i = 0; i < getClusterSize(); i++) {
            assertComponentHasCorrectConfig(comp, backendNode(i));
        }

        iterateCurrentFailNode();

        // config - update 1
        comp.getConfig().get("val1").set(0,
                comp.getConfig().get("val1").get(0) + " - updated");
        comp = updateEntityOnCurrentFailNode(comp, "config");

        for (int i = 0; i < getClusterSize(); i++) {
            assertComponentHasCorrectConfig(comp, backendNode(i));
        }
    }

    @Override
    protected void assertEntityOnSurvivorNodesEqualsTo(ComponentRepresentation testEntityOnFailNode) {
        super.assertEntityOnSurvivorNodesEqualsTo(testEntityOnFailNode);

        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            assertComponentHasCorrectConfig(testEntityOnFailNode, survivorNode);
        }
    }

    protected void assertComponentHasCorrectConfig(ComponentRepresentation testEntityOnFailNode, ContainerInfo survivorNode) throws NumberFormatException {
        log.debug(String.format("Attempt to verify %s component reinstantiation on %s (%s)", getEntityType(testEntityOnFailNode), survivorNode, survivorNode.getContextRoot()));
        ComponentRepresentation componentOnNode = readEntity(testEntityOnFailNode, survivorNode);
        assertThat(componentOnNode.getConfig(), hasEntry("number", testEntityOnFailNode.getConfig().get("number")));
        assertThat(componentOnNode.getConfig(), hasEntry("required", testEntityOnFailNode.getConfig().get("required")));
        assertThat(componentOnNode.getConfig(), hasEntry("val1", testEntityOnFailNode.getConfig().get("val1")));
        assertThat(componentOnNode.getConfig(), hasEntry("val2", testEntityOnFailNode.getConfig().get("val2")));
        final var val3 = testEntityOnFailNode.getConfig().get("val3");
        if (val3 == null) {
            assertThat(componentOnNode.getConfig(), anyOf(hasEntry("val3", null), not(hasKey("val3"))));
        } else {
            assertThat(componentOnNode.getConfig(), hasEntry("val3", val3));
        }
    }

}
