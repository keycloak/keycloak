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

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * Regression test for <a href="https://github.com/keycloak/keycloak/issues/10655">#10655</a>.
 * <p>
 * When the Admin API is served from a dedicated admin hostname ({@code hostname-admin}), the impersonation link
 * returned by the endpoint must still be emitted on the realm frontend hostname. Otherwise the impersonation would
 * complete on the admin host and the resulting identity cookies would be scoped to the wrong host.
 */
@KeycloakIntegrationTest(config = ImpersonationHostnameTest.ServerConfig.class)
public class ImpersonationHostnameTest {

    private static final String FRONTEND_URL = "http://localtest.me:8080";
    private static final String ADMIN_URL = "https://admin.localtest.me:8443";

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(ref = "impersonated")
    ManagedUser user;

    @Test
    public void impersonationLinkIsEmittedOnFrontendHostname() {
        Map<String, Object> response = user.admin().impersonate();

        String redirect = (String) response.get("redirect");
        assertThat(redirect, startsWith(FRONTEND_URL + "/realms/" + realm.getName() + "/"));
        assertThat(redirect, containsString("/login-actions/"));
        assertThat(redirect, not(containsString("admin.localtest.me")));
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder server) {
            return server.features(Profile.Feature.IMPERSONATION)
                    .options(Map.of(
                            "hostname", FRONTEND_URL,
                            "hostname-admin", ADMIN_URL
                    ));
        }
    }
}
