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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractCrossDCTest extends AbstractTestRealmKeycloakTest {

    @ArquillianResource
    @LoadBalancer(value = "auth-server-balancer-cross-dc")
    protected LoadBalancerController loadBalancerCtrl;

    @ArquillianResource
    protected ContainerController containerController;

    protected Map<ContainerInfo, Keycloak> backendAdminClients = new HashMap<>();

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
          .map(ContainerInfo::getQualifier)
          .forEach(containerController::stop);
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
        // Enable only the started nodes in each datacenter
        this.suiteContext.getDcAuthServerBackendsInfo().get(0).stream()
          .filter(ContainerInfo::isStarted)
          .map(ContainerInfo::getQualifier)
          .forEach(loadBalancerCtrl::enableBackendNodeByName);
    }

    protected Keycloak createAdminClientFor(ContainerInfo node) {
        log.info("Initializing admin client for " + node.getContextRoot() + "/auth");
        return Keycloak.getInstance(node.getContextRoot() + "/auth", AuthRealm.MASTER, AuthRealm.ADMIN, AuthRealm.ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    protected Keycloak getAdminClientFor(ContainerInfo node) {
        Keycloak adminClient = backendAdminClients.get(node);
        if (adminClient == null && node.equals(suiteContext.getAuthServerInfo())) {
            adminClient = this.adminClient;
        }
        return adminClient;
    }

    public void disableDcOnLoadBalancer(int dcIndex) {
        log.infof("Disabling load balancer for dc=%d", dcIndex);
        this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).forEach(c -> loadBalancerCtrl.disableBackendNodeByName(c.getQualifier()));
    }

    /**
     * Enables all started nodes in the given data center
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

    public void disableLoadBalancerNode(int dcIndex, int nodeIndex) {
        log.infof("Disabling load balancer for dc=%d, node=%d", dcIndex, nodeIndex);
        loadBalancerCtrl.disableBackendNodeByName(this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex).get(nodeIndex).getQualifier());
    }

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

    public Stream<ContainerInfo> getManuallyStartedBackendNodes(int dcIndex) {
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        return dcNodes.stream().filter(ContainerInfo::isManual);
    }

    public Stream<ContainerInfo> getAutomaticallyStartedBackendNodes(int dcIndex) {
        final List<ContainerInfo> dcNodes = this.suiteContext.getDcAuthServerBackendsInfo().get(dcIndex);
        return dcNodes.stream().filter(c -> ! c.isManual());
    }
}
