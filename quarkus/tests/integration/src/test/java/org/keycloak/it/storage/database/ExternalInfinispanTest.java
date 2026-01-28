/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.storage.database;

import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.InfinispanContainer;
import org.keycloak.it.junit5.extension.WithExternalInfinispan;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;

@DistributionTest(keepAlive = true)
@WithExternalInfinispan
@Tag(DistributionTest.STORAGE)
public class ExternalInfinispanTest {

    @Test
    @Launch({
            "start-dev",
            "--features=multi-site",
            "--cache=ispn",
            "--cache-remote-host=127.0.0.1",
            "--cache-remote-username=keycloak",
            "--cache-remote-password=Password1!",
            "--cache-remote-tls-enabled=false",
            "--spi-cache-embedded-default-site-name=ISPN",
            "--spi-load-balancer-check-remote-poll-interval=500",
            "--verbose"
    })
    void testLoadBalancerCheckFailureWithMultiSite() {
        runLoadBalancerCheckFailureTest();
    }

    @Test
    @Launch({
            "start-dev",
            "--features=multi-site,clusterless",
            "--cache=ispn",
            "--cache-remote-host=127.0.0.1",
            "--cache-remote-username=keycloak",
            "--cache-remote-password=Password1!",
            "--cache-remote-tls-enabled=false",
            "--spi-cache-embedded-default-site-name=ISPN",
            "--spi-load-balancer-check-remote-poll-interval=500",
            "--verbose"
    })
    void testLoadBalancerCheckFailureWithRemoteOnlyCaches() {
        runLoadBalancerCheckFailureTest();
    }

    private void runLoadBalancerCheckFailureTest() {
        when().get("/lb-check").then()
                .statusCode(200);

        InfinispanContainer.removeCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

        // The `lb-check` relies on the Infinispan's persistence check status. By default, Infinispan checks in the background every second that the remote store is available.
        // So we'll wait on average about one second here for the check to switch its state.
        Retry.execute(() -> {
            when().get("/lb-check").then()
                    .statusCode(503);
        }, 10, 200);
    }

    @Test
    @Launch({
            "start-dev",
            "--cache=ispn",
            "-Djboss.site.name=ISPN",
            "--verbose"
    })
    void testSiteNameAsSystemProperty(CLIResult cliResult) {
        cliResult.assertMessage("System property jboss.site.name is in use. Use --spi-cache-embedded-default-site-name config option instead");
    }

    @Test
    @Launch({
            "start-dev",
            "--cache=ispn",
            "-Djboss.node.name=ISPN",
            "--verbose"
    })
    void testNodeNameAsSystemProperty(CLIResult cliResult) {
        cliResult.assertMessage("System property jboss.node.name is in use. Use --spi-cache-embedded-default-node-name config option instead");
    }
}
