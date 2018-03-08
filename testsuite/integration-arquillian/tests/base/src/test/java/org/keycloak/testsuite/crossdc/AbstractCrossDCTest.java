/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.crossdc;

import org.apache.commons.io.FileUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.LoadBalancerController;
import org.keycloak.testsuite.arquillian.annotation.LoadBalancer;
import org.keycloak.testsuite.auth.page.AuthRealm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.client.KeycloakTestingClient;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Abstract cross-data-centre test that defines primitives for handling cross-DC setup.
 * @author hmlnarik
 */
public abstract class AbstractCrossDCTest extends AbstractTestRealmKeycloakTest {

    // Keep the following constants in sync with arquillian
    public static final String QUALIFIER_NODE_BALANCER = "auth-server-balancer-cross-dc";
    public static final String QUALIFIER_AUTH_SERVER_DC_0_NODE_1 = "auth-server-${node.name}-cross-dc-0_1";
    public static final String QUALIFIER_AUTH_SERVER_DC_1_NODE_1 = "auth-server-${node.name}-cross-dc-1_1";

    @ArquillianResource
    @LoadBalancer(value = QUALIFIER_NODE_BALANCER)
    protected LoadBalancerController loadBalancerCtrl;

    @ArquillianResource
    protected ContainerController containerController;

    protected Map<ContainerInfo, Keycloak> backendAdminClients = new HashMap<>();

    protected Map<ContainerInfo, KeycloakTestingClient> backendTestingClients = new HashMap<>();

    @After
    @Before
    public void enableOnlyFirstNodeInFirstDc() {
        this.loadBalancerCtrl.disableAllBackendNodes();
        loadBalancerCtrl.enableBackendNodeByName(getAutomaticallyStartedBackendNodes(DC.FIRST)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No node is started automatically"))
                        .getQualifier()
        );
    }

    @Before
    public void terminateManuallyStartedServers() {
        log.debug("Halting all nodes that are started manually");
        this.suiteContext.getDcAuthServerBackendsInfo().stream()
          .flatMap(List::stream)
          .filter(ContainerInfo::isStarted)
          .filter(ContainerInfo::isManual)
          .forEach(containerInfo -> {
              containerController.stop(containerInfo.getQualifier());
              removeRESTClientsForNode(containerInfo);
          });
    }

    @Before
    public void initRESTClientsForStartedNodes() {
        log.debug("Init REST clients for automatically started nodes");
        this.suiteContext.getDcAuthServerBackendsInfo().stream()
                .flatMap(List::stream)
                .filter(ContainerInfo::isStarted)
                .filter(containerInfo -> !containerInfo.isManual())
                .forEach(containerInfo -> {
                    createRESTClientsForNode(containerInfo);
                });

    }

    // Disable periodic tasks in cross-dc tests. It's needed to have some scenarios more stable.
    @Before
    public void suspendPeriodicTasks() {
        backendTestingClients.values().stream().forEach((KeycloakTestingClient testingClient) -> {
            testingClient.testing().suspendPeriodicTasks();
        });

    }

    @After
    public void restorePeriodicTasks() {
        backendTestingClients.values().stream().forEach((KeycloakTestingClient testingClient) -> {
            testingClient.testing().restorePeriodicTasks();
        });
    }


    @Override
    public void importTestRealms() {
        enableOnlyFirstNodeInFirstDc();
        super.importTestRealms();
    }

    @Override
    public void afterAbstractKeycloakTest() {
        enableOnlyFirstNodeInFirstDc();
        super.afterAbstractKeycloakTest();
    }

    @Override
    public void deleteCookies() {
        enableOnlyFirstNodeInFirstDc();
        super.deleteCookies();
    }

    @Before
    public void initLoadBalancer() {
        log.debug("Initializing load balancer - only enabling started nodes in the first DC");
        this.loadBalancerCtrl.disableAllBackendNodes();
        // Enable only the started nodes in first datacenter
        this.suiteContext.getDcAuthServerBackendsInfo().get(0).stream()
          .filter(ContainerInfo::isStarted)
          .map(ContainerInfo::getQualifier)
          .forEach(loadBalancerCtrl::enableBackendNodeByName);
    }

