package org.keycloak.tests.cluster;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;

import static org.keycloak.common.util.reflections.Reflections.resolveListType;
import static org.keycloak.common.util.reflections.Reflections.setAccessible;
import static org.keycloak.common.util.reflections.Reflections.unsetAccessible;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 *
 * @author tkyjovsk
 * @param <T> entity representation
 * @param <TR> entity resource
 */
@KeycloakIntegrationTest
public abstract class AbstractInvalidationClusterTest<T, TR> extends AbstractClusterTest {

    @InjectRealm
    ManagedRealm managedRealm;

    /**
     * Maximum time to wait for cluster cache synchronization (ms).
     */
    private static final long CLUSTER_SYNC_TIMEOUT_MS = 10000;

    /**
     * Polling interval for checking cluster synchronization (ms).
     */
    private static final long CLUSTER_SYNC_POLL_INTERVAL_MS = 100;

    protected RealmRepresentation createTestRealmRepresentation() {
        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setRealm("test_" + RandomStringUtils.randomAlphabetic(5));
        testRealm.setEnabled(true);
        testRealm.setEditUsernameAllowed(true);
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
        log.info("(1) createEntityOnCurrentFailNode");
        testEntity = createEntityOnCurrentFailNode(testEntity);

        if (backendFailover) {
            log.info("(2) failure");
            failure();
        }

        log.info("(3) assertEntityOnSurvivorNodesEqualsTo");
        assertEntityOnSurvivorNodesEqualsTo(testEntity);

        log.info("(4) failback");
        failback();
        log.info("(5) iterateCurrentFailNode");
        iterateCurrentFailNode();

        // UPDATE(s)
        log.info("(6) testEntityUpdates");
        testEntity = testEntityUpdates(testEntity, backendFailover);

        // DELETE 
        log.info("(7) deleteEntityOnCurrentFailNode");
        deleteEntityOnCurrentFailNode(testEntity);

        if (backendFailover) {
            log.info("(8) failure");
            failure();
        }

        log.info("(9) assertEntityOnSurvivorNodesIsDeleted");
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

    /**
     * Waits for cluster cache synchronization by polling all backend nodes until all have
     * consistent state or timeout is reached. This prevents flaky failures due to cache
     * invalidation events not being fully propagated during split-brain recovery.
     *
     * Unlike getCurrentSurvivorNodes(), this checks ALL backend nodes including the one
     * just restarted by failback(), ensuring the restarted node has rejoined the cluster
     * and received cache invalidation events before proceeding.
     *
     * @param testEntity the entity to verify across all nodes
     * @throws AssertionError if cache consistency is not achieved within the timeout
     */
    protected void waitForClusterSync(T testEntity) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + CLUSTER_SYNC_TIMEOUT_MS;

        log.info(String.format("Waiting for cluster cache synchronization (timeout: %dms, poll interval: %dms)",
                CLUSTER_SYNC_TIMEOUT_MS, CLUSTER_SYNC_POLL_INTERVAL_MS));

        while (System.currentTimeMillis() < endTime) {
            boolean allNodesConsistent = true;

            // Check ALL backend nodes, including the one just restarted by failback()
            for (int i = 0; i < getClusterSize(); i++) {
                ContainerInfo node = backendNode(i);
                T testEntityOnNode = readEntity(testEntity, node);

                if (!EqualsBuilder.reflectionEquals(sortFields(testEntityOnNode), sortFields(testEntity), excludedComparisonFields)) {
                    allNodesConsistent = false;
                    log.debug(String.format("Cache not yet synchronized on %s, retrying...", node));
                    break;
                }
            }

            if (allNodesConsistent) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info(String.format("Cluster cache synchronized successfully across all %d nodes after %dms",
                        getClusterSize(), elapsed));
                return;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(CLUSTER_SYNC_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for cluster synchronization", e);
            }
        }

        // Timeout reached - perform final assertion on all nodes that will show the differences
        log.error(String.format("Cluster cache synchronization timeout after %dms", CLUSTER_SYNC_TIMEOUT_MS));
        assertEntityOnAllNodes(testEntity);
    }

    /**
     * Asserts that the entity is consistent across all backend nodes.
     * Unlike assertEntityOnSurvivorNodesEqualsTo(), this checks ALL nodes.
     */
    private void assertEntityOnAllNodes(T testEntityOnFailNode) {
        boolean entityDiffers = false;
        for (int i = 0; i < getClusterSize(); i++) {
            ContainerInfo node = backendNode(i);
            log.debug(String.format("Attempt to verify %s on node %s (%s)", getEntityType(testEntityOnFailNode), node, node.getContextRoot()));
            T testEntityOnNode = readEntity(testEntityOnFailNode, node);

            if (EqualsBuilder.reflectionEquals(sortFields(testEntityOnNode), sortFields(testEntityOnFailNode), excludedComparisonFields)) {
                log.info(String.format("Verification of %s on node %s PASSED", getEntityType(testEntityOnFailNode), node));
            } else {
                entityDiffers = true;
                log.error(String.format("Verification of %s on node %s FAILED", getEntityType(testEntityOnFailNode), node));
                String tf = ReflectionToStringBuilder.reflectionToString(testEntityOnFailNode, ToStringStyle.SHORT_PREFIX_STYLE);
                String ts = ReflectionToStringBuilder.reflectionToString(testEntityOnNode, ToStringStyle.SHORT_PREFIX_STYLE);
                log.error(String.format(
                        "\nEntity on fail node: \n%s\n"
                        + "\nEntity on node %s: \n%s\n"
                        + "\nDifference: \n%s\n",
                        tf, node, ts, StringUtils.difference(tf, ts)));
            }
        }
        assertFalse(entityDiffers);
    }

    protected void verifyEntityUpdateDuringFailover(T testEntity, boolean backendFailover) {
        if (backendFailover) {
            failure();
        }

        assertEntityOnSurvivorNodesEqualsTo(testEntity);

        failback();
        // Wait for cluster rebalancing to complete after failback
        waitForClusterSync(testEntity);
        iterateCurrentFailNode();
    }

    protected List<String> excludedComparisonFields = new ArrayList<>();

    protected void assertEntityOnSurvivorNodesEqualsTo(T testEntityOnFailNode) {
        boolean entityDiffers = false;
        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            log.debug(String.format("Attempt to verify %s on survivor %s (%s)", getEntityType(testEntityOnFailNode), survivorNode, survivorNode.getContextRoot()));
            T testEntityOnSurvivorNode = readEntity(testEntityOnFailNode, survivorNode);

            if (EqualsBuilder.reflectionEquals(sortFields(testEntityOnSurvivorNode), sortFields(testEntityOnFailNode), excludedComparisonFields)) {
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

    private T sortFields(T entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            try {
                Class<?> type = resolveListType(field, entity);

                if (type != null && Comparable.class.isAssignableFrom(type)) {
                    setAccessible(field);
                    Object value = field.get(entity);

                    if (value != null) {
                        Collections.sort((List) value);
                    }
                }
            } catch (IllegalAccessException cause) {
                throw new RuntimeException("Failed to sort field [" + field + "]", cause);
            } finally {
                unsetAccessible(field);
            }
        }

        return entity;
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
