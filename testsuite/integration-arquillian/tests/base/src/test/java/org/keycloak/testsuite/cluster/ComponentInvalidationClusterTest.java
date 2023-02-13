package org.keycloak.testsuite.cluster;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.components.amphibian.TestAmphibianProvider;
import org.keycloak.testsuite.components.amphibian.TestAmphibianProviderFactoryImpl;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Arrays;

import java.util.Map;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNull;

/**
 *
 * @author tkyjovsk
 */
public class ComponentInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<ComponentRepresentation, ComponentResource> {

    @Before
    public void setExcludedComparisonFields() {
    }

    @Override
    protected ComponentRepresentation createTestEntityRepresentation() {
        ComponentRepresentation comp = new ComponentRepresentation();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        comp.setName("comp_" + RandomStringUtils.randomAlphabetic(5));

        comp.setProviderId(TestAmphibianProviderFactoryImpl.PROVIDER_ID);
        comp.setProviderType(TestAmphibianProvider.class.getName());

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

        for (ContainerInfo ci : suiteContext.getAuthServerBackendsInfo()) {
            assertComponentHasCorrectConfig(comp, ci);
        }

        iterateCurrentFailNode();

        // config - add new
        comp.getConfig().putSingle("val3", "val3 value");
        comp = updateEntityOnCurrentFailNode(comp, "config - adding");

        for (ContainerInfo ci : suiteContext.getAuthServerBackendsInfo()) {
            assertComponentHasCorrectConfig(comp, ci);
        }

        iterateCurrentFailNode();

        // config - remove
        comp.getConfig().remove("val3");
        comp = updateEntityOnCurrentFailNode(comp, "config - removing");

        for (ContainerInfo ci : suiteContext.getAuthServerBackendsInfo()) {
            assertComponentHasCorrectConfig(comp, ci);
        }

        iterateCurrentFailNode();

        // config - update 1
        comp.getConfig().get("val1").set(0,
                comp.getConfig().get("val1").get(0) + " - updated");
        comp = updateEntityOnCurrentFailNode(comp, "config");

        for (ContainerInfo ci : suiteContext.getAuthServerBackendsInfo()) {
            assertComponentHasCorrectConfig(comp, ci);
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
        Map<String, Map<String, Object>> config = getTestingClientFor(survivorNode).testing(testRealmName).getTestAmphibianComponentDetails();
        
        assertThat(config, hasKey(testEntityOnFailNode.getName()));
        Map<String, Object> c = config.get(testEntityOnFailNode.getName());
        assertThat(c, hasEntry("number", Integer.valueOf(testEntityOnFailNode.getConfig().getFirst("number"))));
        assertThat(c, hasEntry("required", testEntityOnFailNode.getConfig().getFirst("required")));
        assertThat(c, hasEntry("val1", testEntityOnFailNode.getConfig().getFirst("val1")));
        assertThat(c, hasEntry("val2", testEntityOnFailNode.getConfig().getFirst("val2")));
        final Object val3 = testEntityOnFailNode.getConfig().getFirst("val3");
        if (val3 == null) {
            assertThat(c, anyOf(hasEntry("val3", null), not(hasKey("val3"))));
        } else {
            assertThat(c, hasEntry("val3", val3));
        }
    }

}
