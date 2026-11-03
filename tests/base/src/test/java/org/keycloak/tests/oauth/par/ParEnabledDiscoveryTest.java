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
package org.keycloak.tests.oauth.par;

import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = ParEnabledDiscoveryTest.ServerConfig.class)
public class ParEnabledDiscoveryTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @Test
    public void advertisesParWhenEnabled() throws Exception {
        // when
        try (SimpleHttpResponse response = http.doGet(realm.getBaseUrl() + "/.well-known/openid-configuration").asResponse()) {
            JsonNode metadata = response.asJson();

            // then
            assertThat(response.getStatus(), is(200));
            String endpoint = realm.getBaseUrl() + "/protocol/openid-connect/ext/par/request";
            assertThat(metadata.path("pushed_authorization_request_endpoint").textValue(), is(endpoint));
            assertThat(metadata.path("mtls_endpoint_aliases").path("pushed_authorization_request_endpoint").textValue(), is(endpoint));
            assertThat(metadata.path("require_pushed_authorization_requests").isBoolean(), is(true));
            assertThat(metadata.path("require_pushed_authorization_requests").booleanValue(), is(false));
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PAR);
        }
    }
}
