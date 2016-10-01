package org.keycloak.testsuite.cluster;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 *
 * @author tkyjovsk
 * @param <T> entity representation
 * @param <TR> entity resource
 */
public abstract class AbstractInvalidationClusterTest<T, TR> extends AbstractClusterTest {

    protected RealmRepresentation createTestRealmRepresentation() {
        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setRealm("test_" + RandomStringUtils.randomAlphabetic(5));
        testRealm.setEnabled(true);
        return testRealm;
    }

    protected abstract T createTestEntityRepresentation();

    @Test
    public void crudWithoutFailover() {
        crud(false);
    }

    @Test
    public void crudWithFailover() {
        crud(true);
    }

    public void crud(boolean backendFailover) {
        T testEntity = createTestEntityRepresentation();

        // CREATE 
        testEntity = createEntityOnCurrentFailNode(testEntity);

        if (backendFailover) {
            failure();
        }

        assertEntityOnSurvivorNodesEqualsTo(testEntity);

        failback();
        iterateCurrentFailNode();

        // UPDATE(s)
        testEntity = testEntityUpdates(testEntity, backendFailover);

        // DELETE 
        deleteEntityOnCurrentFailNode(testEntity);

        if (backendFailover) {
            failure();
        }

        assertEntityOnSurvivorNodesIsDeleted(testEntity);
    }

    protected abstract TR entityResource(T testEntity, ContainerInfo node);

    protected abstract TR entityResource(String idOrName, ContainerInfo node);
    
    protected abstract T createEntity(T testEntity, ContainerInfo node);

    protected abstract T readEntity(T entity, ContainerInfo node);

    protected abstract T updateEntity(T entity, ContainerInfo node);

    protected abstract void deleteEntity(T testEntity, ContainerInfo node);

    protected TR entityResourceOnCurrentFailNode(T testEntity) {
        return entityResource(testEntity, getCurrentFailNode());
    }

    protected String getEntityType(T entity) {
        return entity.getClass().getSimpleName().replace("Representation", "");
    }

    protected T createEntityOnCurrentFailNode(T entity) {
        log.info("Creating " + getEntityType(entity) + " on " + getCurrentFailNode());
        return createEntity(entity, getCurrentFailNode());
    }

    protected T readEntityOnCurrentFailNode(T entity) {
        log.debug("Reading " + getEntityType(entity) + " on " + getCurrentFailNode());
        return readEntity(entity, getCurrentFailNode());
    }

    protected T updateEntityOnCurrentFailNode(T entity) {
        return updateEntityOnCurrentFailNode(entity, "");
    }

    protected T updateEntityOnCurrentFailNode(T entity, String updateType) {
        log.info("Updating " + getEntityType(entity) + " " + updateType + " on " + getCurrentFailNode());
        return updateEntity(entity, getCurrentFailNode());
    }

    protected void deleteEntityOnCurrentFailNode(T entity) {
        log.info("Creating " + getEntityType(entity) + " on " + getCurrentFailNode());
        deleteEntity(entity, getCurrentFailNode());
    }

    protected abstract T testEntityUpdates(T testEntity, boolean backendFailover);

    protected void verifyEntityUpdateDuringFailover(T testEntity, boolean backendFailover) {
        if (backendFailover) {
            failure();
        }

        assertEntityOnSurvivorNodesEqualsTo(testEntity);

        failback();
        iterateCurrentFailNode();
    }

    protected List<String> excludedComparisonFields = new ArrayList<>();

    protected void assertEntityOnSurvivorNodesEqualsTo(T testEntityOnFailNode) {
        boolean entityDiffers = false;
        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            T testEntityOnSurvivorNode = readEntity(testEntityOnFailNode, survivorNode);
            if (EqualsBuilder.reflectionEquals(testEntityOnSurvivorNode, testEntityOnFailNode, excludedComparisonFields)) {
                log.info(String.format("Verification of %s on survivor %s PASSED", getEntityType(testEntityOnFailNode), survivorNode));
            } else {
                entityDiffers = true;
                log.error(String.format("Verification of %s on survivor %s FAILED", getEntityType(testEntityOnFailNode), survivorNode));
                String tf = ReflectionToStringBuilder.reflectionToString(testEntityOnFailNode, ToStringStyle.SHORT_PREFIX_STYLE);
                String ts = ReflectionToStringBuilder.reflectionToString(testEntityOnSurvivorNode, ToStringStyle.SHORT_PREFIX_STYLE);
                log.error(String.format(
                        "\nEntity on fail node: \n%s\n"
                        + "\nEntity on survivor node: \n%s\n"
                        + "\nDifference: \n%s\n",
                        tf, ts, StringUtils.difference(tf, ts)));
            }
        }
        assertFalse(entityDiffers);
    }

    private void assertEntityOnSurvivorNodesIsDeleted(T testEntityOnFailNode) {
        // check if deleted from all survivor nodes
        boolean entityExists = false;
        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            T testEntityOnSurvivorNode = readEntity(testEntityOnFailNode, survivorNode);
            if (testEntityOnSurvivorNode == null) {
                log.info(String.format("Verification of %s deletion on survivor %s PASSED", getEntityType(testEntityOnFailNode), survivorNode));
            } else {
                entityExists = true;
                log.error(String.format("Verification of %s deletion on survivor %s FAILED", getEntityType(testEntityOnFailNode), survivorNode));
            }
        }
        assertFalse(entityExists);
    }

}
