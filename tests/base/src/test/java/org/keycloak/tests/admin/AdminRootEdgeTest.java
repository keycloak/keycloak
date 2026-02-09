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

package org.keycloak.tests.admin;

import java.util.Map;

import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@KeycloakIntegrationTest(config = AdminRootEdgeTest.AdminUrlConfig.class)
public class AdminRootEdgeTest {
    // full url with https, with default hostname-admin
    private static final String HOSTNAME = "https://localtest.me:8080";

    @InjectHttpClient(followRedirects = false)
    private HttpClient client;

    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1:8080", "http://localtest.me:8080"})
    public void testRedirect(String hostname) throws Exception {
        HttpResponse response = client.execute(new HttpGet(hostname + "/admin"));

        assertEquals(302, response.getStatusLine().getStatusCode());
        assertThat(response.getFirstHeader("Location").getValue(), startsWith(HOSTNAME + "/admin/master/console"));
    }

    public static class AdminUrlConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.options(Map.of(
                    "hostname", HOSTNAME
            ));
        }
    }
}
