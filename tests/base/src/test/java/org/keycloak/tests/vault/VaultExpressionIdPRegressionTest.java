/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.vault;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.oauth.OAuth2IdentityProviderFactory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Regression test for vault expression security guard in identity provider configuration.
 * Tests the guard at IdentityProviderResource.java:246-248 that prevents a delegated manager
 * from changing critical IdP settings when vault expressions are involved.
 *
 * This test exercises the production Admin API, not just simulates the guard logic.
 * If the guard in IdentityProviderResource is removed, this test will fail.
 */
@KeycloakIntegrationTest
public class VaultExpressionIdPRegressionTest {

    private static final String REALM_NAME = "vault-idp-test";
    private static final String VAULT_EXPRESSION = "${vault.test_secret}";
    private static final String IDP_ALIAS = "test-oauth2-idp";
    private static final String IDP_CLIENT_ID = "test-idp-client";
    private static final String IDPMANAGER_USER = "idp-manager";
    private static final String REALM_MANAGER_USER = "realm-manager";
    private static final String PASSWORD = "password";

    @InjectRealm(config = VaultIdPRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testDelegatedManagerCannotChangeTokenUrlWithUnchangedVaultSecret() {
        // Realm manager creates IdP with vault expression
        try (Keycloak realmAdminClient = adminClientFactory.create()
                .realm(REALM_NAME)
                .username(REALM_MANAGER_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {
            IdentityProviderRepresentation idp = createOAuth2IdentityProvider(IDP_ALIAS, VAULT_EXPRESSION);
            try (Response response = realmAdminClient.realm(REALM_NAME).identityProviders().create(idp)) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                        "Realm manager should be able to create IdP with vault expression");
            }
        }

        // Delegated manager tries to change tokenUrl while keeping vault expression unchanged
        try (Keycloak delegatedAdminClient = adminClientFactory.create()
                .realm(REALM_NAME)
                .username(IDPMANAGER_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {
            IdentityProviderRepresentation idp = delegatedAdminClient.realm(REALM_NAME).identityProviders()
                    .get(IDP_ALIAS).toRepresentation();

            // Change tokenUrl while keeping vault secret unchanged
            String newTokenUrl = "https://attacker-evil.com/token";
            idp.getConfig().put(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, newTokenUrl);
            // clientSecret remains the vault expression

            // Guard should block this: vault secret + tokenUrl change = 403 FORBIDDEN
            ForbiddenException exception = assertThrows(ForbiddenException.class,
                    () -> delegatedAdminClient.realm(REALM_NAME).identityProviders().get(IDP_ALIAS).update(idp),
                    "Delegated manager should not be able to change tokenUrl with vault-backed clientSecret");
            assertThat(exception.getResponse().getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()));
        }
    }

    @Test
    public void testDelegatedManagerCanUpdateDisplayNameWithVaultSecret() {
        // Realm manager creates IdP with vault expression
        try (Keycloak realmAdminClient = adminClientFactory.create()
                .realm(REALM_NAME)
                .username(REALM_MANAGER_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {
            IdentityProviderRepresentation idp = createOAuth2IdentityProvider(IDP_ALIAS, VAULT_EXPRESSION);
            try (Response response = realmAdminClient.realm(REALM_NAME).identityProviders().create(idp)) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                        "Realm manager should be able to create IdP with vault expression");
            }
        }

        // Delegated manager updates display name (non-security-sensitive field)
        try (Keycloak delegatedAdminClient = adminClientFactory.create()
                .realm(REALM_NAME)
                .username(IDPMANAGER_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {
            IdentityProviderRepresentation idp = delegatedAdminClient.realm(REALM_NAME).identityProviders()
                    .get(IDP_ALIAS).toRepresentation();

            // Update only the display name, keeping vault secret unchanged
            String newDisplayName = "Updated Display Name";
            idp.setDisplayName(newDisplayName);

            // This should succeed (display name change is allowed, only tokenUrl change is blocked)
            delegatedAdminClient.realm(REALM_NAME).identityProviders().get(IDP_ALIAS).update(idp);

            // Verify the update was applied
            IdentityProviderRepresentation updated = delegatedAdminClient.realm(REALM_NAME).identityProviders()
                    .get(IDP_ALIAS).toRepresentation();
            assertEquals(newDisplayName, updated.getDisplayName(),
                    "Delegated manager should be able to update display name with vault-backed clientSecret");
        }
    }

    private IdentityProviderRepresentation createOAuth2IdentityProvider(String alias, String clientSecret) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId(OAuth2IdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);

        Map<String, String> config = new HashMap<>();
        config.put("clientId", IDP_CLIENT_ID);
        config.put("clientSecret", clientSecret);
        config.put("authorizationUrl", "https://example.com/auth");
        config.put(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "https://example.com/token");
        config.put("userInfoUrl", "https://example.com/userinfo");
        config.put("defaultScope", "openid");
        idp.setConfig(config);

        return idp;
    }

    public static class VaultIdPRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(REALM_NAME);

            // Delegated admin: may CRUD identity providers, but has no other realm permissions
            realm.users(UserBuilder.create(IDPMANAGER_USER)
                    .email("idp-manager@localhost")
                    .firstName("Idp")
                    .lastName("Manager")
                    .password(PASSWORD)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_IDENTITY_PROVIDERS));

            // Trusted with all realm secrets: may configure vault expressions
            realm.users(UserBuilder.create(REALM_MANAGER_USER)
                    .email("realm-manager@localhost")
                    .firstName("Realm")
                    .lastName("Manager")
                    .password(PASSWORD)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_REALM,
                            AdminRoles.MANAGE_IDENTITY_PROVIDERS));

            return realm;
        }
    }
}
