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

import java.util.LinkedList;
import java.util.List;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.concurrency.ConcurrentLoginTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.LoadBalancerController;
import org.keycloak.testsuite.arquillian.annotation.LoadBalancer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrentLoginCrossDCTest extends ConcurrentLoginTest {

    @ArquillianResource
    @LoadBalancer(value = AbstractCrossDCTest.QUALIFIER_NODE_BALANCER)
    protected LoadBalancerController loadBalancerCtrl;

    @ArquillianResource
    protected ContainerController containerController;


    // Need to postpone that
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }


    @Before
    @Override
    public void beforeTest() {
        log.debug("Initializing load balancer - only enabling started nodes in the first DC");
        this.loadBalancerCtrl.disableAllBackendNodes();

        // This should enable only the started nodes in first datacenter
        this.suiteContext.getDcAuthServerBackendsInfo().get(0).stream()
                .filter(ContainerInfo::isStarted)
                .map(ContainerInfo::getQualifier)
                .forEach(loadBalancerCtrl::enableBackendNodeByName);

        this.suiteContext.getDcAuthServerBackendsInfo().get(1).stream()
                .filter(ContainerInfo::isStarted)
                .map(ContainerInfo::getQualifier)
                .forEach(loadBalancerCtrl::enableBackendNodeByName);



        // Import realms
        log.info("Importing realms");
        List<RealmRepresentation> testRealms = new LinkedList<>();
        super.addTestRealms(testRealms);
        for (RealmRepresentation testRealm : testRealms) {
            importRealm(testRealm);
        }
        log.info("Realms imported");

        // Finally create clients
        createClients();
    }
}
