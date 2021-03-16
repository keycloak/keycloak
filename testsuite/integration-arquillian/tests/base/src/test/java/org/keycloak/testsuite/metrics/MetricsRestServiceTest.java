/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.metrics;

import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ContainerAssume;

import static org.hamcrest.Matchers.containsString;
import static org.keycloak.testsuite.util.Matchers.body;
import static org.keycloak.testsuite.util.Matchers.statusCodeIs;

public class MetricsRestServiceTest extends AbstractKeycloakTest {

    private static final String MGMT_PORT = System.getProperty("auth.server.management.port", "10090");
    private static final String MGMT_HOST = System.getProperty("auth.server.management.host", "localhost");

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        // no test realms
    }

    @BeforeClass
    public static void enabled() {
        ContainerAssume.assumeNotAuthServerUndertow();
        ContainerAssume.assumeNotAuthServerQuarkus();
    }

    @Test
    public void testHealthEndpoint() {
        Client client = AdminClientUtil.createResteasyClient();
        final String serverReportedRunningPerMicroProfileHealthSubsystem = "{\"status\":\"UP\",\"checks\":[{";
        final String serverReportedRunningPerWildflyHealthSubsystem = "{\"name\" : \"server-state\", \"outcome\" : true, \"data\" : [{ \"value\" : \"running\" }]}";

        // Post upgrade to Wildfly 22 expect output of Wildfly subsystem for health for community builds, and output of MicroProfile health subsystem for product builds
        final String expectedString = Profile.getName().equals("community") ? serverReportedRunningPerWildflyHealthSubsystem : serverReportedRunningPerMicroProfileHealthSubsystem;

        try (Response response = client.target("http://" + MGMT_HOST + ":" + MGMT_PORT + "/health").request().get()) {
            Assert.assertThat(response, statusCodeIs(Status.OK));
            Assert.assertThat(response, body(containsString(expectedString)));
        } finally {
            client.close();
        }
    }

    @Test
    public void  testMetricsEndpoint() {
        Client client = AdminClientUtil.createResteasyClient();

        try (Response response = client.target("http://" + MGMT_HOST + ":" + MGMT_PORT + "/metrics").request().get()) {
            Assert.assertThat(response, statusCodeIs(Status.OK));
            Assert.assertThat(response, body(containsString("base_memory_maxHeap_bytes")));
        } finally {
            client.close();
        }
    }
}