    protected Keycloak createAdminClientFor(ContainerInfo node) {
        log.info("Initializing admin client for " + node.getContextRoot() + "/auth");
        return Keycloak.getInstance(node.getContextRoot() + "/auth", AuthRealm.MASTER, AuthRealm.ADMIN, AuthRealm.ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    protected KeycloakTestingClient createTestingClientFor(ContainerInfo node) {
        log.info("Initializing testing client for " + node.getContextRoot() + "/auth");
        return KeycloakTestingClient.getInstance(node.getContextRoot() + "/auth");
    }


    protected Keycloak getAdminClientForStartedNodeInDc(int dcIndex) {
        ContainerInfo firstStartedNode = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).stream()
                .filter(ContainerInfo::isStarted)
                .findFirst().get();

        return getAdminClientFor(firstStartedNode);
    }

    /**
     * Get admin client directed to the given node.
     * @param node
     * @return
     */
    protected Keycloak getAdminClientFor(ContainerInfo node) {
        Keycloak client = backendAdminClients.get(node);
        if (client == null && node.equals(suiteContext.getAuthServerInfo())) {
            client = this.adminClient;
        }
        return client;
    }


    protected KeycloakTestingClient getTestingClientForStartedNodeInDc(int dcIndex) {
        ContainerInfo firstStartedNode = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).stream()
                .filter(ContainerInfo::isStarted)
                .findFirst().get();

        return getTestingClientFor(firstStartedNode);
    }


    /**
     * Get testing client directed to the given node.
     * @param node
     * @return
     */
    protected KeycloakTestingClient getTestingClientFor(ContainerInfo node) {
        KeycloakTestingClient client = backendTestingClients.get(node);
        if (client == null && node.equals(suiteContext.getAuthServerInfo())) {
            client = this.testingClient;
        }
        return client;
    }

    protected void createRESTClientsForNode(ContainerInfo node) {
        if (!backendAdminClients.containsKey(node)) {
            backendAdminClients.put(node, createAdminClientFor(node));
        }

        if (!backendTestingClients.containsKey(node)) {
            backendTestingClients.put(node, createTestingClientFor(node));
        }
    }

    protected void removeRESTClientsForNode(ContainerInfo node) {
        if (backendAdminClients.containsKey(node)) {
            backendAdminClients.get(node).close();
            backendAdminClients.remove(node);
        }

        if (backendTestingClients.containsKey(node)) {
            backendTestingClients.get(node).close();
            backendTestingClients.remove(node);
        }
    }


