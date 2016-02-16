package org.keycloak.testsuite.cluster;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractInvalidationClusterTest<T> extends AbstractClusterTest {

    private final SecureRandom random = new SecureRandom();

    protected String randomString(int length) {
        return new BigInteger(130, random).toString(length);
    }

    protected RealmRepresentation createTestRealmRepresentation() {
        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setRealm("test_" + randomString(5));
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

    protected abstract T createEntity(T testEntity, ContainerInfo node);

    protected abstract T readEntity(T entity, ContainerInfo node);

    protected abstract T updateEntity(T entity, ContainerInfo node);

    protected abstract void deleteEntity(T testEntity, ContainerInfo node);

    protected T createEntityOnCurrentFailNode(T testEntity) {
        return createEntity(testEntity, getCurrentFailNode());
    }

    protected T readEntityOnCurrentFailNode(T entity) {
        return readEntity(entity, getCurrentFailNode());
    }

    protected T updateEntityOnCurrentFailNode(T entity) {
        return updateEntity(entity, getCurrentFailNode());
    }

    protected void deleteEntityOnCurrentFailNode(T testEntity) {
        deleteEntity(testEntity, getCurrentFailNode());
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
                log.info("Verification on survivor " + survivorNode + " PASSED");
            } else {
                entityDiffers = true;
                log.error("Verification on survivor " + survivorNode + " FAILED");
                String tf = ReflectionToStringBuilder.reflectionToString(testEntityOnFailNode, ToStringStyle.SHORT_PREFIX_STYLE);
                String ts = ReflectionToStringBuilder.reflectionToString(testEntityOnSurvivorNode, ToStringStyle.SHORT_PREFIX_STYLE);
                log.error("\nEntity on fail node: \n\n" + tf + "\n"
                        + "\nEntity on survivor node: \n" + ts + "\n"
                        + "\nDifference: \n" + StringUtils.difference(tf, ts) + "\n");
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
                log.info("Verification of deletion on survivor " + survivorNode + " PASSED");
            } else {
                entityExists = true;
                log.error("Verification of deletion on survivor " + survivorNode + " FAILED");
            }
        }
        assertFalse(entityExists);
    }

}
