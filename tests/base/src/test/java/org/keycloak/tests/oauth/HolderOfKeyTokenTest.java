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
package org.keycloak.tests.oauth;

import java.util.List;

import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class HolderOfKeyTokenTest {

    @InjectRealm(config = TestRealm.class)
    ManagedRealm realm;

    @InjectCertificates(config = Certificates.class)
    ManagedCertificates certificates;

    @Test
    public void testCertificateBoundResources() throws Exception {
        try (CloseableHttpClient matching = HttpClients.custom().setSSLContext(certificates.getClientSSLContext()).build();
             CloseableHttpClient missing = HttpClients.custom().setSSLContext(certificates.createClientSSLContext(null, "keycloak-truststore.bcfks", false)).build();
             CloseableHttpClient different = HttpClients.custom().setSSLContext(certificates.createClientSSLContext("keycloak.bcfks", "keycloak-truststore.bcfks", true)).build()) {
            SimpleHttp http = SimpleHttp.create(matching);
            AccessTokenResponse tokens = getTokens(http);
            assertNotNull(tokens.getToken());

            List<String> urls = List.of(realm.getBaseUrl() + "/account",
                    realm.getBaseUrl().replace("/realms/", "/admin/realms/"),
                    realm.getBaseUrl() + "/clients-registrations/default/bound-client",
                    realm.getBaseUrl() + "/protocol/openid-connect/userinfo");
            for (String url : urls) {
                try (SimpleHttpResponse response = http.doGet(url).auth(tokens.getToken()).header("Accept", "application/json").asResponse()) {
                    assertEquals(200, response.getStatus(), url + ": " + response.asString() + " " + response.getHeader("WWW-Authenticate"));
                }
                assertEquals(401, SimpleHttp.create(missing).doGet(url).auth(tokens.getToken()).header("Accept", "application/json").asStatus(), url);
                assertEquals(401, SimpleHttp.create(different).doGet(url).auth(tokens.getToken()).header("Accept", "application/json").asStatus(), url);
            }

            realm.updateClientWithCleanup("bound-client", c -> c.attribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, "false"));
            for (String url : urls) {
                assertEquals(401, SimpleHttp.create(missing).doGet(url).auth(tokens.getToken()).header("Accept", "application/json").asStatus(), url);
            }

            assertTrue(SimpleHttp.create(missing).doPost(realm.getBaseUrl() + "/protocol/openid-connect/token/introspect")
                    .param("client_id", "bound-client").param("client_secret", "secret").param("token", tokens.getToken())
                    .asJson().get("active").asBoolean());

            AccessTokenResponse bearerTokens = getTokens(SimpleHttp.create(missing));
            for (String url : urls) {
                assertEquals(200, SimpleHttp.create(missing).doGet(url).auth(bearerTokens.getToken())
                        .header("Accept", "application/json").asStatus(), url);
            }

            ClientRepresentation boundClient = realm.admin().clients().findByClientId("bound-client").get(0);
            boundClient.getAttributes().put(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, "true");
            realm.admin().clients().get(boundClient.getId()).update(boundClient);
            for (String url : urls) {
                assertEquals(401, SimpleHttp.create(matching).doGet(url).auth(bearerTokens.getToken())
                        .header("Accept", "application/json").asStatus(), url);
            }
        }
    }

    @Test
    public void testCertificateBoundAccessTokenAsClaimToken() throws Exception {
        try (CloseableHttpClient matching = HttpClients.custom().setSSLContext(certificates.getClientSSLContext()).build();
             CloseableHttpClient missing = HttpClients.custom().setSSLContext(certificates.createClientSSLContext(null, "keycloak-truststore.bcfks", false)).build()) {
            AccessTokenResponse tokens = getTokens(SimpleHttp.create(matching));
            try (SimpleHttpResponse response = SimpleHttp.create(missing).doPost(realm.getBaseUrl() + "/protocol/openid-connect/token")
                    .authBasic("bound-client", "secret")
                    .param("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket")
                    .param("audience", "bound-client").param("claim_token", tokens.getToken()).asResponse()) {
                assertEquals(400, response.getStatus());
                assertEquals("invalid_claim_token", response.asJson(OAuth2ErrorRepresentation.class).getError());
            }
        }
    }

    @Test
    public void testStandardExchangeWithCertificateBoundToken() throws Exception {
        realm.updateClientWithCleanup("bound-client", c -> c.attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, "true"));
        try (CloseableHttpClient matching = HttpClients.custom().setSSLContext(certificates.getClientSSLContext()).build();
             CloseableHttpClient missing = HttpClients.custom().setSSLContext(certificates.createClientSSLContext(null, "keycloak-truststore.bcfks", false)).build();
             CloseableHttpClient different = HttpClients.custom().setSSLContext(certificates.createClientSSLContext("keycloak.bcfks", "keycloak-truststore.bcfks", true)).build()) {
            AccessTokenResponse tokens = getTokens(SimpleHttp.create(matching));
            for (CloseableHttpClient client : List.of(matching, missing, different)) {
                try (SimpleHttpResponse response = SimpleHttp.create(client).doPost(realm.getBaseUrl() + "/protocol/openid-connect/token")
                        .authBasic("bound-client", "secret")
                        .param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                        .param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
                        .param("subject_token", tokens.getToken()).asResponse()) {
                    assertEquals(client == matching ? 200 : 400, response.getStatus(), response.asString());
                    if (client == matching) {
                        String exchanged = response.asJson(AccessTokenResponse.class).getToken();
                        assertEquals(401, SimpleHttp.create(missing).doGet(realm.getBaseUrl() + "/account").auth(exchanged).header("Accept", "application/json").asStatus());
                        assertEquals(200, SimpleHttp.create(matching).doGet(realm.getBaseUrl() + "/account").auth(exchanged)
                                .header("Accept", "application/json").asStatus());
                    }
                }
            }
        }
    }

    private AccessTokenResponse getTokens(SimpleHttp http) throws Exception {
        try (SimpleHttpResponse response = http.doPost(realm.getBaseUrl() + "/protocol/openid-connect/token")
                .param("grant_type", "password").param("client_id", "bound-client").param("client_secret", "secret")
                .param("username", "admin").param("password", "password").param("scope", "openid")
                .asResponse()) {
            assertEquals(200, response.getStatus(), response.asString());
            return response.asJson(AccessTokenResponse.class);
        }
    }

    public static class TestRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create("bound-client").secret("secret").directAccessGrantsEnabled(true).fullScopeEnabled(true).defaultClientScopes("basic", "roles", "profile", "email", Constants.KEYCLOAK_ADMIN_API_SCOPE)
                            .attribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, "true")
                            .protocolMappers(ProtocolMapperBuilder.create().name("bound-client-audience")
                                    .protocol("openid-connect").protocolMapper("oidc-audience-mapper")
                                    .config("included.client.audience", "bound-client").config("access.token.claim", "true").build()))
                    .users(UserBuilder.create("admin").password("password").name("Test", "Admin").email("admin@example.com")
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                            .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE));
        }
    }

    public static class Certificates implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true).mTlsEnabled(true).keystoreFormat(KeystoreUtil.KeystoreFormat.BCFKS)
                    .stores("keycloak.bcfks", "keycloak-truststore.pem", "client.bcfks", "keycloak-truststore.bcfks");
        }
    }
}
