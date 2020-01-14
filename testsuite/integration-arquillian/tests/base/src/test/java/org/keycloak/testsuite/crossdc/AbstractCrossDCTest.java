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
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.LoadBalancerController;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.LoadBalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.client.KeycloakTestingClient;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.CrossDCTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.InitialDcState;


/**
 * Abstract cross-data-centre test that defines primitives for handling cross-DC setup.
 * @author hmlnarik
 */
@InitialDcState
@AuthServerContainerExclude(AuthServer.REMOTE)
public abstract class AbstractCrossDCTest extends AbstractTestRealmKeycloakTest {

    // Keep the following constants in sync with arquillian
    public static final String QUALIFIER_NODE_BALANCER = "auth-server-balancer-cross-dc";
    public static final String QUALIFIER_AUTH_SERVER_DC_0_NODE_1 = "auth-server-${node.name}-cross-dc-0_1";
    public static final String QUALIFIER_AUTH_SERVER_DC_1_NODE_1 = "auth-server-${node.name}-cross-dc-1_1";

    @ArquillianResource
    @LoadBalancer(value = QUALIFIER_NODE_BALANCER)
    protected LoadBalancerController loadBalancerCtrl;

    @Before
    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        enableOnlyFirstNodeInFirstDc();

        super.beforeAbstractKeycloakTest();
    }

    @After
    @Override
    public void afterAbstractKeycloakTest() {
        log.debug("--DC: after AbstractCrossDCTest");
        CrossDCTestEnricher.startAuthServerBackendNode(DC.FIRST, 0);    // make sure first node is started
        enableOnlyFirstNodeInFirstDc();

        super.afterAbstractKeycloakTest();

        removeTestRealms();
        loadBalancerCtrl.disableAllBackendNodes();
    }

    private void enableOnlyFirstNodeInFirstDc() {
        log.debug("--DC: Enable only first node in first datacenter @ load balancer");
        this.loadBalancerCtrl.disableAllBackendNodes();
        if (!CrossDCTestEnricher.getBackendNode(DC.FIRST, 0).isStarted()) {
            throw new IllegalStateException("--DC: Trying to enable not started node on load-balancer");
        }
        loadBalancerCtrl.enableBackendNodeByName(CrossDCTestEnricher.getBackendNode(DC.FIRST, 0).getQualifier());
    }

    private void removeTestRealms() {
        testContext.getTestRealmReps().stream().forEach((RealmRepresentation realm) -> deleteAllCookiesForRealm(realm.getRealm()));

        log.debug("--DC: removing rest realms");
        AuthServerTestEnricher.removeTestRealms(testContext, adminClient);
        testContext.setTestRealmReps(new ArrayList<>());
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
        Keycloak client = CrossDCTestEnricher.getBackendAdminClients().get(node);
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
        KeycloakTestingClient client = CrossDCTestEnricher.getBackendTestingClients().get(node);
        if (client == null && node.equals(suiteContext.getAuthServerInfo())) {
            client = this.testingClient;
        }
        return client;
    }

    /**
     * Disables routing requests to the given data center in the load balancer.
     * @param dc
     */
    public void disableDcOnLoadBalancer(DC dc) {
        int dcIndex = dc.ordinal();
        log.infof("--DC: Disabling load balancer for dc=%d", dcIndex);
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
        log.infof("--DC: Enabling load balancer for dc=%d", dcIndex);
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        if (! dcNodes.stream().anyMatch(ContainerInfo::isStarted)) {
            log.warnf("--DC: No node is started in DC %d", dcIndex);
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
        log.infof("--DC: Disabling load balancer for dc=%d, node=%d", dcIndex, nodeIndex);
        loadBalancerCtrl.disableBackendNodeByName(this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).get(nodeIndex).getQualifier());
    }

    /**
     * Enables routing requests to the given node within the given data center in the load balancer.
     * @param dc
     * @param nodeIndex
     */
    public void enableLoadBalancerNode(DC dc, int nodeIndex) {
        int dcIndex = dc.ordinal();
        log.infof("--DC: Enabling load balancer for dc=%d, node=%d", dcIndex, nodeIndex);
        final ContainerInfo backendNode = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).get(nodeIndex);
        if (backendNode == null) {
            throw new IllegalArgumentException("Invalid node with index " + nodeIndex + " for DC " + dcIndex);
        }
        if (! backendNode.isStarted()) {
            log.warnf("--DC: Node %s is not started in DC %d", backendNode.getQualifier(), dcIndex);
        }
        loadBalancerCtrl.enableBackendNodeByName(backendNode.getQualifier());
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
        CrossDCTestEnricher.getBackendTestingClients().entrySet().stream()
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
