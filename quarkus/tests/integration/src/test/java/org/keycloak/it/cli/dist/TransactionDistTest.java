/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.Map;

import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.resource.realm.TestRealmResourceTestProvider;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.MatcherAssert.assertThat;

@DistributionTest(keepAlive = true)
@RawDistOnly(reason = "Containers are immutable")
public class TransactionDistTest {

    @Test
    void testZeroTransactionTimeout(KeycloakDistribution dist) {
        var result = dist.run("start-dev", "--transaction-default-timeout=0s");
        result.assertError("Invalid duration '0s' for option 'transaction-default-timeout. Duration must be positive.");
        result = dist.run("start-dev", "--transaction-migration-timeout=0s");
        result.assertError("Invalid duration '0s' for option 'transaction-migration-timeout. Duration must be positive.");
    }

    @Test
    void testNegativeTransactionTimeout(KeycloakDistribution dist) {
        var result = dist.run("start-dev", "--transaction-default-timeout=-1s");
        result.assertError("Invalid duration '-1s' for option 'transaction-default-timeout. Duration must be positive.");
        result = dist.run("start-dev", "--transaction-migration-timeout=-2s");
        result.assertError("Invalid duration '-2s' for option 'transaction-migration-timeout. Duration must be positive.");
    }

    @Test
    void testNonNumberTransactionTimeout(KeycloakDistribution dist) {
        var result = dist.run("start-dev", "--transaction-default-timeout=abc");
        result.assertError("Invalid duration format 'abc' for option 'transaction-default-timeout'. May be an ISO 8601 duration value, an integer number of seconds, or an integer followed by one of [ms, h, m, s, d].");
        result = dist.run("start-dev", "--transaction-migration-timeout=def");
        result.assertError("Invalid duration format 'def' for option 'transaction-migration-timeout'. May be an ISO 8601 duration value, an integer number of seconds, or an integer followed by one of [ms, h, m, s, d].");
    }

    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    void testValidTransactionTimeout(KeycloakDistribution dist) throws IOException {
        var result = dist.run("start-dev", "--transaction-default-timeout=123s", "--transaction-migration-timeout=456s");
        result.assertStartedDevMode();
        assertTimeouts();
    }

    private void assertTimeouts() throws IOException {
        var rsp = when()
                .get("/realms/master/test-resources/transaction/timeouts")
                .thenReturn()
                .getBody()
                .jsonPath()
                .prettyPrint();
        var timeout = JsonSerialization.readValue(rsp, new TypeReference<Map<String, Integer>>() {
        });
        assertThat(timeout.size(), Matchers.is(2));
        assertThat(timeout.get("default"), Matchers.is(123));
        assertThat(timeout.get("migration"), Matchers.is(456));
    }

}
