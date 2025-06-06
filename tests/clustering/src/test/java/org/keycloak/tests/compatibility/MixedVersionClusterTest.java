/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.compatibility;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;

@KeycloakIntegrationTest
public class MixedVersionClusterTest {

    @InjectLoadBalancer
    LoadBalancer loadBalancer;

    @Test
    public void testUrls() throws InterruptedException {
        // TODO annotation based to skip if running in non-clustered mode.
        Assumptions.assumeTrue(loadBalancer.clusterSize() == 2);
        System.out.println("url0->" + loadBalancer.node(0).getBaseUrl());
        System.out.println("url1->" + loadBalancer.node(1).getBaseUrl());
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
    }
}
