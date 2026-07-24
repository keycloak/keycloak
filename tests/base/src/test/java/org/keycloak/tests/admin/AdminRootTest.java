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

import java.util.Arrays;
import java.util.Map;

import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@KeycloakIntegrationTest(config = AdminRootTest.AdminUrlConfig.class)
public class AdminRootTest {
    // This might not be robust enough. If something made KC on a different port, this would fail.
    private static final String HOSTNAME = "http://localtest.me:8080";
    private static final String HOSTNAME_ADMIN = "http://admin.localtest.me:8080";
    private static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080";

    @InjectHttpClient(followRedirects = false)
    private HttpClient client;

    @ParameterizedTest
    @ValueSource(strings = {HOSTNAME_ADMIN, HOSTNAME_LOCAL_ADMIN})
    public void testRedirect(String hostname) throws Exception {
        HttpResponse response = client.execute(new HttpGet(hostname + "/admin"));

        assertEquals(302, response.getStatusLine().getStatusCode());
        assertThat(response.getFirstHeader("Location").getValue(), startsWith(HOSTNAME_ADMIN + "/admin/master/console"));
    }

    @Test
    public void testNoRedirectWithFrontendUrl() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME + "/admin");
        // make the request seem like it's coming from a proxy so that it won't seem local
        request.addHeader("Forwarded", "for=192.0.2.60");
        HttpResponse response = client.execute(request);

        assertEquals(404, response.getStatusLine().getStatusCode());

        // Check for leaks of hostname-admin in headers and body
        assertFalse(Arrays.stream(response.getAllHeaders()).anyMatch(header -> header.getValue().contains(HOSTNAME_ADMIN)));
        assertThat(EntityUtils.toString(response.getEntity()), not(containsString(HOSTNAME_ADMIN))); // just in case of a JS redirect
    }

    public static class AdminUrlConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.options(Map.of(
                    "hostname", HOSTNAME,
                    "hostname-admin", HOSTNAME_ADMIN
            ));
        }
    }
}
