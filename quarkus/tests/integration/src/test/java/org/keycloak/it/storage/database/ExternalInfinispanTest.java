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

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Retry;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.InfinispanContainer;
import org.keycloak.it.junit5.extension.WithExternalInfinispan;

import static io.restassured.RestAssured.when;

@DistributionTest(keepAlive = true)
@WithExternalInfinispan
public class ExternalInfinispanTest {

    @Test
    @Launch({ "start-dev", "--features=multi-site", "--cache=ispn", "--cache-config-file=../../../test-classes/ExternalInfinispan/kcb-infinispan-cache-remote-store-config.xml", "--spi-connections-infinispan-quarkus-site-name=ISPN" })
    void testLoadBalancerCheckFailure() {
        when().get("/lb-check").then()
                .statusCode(200);

        InfinispanContainer.removeCache("sessions");

        // The `lb-check` relies on the Infinispan's persistence check status. By default, Infinispan checks in the background every second that the remote store is available.
        // So we'll wait on average about one second here for the check to switch its state.
        Retry.execute(() -> {
            when().get("/lb-check").then()
                    .statusCode(503);
        }, 10, 200);
    }

    @Test
    @Launch({ "start-dev", "--features=multi-site", "--cache=ispn", "--cache-config-file=../../../test-classes/ExternalInfinispan/kcb-infinispan-cache-remote-store-config.xml", "-Djboss.site.name=ISPN" })
    void testSiteNameAsSystemProperty(LaunchResult result) {
        ((CLIResult) result).assertMessage("System property jboss.site.name is in use. Use --spi-connections-infinispan-quarkus-site-name config option instead");
    }
}
