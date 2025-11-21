/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.cli.dist;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.resource.realm.TestRealmResourceTestProvider;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@DistributionTest(keepAlive = true, enableTls = true)
@RawDistOnly(reason = "Containers are immutable")
public class HttpDistTest {
    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    public void maxQueuedRequestsTest(KeycloakDistribution dist) {
        dist.run("start-dev", "--http-max-queued-requests=1", "--http-pool-max-threads=1");

        // run requests async
        List<CompletableFuture<Integer>> statusCodesFuture = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            statusCodesFuture.add(CompletableFuture.supplyAsync(() ->
                    when().get("/realms/master/test-resources/slow").getStatusCode()));
        }
        List<Integer> statusCodes = statusCodesFuture.stream().map(CompletableFuture::join).toList();

        assertThat("Some of the requests should be properly rejected", statusCodes, hasItem(503));
        assertThat("None of the requests should throw an unhandled exception", statusCodes, not(hasItem(500)));
    }

    @Test
    @Launch({"start-dev", "--log-level=INFO,org.keycloak.quarkus.runtime.services.RejectNonNormalizedPathFilter:debug", "--http-access-log-enabled=true"})
    public void preventNonNormalizedURLs() {
        when().get("/realms/master").then().statusCode(200);
        when().get("/realms/xxx/../master").then().statusCode(400);
    }

    @Test
    @Launch({"start-dev", "--http-access-log-enabled=true", "--http-accept-non-normalized-paths=true"})
    public void allowNonNormalizedURLs() {
        when().get("/realms/master").then().statusCode(200);
        when().get("/realms/xxx/../master").then().statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--https-certificates-reload-period=wrong"})
    public void testHttpCertificateReloadPeriod(CLIResult result) {
        result.assertError("Text cannot be parsed to a Duration");
    }

    @Test
    public void httpStoreTypeValidation(KeycloakDistribution dist) {
        CLIResult result = dist.run("start", "--https-key-store-file=not-there.ks", "--hostname-strict=false");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: Unable to determine 'https-key-store-type' automatically. Adjust the file extension or specify the property");

        result = dist.run("start", "--https-trust-store-file=not-there.ks", "--hostname-strict=false");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: Unable to determine 'https-trust-store-type' automatically. Adjust the file extension or specify the property");

        result = dist.run("start", "--https-key-store-file=not-there.ks", "--hostname-strict=false", "--https-key-store-type=jdk");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: Failed to load 'https-*' material: NoSuchFileException not-there.ks");

        dist.copyOrReplaceFileFromClasspath("/server.keystore.pkcs12", Path.of("conf", "server.p12"));
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("server.p12").toAbsolutePath();

        result = dist.run("start", "--https-trust-store-file=" + truststorePath, "--hostname-strict=false");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: No trust store password provided");
    }
}
