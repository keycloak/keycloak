/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.junit.rules.ExternalResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.CrossDCTestEnricher;

import static org.keycloak.testsuite.arquillian.CrossDCTestEnricher.forAllBackendNodesStream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanTestTimeServiceRule extends ExternalResource {

    private static final Logger log = Logger.getLogger(InfinispanTestTimeServiceRule.class);

    private final AbstractKeycloakTest test;

    public InfinispanTestTimeServiceRule(AbstractKeycloakTest test) {
        this.test = test;
    }

    @Override
    protected void before() throws Throwable {
        if (!this.test.getTestContext().getSuiteContext().isAuthServerCrossDc()) {
            // No cross-dc environment
            test.getTestingClient().testing().setTestingInfinispanTimeService();
        } else {
            AtomicInteger count = new AtomicInteger(0);
            // Cross-dc environment - Set on all started nodes
            forAllBackendNodesStream()
                    .filter(ContainerInfo::isStarted)
                    .map(CrossDCTestEnricher.getBackendTestingClients()::get)
                    .forEach(testingClient -> {
                        testingClient.testing().setTestingInfinispanTimeService();
                        count.incrementAndGet();
                    });

            //
            log.infof("Totally set infinispanTimeService rule in %d servers", count.get());
        }
    }

    @Override
    protected void after() {
        if (!this.test.getTestContext().getSuiteContext().isAuthServerCrossDc()) {
            // No cross-dc environment
            test.getTestingClient().testing().revertTestingInfinispanTimeService();
        } else {
            // Cross-dc environment - Revert on all started nodes
            forAllBackendNodesStream()
                    .filter(ContainerInfo::isStarted)
                    .map(CrossDCTestEnricher.getBackendTestingClients()::get)
                    .forEach(testingClient -> testingClient.testing().revertTestingInfinispanTimeService());

        }
    }
}
