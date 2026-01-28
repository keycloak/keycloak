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

import java.net.URL;

import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.client.authentication.JWTClientSecretCredentialsProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = ClientVaultTest.ClientVaultConfig.class)
class ClientVaultTest {

    @InjectRealm(config = ClientVaultTest.ClientTestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauthClient;

    @Test
    void testClientVault() {
        AccessTokenResponse response = oauthClient
                .client("myclient", "mysecret")
                .doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());
    }

    @Test
    void testClientVaultWithInvalidSecret() {
        AccessTokenResponse response = oauthClient
                .client("myclient", "invalid-secret")
                .doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(401, response.getStatusCode());
    }

    @Test
    void testClientVaultWithInvalidVaultReference() {
        AccessTokenResponse response = oauthClient
                .client("myclient-with-invalid-vault-reference", "mysecret")
                .doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(401, response.getStatusCode());
    }

    @Test
    void testClientVaultWithJwtClientSecretAuthenticator() {
        String clientId = "myclient-jwt-client-secret-authenticator";
        JWTClientSecretCredentialsProvider jwtProvider = new JWTClientSecretCredentialsProvider();
        jwtProvider.setClientSecret("mysecret", Algorithm.HS256);
        String jwt = jwtProvider.createSignedRequestToken(clientId, oauthClient.getEndpoints().getIssuer(), Algorithm.HS256);

        AccessTokenResponse response = oauthClient
                .passwordGrantRequest("test-user@localhost", "password")
                .client(clientId)
                .clientJwt(jwt)
                .send();

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAccessToken());
    }

    public static class ClientVaultConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            URL url = ClientVaultTest.class.getResource("vault");
            if (url == null) {
                throw new RuntimeException("Unable to find the vault folder in the classpath for the default_client__secret file!");
            }
            return config.option("vault", "file").option("vault-dir", url.getPath());
        }
    }

    public static class ClientTestRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("myclient")
                    .publicClient(false)
                    .directAccessGrantsEnabled(true)
                    .secret("${vault.client_secret}");

            realm.addClient("myclient-with-invalid-vault-reference")
                    .publicClient(false)
                    .directAccessGrantsEnabled(true)
                    .secret("${vault.non_existing_client_secret}");

            realm.addClient("myclient-jwt-client-secret-authenticator")
                    .publicClient(false)
                    .directAccessGrantsEnabled(true)
                    .authenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID)
                    .secret("${vault.client_secret}");

            realm.addUser("test-user@localhost")
                    .email("test-user@localhost")
                    .password("password")
                    .name("first", "last")
                    .enabled(true);
            return realm;
        }
    }
}
