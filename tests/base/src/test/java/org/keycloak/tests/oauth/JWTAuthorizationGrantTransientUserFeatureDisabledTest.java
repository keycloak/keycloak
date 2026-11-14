/*
 * Copyright 2026 Keycloak contributors.
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

package org.keycloak.tests.oauth;

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = JWTAuthorizationGrantTransientUserFeatureDisabledTest.ServerConfig.class)
public class JWTAuthorizationGrantTransientUserFeatureDisabledTest extends BaseAbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = JWTAuthorizationGrantTransientUserTest.TransientRealmConfig.class)
    protected ManagedRealm realm;

    @Test
    public void rejectsTransientOptInWithoutFeature() {
        String assertion = getIdentityProvider().encodeToken(createDefaultAuthorizationGrantToken());
        AccessTokenResponse response = oAuthClient.jwtAuthorizationGrantRequest(assertion).send();
        assertFailure("Transient users are not enabled", response, events.poll());
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.featuresDisabled(Profile.Feature.TRANSIENT_USERS);
        }
    }
}
