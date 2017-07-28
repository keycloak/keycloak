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

import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.LoadBalancerController;
import org.keycloak.testsuite.arquillian.annotation.LoadBalancer;
import org.keycloak.testsuite.auth.page.AuthRealm;

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
 *
 * @author hmlnarik
 */
public abstract class AbstractCrossDCTest extends AbstractTestRealmKeycloakTest {

    // Keep the following constants in sync with arquillian
    public static final String QUALIFIER_NODE_BALANCER = "auth-server-balancer-cross-dc";

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
        loadBalancerCtrl.enableBackendNodeByName(getAutomaticallyStartedBackendNodes(0)
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
    public void InitRESTClientsForStartedNodes() {
        log.debug("Init REST clients for automatically started nodes");
        this.suiteContext.getDcAuthServerBackendsInfo().stream()
                .flatMap(List::stream)
                .filter(ContainerInfo::isStarted)
                .filter(containerInfo -> !containerInfo.isManual())
                .forEach(containerInfo -> {
                    createRESTClientsForNode(containerInfo);
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
     * @param dcIndex
     */
    public void disableDcOnLoadBalancer(int dcIndex) {
        log.infof("Disabling load balancer for dc=%d", dcIndex);
        this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).forEach(c -> loadBalancerCtrl.disableBackendNodeByName(c.getQualifier()));
    }

    /**
     * Enables routing requests to all started nodes to the given data center in the load balancer.
     * @param dcIndex
     */
    public void enableDcOnLoadBalancer(int dcIndex) {
        log.infof("Enabling load balancer for dc=%d", dcIndex);
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        if (! dcNodes.stream().anyMatch(ContainerInfo::isStarted)) {
            log.warnf("No node is started in DC %d", dcIndex);
        } else {
            dcNodes.stream()
              .filter(ContainerInfo::isStarted)
              .forEach(c -> loadBalancerCtrl.enableBackendNodeByName(c.getQualifier()));
        }
    }

    /**
     * Disables routing requests to the given node within the given data center in the load balancer.
     * @param dcIndex
     * @param nodeIndex
     */
    public void disableLoadBalancerNode(int dcIndex, int nodeIndex) {
        log.infof("Disabling load balancer for dc=%d, node=%d", dcIndex, nodeIndex);
        loadBalancerCtrl.disableBackendNodeByName(this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).get(nodeIndex).getQualifier());
    }

    /**
     * Enables routing requests to the given node within the given data center in the load balancer.
     * @param dcIndex
     * @param nodeIndex
     */
    public void enableLoadBalancerNode(int dcIndex, int nodeIndex) {
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
     * @param dcIndex
     * @param nodeIndex
     * @return Started instance descriptor.
     */
    public ContainerInfo startBackendNode(int dcIndex, int nodeIndex) {
        assertThat((Integer) dcIndex, lessThan(this.suiteContext.getDcAuthServerBackendsInfo().size()));
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        assertThat((Integer) nodeIndex, lessThan(dcNodes.size()));
        ContainerInfo dcNode = dcNodes.get(nodeIndex);
        assertTrue("Node " + dcNode.getQualifier() + " has to be controlled manually", dcNode.isManual());
        containerController.start(dcNode.getQualifier());

        createRESTClientsForNode(dcNode);

        return dcNode;
    }

    /**
     * Stops a manually-controlled backend auth-server node in cross-DC scenario.
     * @param dcIndex
     * @param nodeIndex
     * @return Stopped instance descriptor.
     */
    public ContainerInfo stopBackendNode(int dcIndex, int nodeIndex) {
        assertThat((Integer) dcIndex, lessThan(this.suiteContext.getDcAuthServerBackendsInfo().size()));
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        assertThat((Integer) nodeIndex, lessThan(dcNodes.size()));
        ContainerInfo dcNode = dcNodes.get(nodeIndex);

        removeRESTClientsForNode(dcNode);

        assertTrue("Node " + dcNode.getQualifier() + " has to be controlled manually", dcNode.isManual());
        containerController.stop(dcNode.getQualifier());
        return dcNode;
    }

    /**
     * Returns stream of all nodes in the given dc that are started manually.
     * @param dcIndex
     * @return
     */
    public Stream<ContainerInfo> getManuallyStartedBackendNodes(int dcIndex) {
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        return dcNodes.stream().filter(ContainerInfo::isManual);
    }

    /**
     * Returns stream of all nodes in the given dc that are started automatically.
     * @param dcIndex
     * @return
     */
    public Stream<ContainerInfo> getAutomaticallyStartedBackendNodes(int dcIndex) {
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        return dcNodes.stream().filter(c -> ! c.isManual());
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
                .forEach(testingClientEntry -> {
                    KeycloakTestingClient testingClient = testingClientEntry.getValue();
                    testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(offset)));
                });
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