    /**
     * Disables routing requests to the given data center in the load balancer.
     * @param dc
     */
    public void disableDcOnLoadBalancer(DC dc) {
        int dcIndex = dc.ordinal();
        log.infof("Disabling load balancer for dc=%d", dcIndex);
        this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).forEach(containerInfo -> {
            loadBalancerCtrl.disableBackendNodeByName(containerInfo.getQualifier());
        });
    }

    /**
     * Enables routing requests to all started nodes to the given data center in the load balancer.
     * @param dc
     */
    public void enableDcOnLoadBalancer(DC dc) {
        int dcIndex = dc.ordinal();
        log.infof("Enabling load balancer for dc=%d", dcIndex);
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        if (! dcNodes.stream().anyMatch(ContainerInfo::isStarted)) {
            log.warnf("No node is started in DC %d", dcIndex);
        } else {
            dcNodes.stream()
              .filter(ContainerInfo::isStarted)
              .forEach(containerInfo -> {
                  loadBalancerCtrl.enableBackendNodeByName(containerInfo.getQualifier());
              });
        }
    }

    /**
     * Disables routing requests to the given node within the given data center in the load balancer.
     * @param dc
     * @param nodeIndex
     */
    public void disableLoadBalancerNode(DC dc, int nodeIndex) {
        int dcIndex = dc.ordinal();
        log.infof("Disabling load balancer for dc=%d, node=%d", dcIndex, nodeIndex);
        loadBalancerCtrl.disableBackendNodeByName(this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).get(nodeIndex).getQualifier());
    }

    /**
     * Enables routing requests to the given node within the given data center in the load balancer.
     * @param dc
     * @param nodeIndex
     */
    public void enableLoadBalancerNode(DC dc, int nodeIndex) {
        int dcIndex = dc.ordinal();
        log.infof("Enabling load balancer for dc=%d, node=%d", dcIndex, nodeIndex);
        final ContainerInfo backendNode = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).get(nodeIndex);
        if (backendNode == null) {
            throw new IllegalArgumentException("Invalid node with index " + nodeIndex + " for DC " + dcIndex);
        }
        if (! backendNode.isStarted()) {
            log.warnf("Node %s is not started in DC %d", backendNode.getQualifier(), dcIndex);
        }
        loadBalancerCtrl.enableBackendNodeByName(backendNode.getQualifier());
    }

    /**
     * Starts a manually-controlled backend auth-server node in cross-DC scenario.
     * @param dc
     * @param nodeIndex
     * @return Started instance descriptor.
     */
    public ContainerInfo startBackendNode(DC dc, int nodeIndex) {
        int dcIndex = dc.ordinal();
        assertThat((Integer) dcIndex, lessThan(this.suiteContext.getDcAuthServerBackendsInfo().size()));
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        assertThat((Integer) nodeIndex, lessThan(dcNodes.size()));
        ContainerInfo dcNode = dcNodes.get(nodeIndex);
        assertTrue("Node " + dcNode.getQualifier() + " has to be controlled manually", dcNode.isManual());
        
        log.infof("Starting backend node: %s (dcIndex: %d, nodeIndex: %d)", dcNode.getQualifier(), dcIndex, nodeIndex);
        containerController.start(dcNode.getQualifier());

        createRESTClientsForNode(dcNode);

        return dcNode;
    }

    /**
     * Stops a manually-controlled backend auth-server node in cross-DC scenario.
     * @param dc
     * @param nodeIndex
     * @return Stopped instance descriptor.
     */
    public ContainerInfo stopBackendNode(DC dc, int nodeIndex) {
        int dcIndex = dc.ordinal();
        assertThat((Integer) dcIndex, lessThan(this.suiteContext.getDcAuthServerBackendsInfo().size()));
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        assertThat((Integer) nodeIndex, lessThan(dcNodes.size()));
        ContainerInfo dcNode = dcNodes.get(nodeIndex);

        removeRESTClientsForNode(dcNode);

        assertTrue("Node " + dcNode.getQualifier() + " has to be controlled manually", dcNode.isManual());
        
        log.infof("Stopping backend node: %s (dcIndex: %d, nodeIndex: %d)", dcNode.getQualifier(), dcIndex, nodeIndex);
        containerController.stop(dcNode.getQualifier());
        return dcNode;
    }

    /**
     * Returns stream of all nodes in the given dc that are started manually.
     * @param dc
     * @return
     */
    public Stream<ContainerInfo> getManuallyStartedBackendNodes(DC dc) {
        int dcIndex = dc.ordinal();
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        return dcNodes.stream().filter(ContainerInfo::isManual);
    }

    /**
     * Returns stream of all nodes in the given dc that are started automatically.
     * @param dc
     * @return
     */
    public Stream<ContainerInfo> getAutomaticallyStartedBackendNodes(DC dc) {
        int dcIndex = dc.ordinal();
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        return dcNodes.stream().filter(c -> ! c.isManual());
    }

    /**
     * Returns cache server corresponding to given DC
     * @param dc
     * @return
     */
    public ContainerInfo getCacheServer(DC dc) {
        int dcIndex = dc.ordinal();
        return this.suiteContext.getCacheServersInfo().get(dcIndex);
    }


    public void stopCacheServer(ContainerInfo cacheServer) {
        log.infof("Stopping %s", cacheServer.getQualifier());

        containerController.stop(cacheServer.getQualifier());

        // Workaround for possible arquillian bug. Needs to cleanup dir manually
        String setupCleanServerBaseDir = cacheServer.getArquillianContainer().getContainerConfiguration().getContainerProperties().get("setupCleanServerBaseDir");
        String cleanServerBaseDir = cacheServer.getArquillianContainer().getContainerConfiguration().getContainerProperties().get("cleanServerBaseDir");

        if (Boolean.parseBoolean(setupCleanServerBaseDir)) {
            log.infof("Going to clean directory: %s", cleanServerBaseDir);

            File dir = new File(cleanServerBaseDir);
            if (dir.exists()) {
                try {
                    dir.renameTo(new File(dir.getParentFile(), dir.getName() + "--" + System.currentTimeMillis()));

                    File deploymentsDir = new File(dir, "deployments");
                    FileUtils.forceMkdir(deploymentsDir);
                } catch (IOException ioe) {
                    throw new RuntimeException("Failed to clean directory: " + cleanServerBaseDir, ioe);
                }
            }
        }

        log.infof("Stopped %s", cacheServer.getQualifier());
    }


    /**
     * Sets time offset on all the started containers.
     *
     * @param offset
     */
    @Override
    public void setTimeOffset(int offset) {
        super.setTimeOffset(offset);
        setTimeOffsetOnAllStartedContainers(offset);
    }

    private void setTimeOffsetOnAllStartedContainers(int offset) {
        backendTestingClients.entrySet().stream()
                .filter(testingClientEntry -> testingClientEntry.getKey().isStarted())
                .map(testingClientEntry -> testingClientEntry.getValue())
                .forEach(testingClient -> testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(offset))));
    }

    /**
     * Resets time offset on all the started containers.
     */
    @Override
    public void resetTimeOffset() {
        super.resetTimeOffset();
        setTimeOffsetOnAllStartedContainers(0);
    }
}
