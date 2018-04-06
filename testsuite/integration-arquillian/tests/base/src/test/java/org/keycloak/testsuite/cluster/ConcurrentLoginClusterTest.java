/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.cluster;

import java.util.LinkedList;
import java.util.List;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.concurrency.ConcurrentLoginTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.rest.representation.JGroupsStats;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrentLoginClusterTest extends ConcurrentLoginTest {


    @ArquillianResource
    protected ContainerController controller;


    // Need to postpone that
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }


    @Before
    @Override
    public void beforeTest() {
        // Start backend nodes
        log.info("Starting 2 backend nodes now");
        for (ContainerInfo node : suiteContext.getAuthServerBackendsInfo()) {
            if (!controller.isStarted(node.getQualifier())) {
                log.info("Starting backend node: " + node);
                controller.start(node.getQualifier());
                Assert.assertTrue(controller.isStarted(node.getQualifier()));
            }
        }

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


    @Override
    public void concurrentLoginSingleUser() throws Throwable {
        super.concurrentLoginSingleUser();
        JGroupsStats stats = testingClient.testing().cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getJgroupsStats();
        log.info("JGroups statistics: " + stats.statsAsString());
    }


}
