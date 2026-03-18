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

package org.keycloak.it.cli.dist;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.truststore.TruststoreBuilder;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@DistributionTest(keepAlive = true)
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
public class TruststoreDistTest {

    @BeforeAll
    static void before() {
        RestAssured.reset();
    }

    @Test
    void testMutualAuthWithTruststorePaths(KeycloakDistribution dist) {
        String[] truststoreNames = new String[] { "keycloak-truststore.p12", "self-signed.pem" };
        Stream.of(truststoreNames).forEach(truststoreName -> {
            dist.copyOrReplaceFileFromClasspath("/" + truststoreName, Path.of("conf", truststoreName));
        });

        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        String paths = Stream.of(truststoreNames).map(truststoreName -> rawDist.getDistPath().resolve("conf")
                .resolve(truststoreName).toAbsolutePath().toString()).collect(Collectors.joining(","));
        dist.copyOrReplaceFileFromClasspath("/self-signed.p12", Path.of("conf", "self-signed.p12"));
        Path keyStore = rawDist.getDistPath().resolve("conf").resolve("self-signed.p12").toAbsolutePath();

        rawDist.run("--verbose", "start", "--db=dev-file", "--http-enabled=true", "--hostname=mykeycloak.org",
                "--truststore-paths=" + paths, "--https-client-auth=required", "--https-key-store-file=" + keyStore);

        given().trustStore(TruststoreDistTest.class.getResource("/self-signed-truststore.p12").getPath(), TruststoreBuilder.DUMMY_PASSWORD)
                .keyStore(TruststoreDistTest.class.getResource("/self-signed.p12").getPath(), "password")
                .get("https://mykeycloak.org:8443").then().body(Matchers.containsString("https://mykeycloak.org"));
    }

    @Test
    void testMutualAuthWithDefaultTruststoresDir(KeycloakDistribution dist) {
        String[] truststoreNames = new String[] { "keycloak-truststore.p12", "self-signed.pem" };
        Stream.of(truststoreNames).forEach(truststoreName -> {
            dist.copyOrReplaceFileFromClasspath("/" + truststoreName, Path.of("conf", "truststores", truststoreName));
        });

        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        dist.copyOrReplaceFileFromClasspath("/self-signed.p12", Path.of("conf", "self-signed.p12"));
        Path keyStore = rawDist.getDistPath().resolve("conf").resolve("self-signed.p12").toAbsolutePath();

        rawDist.run("--verbose", "start", "--db=dev-file", "--http-enabled=true", "--hostname=mykeycloak.org",
                "--https-client-auth=required", "--https-key-store-file=" + keyStore);

        given().trustStore(TruststoreDistTest.class.getResource("/self-signed-truststore.p12").getPath(), TruststoreBuilder.DUMMY_PASSWORD)
                .keyStore(TruststoreDistTest.class.getResource("/self-signed.p12").getPath(), "password")
                .get("https://mykeycloak.org:8443").then().body(Matchers.containsString("https://mykeycloak.org"));
    }

}
