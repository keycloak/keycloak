/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.it.cli.dist;

import java.io.IOException;
import java.net.ConnectException;

import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true, defaultOptions = { "--db=dev-file", "--http-enabled=true", "--hostname-strict=false"})
@RawDistOnly(reason = "Containers are immutable")
public class IpStackDistTest {

    @Test
    @Launch({"start"})
    public void dualStackEnabled() {
        assertThat(given().when().get("http://localhost:8080").getStatusCode(), is(200));
        assertThat(given().when().get("http://127.0.0.1:8080").getStatusCode(), is(200));
        assertThat(given().when().get("http://[::1]:8080").getStatusCode(), is(200));
    }

    @Test
    @Launch({"start", "-Djava.net.preferIPv4Stack=true"})
    public void ipv4Preferred() throws IOException {
        assertThat(given().when().get("http://localhost:8080").getStatusCode(), is(200));
        assertThat(given().when().get("http://127.0.0.1:8080").getStatusCode(), is(200));

        assertThrows(ConnectException.class, () -> given().when().get("http://[::1]:8080"), "Connection refused must be thrown");
    }

    @Test
    @Launch({"start", "-Djava.net.preferIPv6Addresses=true"})
    public void ipv6Prefered() {
        assertThat(given().when().get("http://localhost:8080").getStatusCode(), is(200));
        assertThat(given().when().get("http://127.0.0.1:8080").getStatusCode(), is(200));
        assertThat(given().when().get("http://[::1]:8080").getStatusCode(), is(200));
    }
}
