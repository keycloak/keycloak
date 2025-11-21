/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.migration;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.authentication.authenticators.broker.IdpConfirmLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpUsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserConfiguredAuthenticatorFactory;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.PrioritizedComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolFactory;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.exportimport.ExportImportUtil;
import org.keycloak.testsuite.runonserver.RunHelpers;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.theme.DefaultThemeSelectorProvider;
import org.keycloak.util.TokenUtil;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.xml.security.encryption.XMLCipher;
import org.hamcrest.Matchers;

import static org.keycloak.migration.migrators.MigrateTo24_0_0.REALM_USER_PROFILE_ENABLED;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_LINKS;
import static org.keycloak.models.AccountRoles.VIEW_GROUPS;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
import static org.keycloak.testsuite.Assert.assertNames;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractMigrationTest extends AbstractKeycloakTest {
    public static final String MIGRATION = "Migration";
    public static final String MIGRATION2 = "Migration2";
    protected RealmResource migrationRealm;
    protected RealmResource migrationRealm2;
    protected RealmResource masterRealm;

    protected void testMigratedData() {
        testMigratedData(true);
    }

    protected void testMigratedData(boolean supportsAuthzService) {
        log.info("testing migrated data");
        //master realm
        testMigratedMasterData();
        //migrationRealm
        testMigratedMigrationData(supportsAuthzService);
    }

    protected void testMigratedMigrationData(boolean supportsAuthzService) {
        assertNames(migrationRealm.roles().list(), "offline_access", "uma_authorization", "default-roles-migration", "migration-test-realm-role");
        List<String> expectedClientIds = new ArrayList<>(Arrays.asList("account", "account-console", "admin-cli", "broker", "migration-test-client", "migration-saml-client",
                "realm-management", "security-admin-console", "http://localhost:8280/sales-post-enc/"));

        if (supportsAuthzService) {
            expectedClientIds.add("authz-servlet");
            expectedClientIds.add("client-with-template");
        }

        assertNames(migrationRealm.clients().findAll(), expectedClientIds.toArray(new String[expectedClientIds.size()]));
        String id2 = migrationRealm.clients().findByClientId("migration-test-client").get(0).getId();
        assertNames(migrationRealm.clients().get(id2).roles().list(), "migration-test-client-role");
        assertNames(migrationRealm.users().search("", 0, 5), "migration-test-user", "offline-test-user");
        assertNames(migrationRealm.groups().groups(), "migration-test-group");
    }

    protected void testMigratedMasterData() {
        assertNames(masterRealm.roles().list(), "offline_access", "uma_authorization", "default-roles-master", "create-realm", "master-test-realm-role", "admin");
        assertNames(masterRealm.clients().findAll(), "admin-cli", "security-admin-console", "broker", "account", "account-console",
                "master-realm", "master-test-client", "Migration-realm", "Migration2-realm");
        String id = masterRealm.clients().findByClientId("master-test-client").get(0).getId();
        assertNames(masterRealm.clients().get(id).roles().list(), "master-test-client-role");
        assertNames(masterRealm.users().search("", 0, 5), "admin", "master-test-user");
        assertNames(masterRealm.groups().groups(), "master-test-group");
    }

    protected void testRhssoThemes(RealmResource realm) {
        // check themes are removed
        RealmRepresentation rep = realm.toRepresentation();
        assertThat("Login theme modified for test purposes", rep.getLoginTheme(), anyOf(nullValue(), equalTo(PREFERRED_DEFAULT_LOGIN_THEME)));
        Assert.assertNull("Email theme was not modified", rep.getEmailTheme());
        // there should be either new default or left null if not set
        assertThat("Account theme was not modified", rep.getAccountTheme(), anyOf(equalTo("keycloak.v2"), nullValue()));
        // check the client theme is also removed
        List<ClientRepresentation> client = realm.clients().findByClientId("migration-saml-client");
        Assert.assertNotNull("migration-saml-client client is missing", client);
        Assert.assertEquals("migration-saml-client client is missing", 1, client.size());
        Assert.assertNull("migration-saml-client login theme was not removed", client.get(0).getAttributes().get(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));
    }

    protected void testHttpChallengeFlow(RealmResource realm) {
        log.info("testing 'http challenge' flow not present");
        Assert.assertFalse(realm.flows().getFlows()
                .stream()
                .anyMatch(authFlow -> authFlow.getAlias().equalsIgnoreCase("http challenge")));
    }

    protected void testUserProfile(RealmResource realm) {
        // check user profile config
        List<ComponentRepresentation> userProfileComponents = realm.components().query(null, "org.keycloak.userprofile.UserProfileProvider");
        assertThat(userProfileComponents, hasSize(1));

        ComponentRepresentation component = userProfileComponents.get(0);
        assertThat(component.getProviderId(), equalTo("declarative-user-profile"));
        assertThat(component.getConfig().size(), equalTo(1));
        assertThat(component.getConfig().getList(UP_COMPONENT_CONFIG_KEY), not(empty()));
    }

    protected void testRegistrationProfileFormActionRemoved(RealmResource realm) {
        AuthenticationFlowRepresentation registrationFlow = realm.flows().getFlows().stream()
                .filter(flowRep -> DefaultAuthenticationFlows.REGISTRATION_FLOW.equals(flowRep.getAlias()))
                .findFirst().orElseThrow(() -> new NoSuchElementException("No registration flow in realm " + realm.toRepresentation().getRealm()));

        Assert.assertFalse(realm.flows().getExecutions(registrationFlow.getAlias())
                .stream()
                .anyMatch(execution -> "registration-profile-action".equals(execution.getProviderId())));
    }

    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_0_0
     */
    protected void testMigrationTo2_0_0() {
        testAuthorizationServices(masterRealm, migrationRealm);
    }

    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_1_0
     */
    protected void testMigrationTo2_1_0() {
        testNameOfOTPRequiredAction(masterRealm, migrationRealm);
    }

    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_2_0
     */
    protected void testMigrationTo2_2_0() {
        testIdentityProviderAuthenticator(masterRealm, migrationRealm);
        //MigrateTo2_2_0#migrateRolePolicies is not relevant any more
    }

    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_3_0
     */
    protected void testMigrationTo2_3_0() {
        testUpdateProtocolMappers(masterRealm, migrationRealm);
        testExtractRealmKeysMasterRealm(masterRealm);
        testExtractRealmKeysMigrationRealm(migrationRealm);
    }

    /**
     * @see org.keycloak.migration.migrators.MigrateTo2_5_0
     */
    protected void testMigrationTo2_5_0() {
        testLdapKerberosMigration_2_5_0();
        //https://github.com/keycloak/keycloak/pull/3630
        testDuplicateEmailSupport(masterRealm, migrationRealm);
    }

    /**
     * @see org.keycloak.migration.migrators.MigrateTo3_0_0
     */
    protected void testMigrationTo3_0_0() {
        testRoleManageAccountLinks(masterRealm, migrationRealm);
    }

    protected void testMigrationTo3_2_0() {
        assertNull(masterRealm.toRepresentation().getPasswordPolicy());
        assertNull(migrationRealm.toRepresentation().getPasswordPolicy());

        testDockerAuthenticationFlow(masterRealm, migrationRealm);
    }

    protected void testMigrationTo3_4_0() {
        Map<String, String> securityHeaders = masterRealm.toRepresentation().getBrowserSecurityHeaders();
        if (securityHeaders != null) {
            assertEquals("max-age=31536000; includeSubDomains",
                    securityHeaders.get("strictTransportSecurity"));
        } else {
            fail("Browser security headers not found");
        }
    }

    protected void testMigrationTo3_4_1() {
        Map<String, String> securityHeaders = masterRealm.toRepresentation().getBrowserSecurityHeaders();
        if (securityHeaders != null) {
            assertEquals("frame-src 'self'; frame-ancestors 'self'; object-src 'none';",
                    securityHeaders.get("contentSecurityPolicy"));
        } else {
            fail("Browser security headers not found");
        }
    }

    protected void testMigrationTo3_4_2() {
        testCliConsoleScopeSize(this.masterRealm);
        testCliConsoleScopeSize(this.migrationRealm);
    }

    protected void testMigrationTo4_0_0() {
        testRealmDefaultClientScopes(this.masterRealm);
        testRealmDefaultClientScopes(this.migrationRealm);
        testClientDefaultClientScopes(this.migrationRealm);
        testOfflineScopeAddedToClient();
    }

    protected void testMigrationTo4_2_0(boolean supportsAuthzService) {
        testRequiredActionsPriority(this.masterRealm, this.migrationRealm);

        if (supportsAuthzService) {
            testResourceWithMultipleUris();
        }
    }

    protected void testMigrationTo4_6_0(boolean supportsAuthzService, boolean checkMigrationData) {
        if (supportsAuthzService && checkMigrationData) {
            testGroupPolicyTypeFineGrainedAdminPermission();
        }

        // NOTE: Fact that 'roles' and 'web-origins' scope were added was tested in testMigrationTo4_0_0 already
        testRolesAndWebOriginsScopesAddedToClient();
    }

    protected void testMigrationTo6_0_0() {
        // check that all expected scopes exist in the migrated realm.
        testRealmDefaultClientScopes(migrationRealm);
        // check that the 'microprofile-jwt' scope was added to the migrated clients.
        testMicroprofileJWTScopeAddedToClient();
    }

    protected void testMigrationTo8_0_0() {
        // Common
        testAdminClientUrls(masterRealm);
        testAdminClientUrls(migrationRealm);
        testAccountClientUrls(masterRealm);
        testAccountClientUrls(migrationRealm);

        // MFA - Check that credentials were created for user and are available
        testCredentialsMigratedToNewFormat();

        // MFA - Check that authentication flows were migrated as expected
        testOTPAuthenticatorsMigratedToConditionalFlow();

        testResourceTag();
    }

    protected void testMigrationTo9_0_0() {
        testAccountConsoleClient(masterRealm);
        testAccountConsoleClient(migrationRealm);
        testAlwaysDisplayInConsole();

        // master realm is not imported from json
        testFirstBrokerLoginFlowMigrated(masterRealm, false);
        testFirstBrokerLoginFlowMigrated(migrationRealm, true);
        testAccountClient(masterRealm);
        testAccountClient(migrationRealm);
        testAdminClientPkce(masterRealm);
        testAdminClientPkce(migrationRealm);
        testUserLocaleActionAdded(masterRealm);
        testUserLocaleActionAdded(migrationRealm);
    }

    protected void testMigrationTo12_0_0() {
        testDeleteAccount(masterRealm);
        testDeleteAccount(migrationRealm);
    }

    protected void testMigrationTo13_0_0(boolean testRealmAttributesMigration) {
        testDefaultRoles(masterRealm);
        testDefaultRoles(migrationRealm);

        testDefaultRolesNameWhenTaken();
        if (testRealmAttributesMigration) {
            testRealmAttributesMigration();
        }
    }

    protected void testMigrationTo14_0_0() {
        testSamlAttributes(migrationRealm);
    }

    protected void testMigrationTo18_0_0() {
        // check that all expected scopes exist in the migrated realm.
        testRealmDefaultClientScopes(migrationRealm);
    }

    protected void testMigrationTo19_0_0() {
        testPostLogoutRedirectUrisSet(migrationRealm);
    }

    protected void testMigrationTo20_0_0() {
        testViewGroups(masterRealm);
        testViewGroups(migrationRealm);
    }

    protected void testMigrationTo21_0_2() {
        testTermsAndConditionsMigrated(masterRealm);
        testTermsAndConditionsMigrated(migrationRealm);
        testTermsAndConditionsMigrated(migrationRealm2);
    }

    protected void testMigrationTo22_0_0() {
        testRhssoThemes(migrationRealm);
        testHttpChallengeFlow(migrationRealm);
    }

    /**
     * @param testUserProfileMigration whether a migrated realm contains a user profile component or not.
     */
    protected void testMigrationTo23_0_0(boolean testUserProfileMigration) {
        if (testUserProfileMigration) testUserProfile(migrationRealm2);
        testRegistrationProfileFormActionRemoved(migrationRealm2);
    }

    protected void testMigrationTo24_0_0(boolean testUserProfileMigration, boolean testLdapUseTruststoreSpiMigration) {
        if (testUserProfileMigration) {
            testUserProfileEnabledByDefault(migrationRealm);
            testUnmanagedAttributePolicySet(migrationRealm, UnmanagedAttributePolicy.ENABLED);
            testUserProfileEnabledByDefault(migrationRealm2);
            testUnmanagedAttributePolicySet(migrationRealm2, null);
            testHS512KeyCreated(migrationRealm);
            testHS512KeyCreated(migrationRealm2);
            testClientAttributes(migrationRealm);
            testDeleteCredentialActionAvailable(migrationRealm);
        }
        if (testLdapUseTruststoreSpiMigration) {
            testLdapUseTruststoreSpiMigration(migrationRealm2);
        }
    }

    protected void testMigrationTo25_0_0() {
        // check that all expected scopes exist in the migrated realm.
        testRealmDefaultClientScopes(migrationRealm);
        testClientContainsExpectedClientScopes();
    }

    protected void testMigrationTo26_0_0(boolean testIdentityProviderConfigMigration) {
        if (testIdentityProviderConfigMigration) {
            testIdentityProviderConfigMigration(migrationRealm2);
        }
        testLightweightClientAndFullScopeAllowed(masterRealm, Constants.ADMIN_CONSOLE_CLIENT_ID);
        testLightweightClientAndFullScopeAllowed(masterRealm, Constants.ADMIN_CLI_CLIENT_ID);
        testLightweightClientAndFullScopeAllowed(migrationRealm, Constants.ADMIN_CONSOLE_CLIENT_ID);
        testLightweightClientAndFullScopeAllowed(migrationRealm, Constants.ADMIN_CLI_CLIENT_ID);
    }

    protected void testMigrationTo26_1_0(boolean testIdentityProviderConfigMigration) {
        testRealmDefaultClientScopes(migrationRealm);
    }

    protected void testMigrationTo26_3_0() {
        testIdpLinkActionAvailable(migrationRealm);
    }

    protected void testMigrationTo26_4_0() {
        testSamlEncryptionAttributes(migrationRealm);
    }

    private void testClientContainsExpectedClientScopes() {
        // Test OIDC client contains expected client scopes
        ClientResource migrationTestOIDCClient = ApiUtil.findClientByClientId(migrationRealm, "migration-test-client");
        List<String> defaultClientScopes = migrationTestOIDCClient.getDefaultClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toList());
        List<String> optionalClientScopes = migrationTestOIDCClient.getOptionalClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toList());

        assertThat(defaultClientScopes, Matchers.hasItems(
                OIDCLoginProtocolFactory.BASIC_SCOPE,
                OIDCLoginProtocolFactory.ACR_SCOPE,
                OAuth2Constants.SCOPE_PROFILE,
                OAuth2Constants.SCOPE_EMAIL
        ));
        assertThat(optionalClientScopes, Matchers.hasItems(
                OAuth2Constants.SCOPE_ADDRESS,
                OAuth2Constants.OFFLINE_ACCESS,
                OAuth2Constants.SCOPE_PHONE
        ));

        // Test SAML client
        ClientResource migrationTestSAMLClient = ApiUtil.findClientByClientId(migrationRealm, "migration-saml-client");
        defaultClientScopes = migrationTestSAMLClient.getDefaultClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toList());
        optionalClientScopes = migrationTestSAMLClient.getOptionalClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toList());
        assertThat(defaultClientScopes, Matchers.hasItems(
                SamlProtocolFactory.SCOPE_ROLE_LIST
        ));
        Assert.assertTrue(optionalClientScopes.isEmpty());
    }

    protected void testDeleteAccount(RealmResource realm) {
        ClientRepresentation accountClient = realm.clients().findByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource accountResource = realm.clients().get(accountClient.getId());

        assertNotNull(accountResource.roles().get(AccountRoles.DELETE_ACCOUNT).toRepresentation());
        assertNotNull(realm.flows().getRequiredAction("delete_account"));
    }

    private void testAccountClient(RealmResource realm) {
        ClientRepresentation accountClient = realm.clients().findByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);

        ClientResource accountResource = realm.clients().get(accountClient.getId());
        RoleRepresentation viewAppRole = accountResource.roles().get(AccountRoles.VIEW_APPLICATIONS).toRepresentation();
        assertNotNull(viewAppRole);
        RoleRepresentation viewConsentRole = accountResource.roles().get(AccountRoles.VIEW_CONSENT).toRepresentation();
        assertNotNull(viewConsentRole);
        RoleResource manageConsentResource = accountResource.roles().get(AccountRoles.MANAGE_CONSENT);
        RoleRepresentation manageConsentRole = manageConsentResource.toRepresentation();
        assertNotNull(manageConsentRole);
        assertTrue(manageConsentRole.isComposite());
        Set<RoleRepresentation> composites = manageConsentResource.getRoleComposites();
        assertEquals(1, composites.size());
        assertEquals(viewConsentRole.getId(), composites.iterator().next().getId());
    }

    private void testAdminClientUrls(RealmResource realm) {
        ClientRepresentation adminConsoleClient = realm.clients().findByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID).get(0);

        assertEquals(Constants.AUTH_ADMIN_URL_PROP, adminConsoleClient.getRootUrl());
        String baseUrl = "/admin/" + realm.toRepresentation().getRealm() + "/console/";
        assertEquals(baseUrl, adminConsoleClient.getBaseUrl());
        assertEquals(baseUrl + "*", adminConsoleClient.getRedirectUris().iterator().next());
        assertEquals(1, adminConsoleClient.getRedirectUris().size());
        assertEquals("+", adminConsoleClient.getWebOrigins().iterator().next());
        assertEquals(1, adminConsoleClient.getWebOrigins().size());
    }

    private void testAdminClientPkce(RealmResource realm) {
        ClientRepresentation adminConsoleClient = realm.clients().findByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID).get(0);
        assertEquals("S256", adminConsoleClient.getAttributes().get(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD));
    }

    private void testAccountClientUrls(RealmResource realm) {
        ClientRepresentation accountConsoleClient = realm.clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);

        assertEquals(Constants.AUTH_BASE_URL_PROP, accountConsoleClient.getRootUrl());
        String baseUrl = "/realms/" + realm.toRepresentation().getRealm() + "/account/";
        assertEquals(baseUrl, accountConsoleClient.getBaseUrl());
        assertEquals(baseUrl + "*", accountConsoleClient.getRedirectUris().iterator().next());
        assertEquals(1, accountConsoleClient.getRedirectUris().size());
    }

    private void testAccountConsoleClient(RealmResource realm) {
        ClientRepresentation accountConsoleClient = realm.clients().findByClientId(Constants.ACCOUNT_CONSOLE_CLIENT_ID).get(0);

        assertEquals(Constants.AUTH_BASE_URL_PROP, accountConsoleClient.getRootUrl());
        assertEquals("/realms/" + realm.toRepresentation().getRealm() + "/account/", accountConsoleClient.getBaseUrl());
        assertTrue(accountConsoleClient.isPublicClient());
        assertFalse(accountConsoleClient.isFullScopeAllowed());
        assertTrue(accountConsoleClient.isStandardFlowEnabled());
        assertFalse(accountConsoleClient.isDirectAccessGrantsEnabled());
        assertEquals("S256", accountConsoleClient.getAttributes().get(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD));

        ClientResource clientResource = realm.clients().get(accountConsoleClient.getId());

        MappingsRepresentation scopes = clientResource.getScopeMappings().getAll();
        assertNull(scopes.getRealmMappings());
        assertEquals(1, scopes.getClientMappings().size());
        assertEquals(2, scopes.getClientMappings().get(ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings().size());
        Assert.assertNames(scopes.getClientMappings().get(ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(), MANAGE_ACCOUNT, VIEW_GROUPS);
        List<ProtocolMapperRepresentation> mappers = clientResource.getProtocolMappers().getMappers();
        assertEquals(1, mappers.size());
        assertEquals("oidc-audience-resolve-mapper", mappers.get(0).getProtocolMapper());
    }

    private void testFirstBrokerLoginFlowMigrated(RealmResource realm, boolean imported) {
        log.infof("Test that firstBrokerLogin flow was migrated in new realm '%s'", realm.toRepresentation().getRealm());

        List<AuthenticationExecutionInfoRepresentation> authExecutions = realm.flows().getExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW);

        testAuthenticationExecution(authExecutions.get(0), null,
                IdpReviewProfileAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED, 0, 0);

        testAuthenticationExecution(authExecutions.get(1), true,
                null, AuthenticationExecutionModel.Requirement.REQUIRED, 0, 1);

        testAuthenticationExecution(authExecutions.get(2), null,
                IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.ALTERNATIVE, 1, 0);

        testAuthenticationExecution(authExecutions.get(3), true,
                null, AuthenticationExecutionModel.Requirement.ALTERNATIVE, 1, 1);

        testAuthenticationExecution(authExecutions.get(4), null,
                IdpConfirmLinkAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED, 2, 0);

        testAuthenticationExecution(authExecutions.get(5), true,
                null, AuthenticationExecutionModel.Requirement.REQUIRED, 2, 1);

        testAuthenticationExecution(authExecutions.get(6), null,
                IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.ALTERNATIVE, 3, 0);

        testAuthenticationExecution(authExecutions.get(7), true,
                null, AuthenticationExecutionModel.Requirement.ALTERNATIVE, 3, 1);

        testAuthenticationExecution(authExecutions.get(8), null,
                IdpUsernamePasswordFormFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED, 4, 0);

        testAuthenticationExecution(authExecutions.get(9), true,
                null, AuthenticationExecutionModel.Requirement.CONDITIONAL, 4, 1);

        // There won't be a requirement in the future, so this test would need to change
        testAuthenticationExecution(authExecutions.get(10), null,
                ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED, 5, 0);

        AuthenticationExecutionModel.Requirement requirement = imported ? AuthenticationExecutionModel.Requirement.REQUIRED : AuthenticationExecutionModel.Requirement.ALTERNATIVE;
        testAuthenticationExecution(imported ? authExecutions.get(11) : authExecutions.get(12), null,
                OTPFormAuthenticatorFactory.PROVIDER_ID, requirement, 5, imported ? 1 : 2);
    }


    private void testAuthenticationExecution(AuthenticationExecutionInfoRepresentation execution, Boolean expectedAuthenticationFlow, String expectedProviderId,
                                             AuthenticationExecutionModel.Requirement expectedRequirement, int expectedLevel, int expectedIndex) {
        Assert.assertEquals(execution.getAuthenticationFlow(), expectedAuthenticationFlow);
        Assert.assertEquals(execution.getProviderId(), expectedProviderId);
        Assert.assertEquals(execution.getRequirement(), expectedRequirement.toString());
        Assert.assertEquals(execution.getLevel(), expectedLevel);
        Assert.assertEquals(execution.getIndex(), expectedIndex);
    }

    private void testDecisionStrategySetOnResourceServer() {
        ClientsResource clients = migrationRealm.clients();
        ClientRepresentation clientRepresentation = clients.findByClientId("authz-servlet").get(0);
        ResourceServerRepresentation settings = clients.get(clientRepresentation.getId()).authorization().getSettings();
        assertEquals(DecisionStrategy.UNANIMOUS, settings.getDecisionStrategy());
    }

    private void testGroupPolicyTypeFineGrainedAdminPermission() {
        ClientsResource clients = migrationRealm.clients();
        ClientRepresentation clientRepresentation = clients.findByClientId("realm-management").get(0);
        List<ResourceRepresentation> resources = clients.get(clientRepresentation.getId()).authorization().resources().resources();

        assertEquals(5, resources.size());

        for (ResourceRepresentation resource : resources) {
            assertEquals("Group", resource.getType());
        }
    }

    private void testCliConsoleScopeSize(RealmResource realm) {
        ClientRepresentation cli = realm.clients().findByClientId(Constants.ADMIN_CLI_CLIENT_ID).get(0);
        ClientRepresentation console = realm.clients().findByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID).get(0);
        MappingsRepresentation scopeMappings = realm.clients().get(console.getId()).getScopeMappings().getAll();
        Assert.assertNull(scopeMappings.getClientMappings());
        Assert.assertNull(scopeMappings.getRealmMappings());

        scopeMappings = realm.clients().get(cli.getId()).getScopeMappings().getAll();
        Assert.assertNull(scopeMappings.getClientMappings());
        Assert.assertNull(scopeMappings.getRealmMappings());
    }

    protected void testDockerAuthenticationFlow(RealmResource... realms) {
        for (RealmResource realm : realms) {
            AuthenticationFlowRepresentation flow = null;
            for (AuthenticationFlowRepresentation f : realm.flows().getFlows()) {
                if (DefaultAuthenticationFlows.DOCKER_AUTH.equals(f.getAlias())) {
                    flow = f;
                }
            }
            assertNotNull(flow);
        }
    }

    protected void testViewGroups(RealmResource realm) {
        ClientRepresentation accountClient = realm.clients().findByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);

        ClientResource accountResource = realm.clients().get(accountClient.getId());
        RoleRepresentation viewAppRole = accountResource.roles().get(VIEW_GROUPS).toRepresentation();
        assertNotNull(viewAppRole);
    }

    protected void testTermsAndConditionsMigrated(RealmResource realmResource) {
        final String legacyTermsAndConditionsAlias = "terms_and_conditions";
        // Test realm RequiredAction migrated
        RealmRepresentation realm = realmResource.toRepresentation();
        List<RequiredActionProviderRepresentation> requiredActions = realm.getRequiredActions();

        if (requiredActions != null && !requiredActions.isEmpty()) {
            assertThat(requiredActions.stream()
                    .map(RequiredActionProviderRepresentation::getAlias)
                    .collect(Collectors.toList()), not(hasItem(legacyTermsAndConditionsAlias)));
            assertThat(requiredActions.stream()
                    .map(RequiredActionProviderRepresentation::getProviderId)
                    .collect(Collectors.toList()), not(hasItem(legacyTermsAndConditionsAlias)));
        }

        List<UserRepresentation> users = realmResource.users().list(null, null);

        if (users != null && !users.isEmpty()) {
            // Test users required actions migrated
            assertThat(users.stream()
                            .flatMap(user -> user.getRequiredActions().stream())
                            .collect(Collectors.toList()),
                    not(hasItem(legacyTermsAndConditionsAlias)));
        }
    }

    protected void testRoleManageAccountLinks(RealmResource... realms) {
        log.info("testing role manage account links");
        for (RealmResource realm : realms) {
            List<ClientRepresentation> clients = realm.clients().findByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID);
            if (!clients.isEmpty()) {
                String accountClientId = clients.get(0).getId();
                ClientResource accountClient = realm.clients().get(accountClientId);
                accountClient.roles().get(MANAGE_ACCOUNT_LINKS).toRepresentation(); //the role should be presented, it'll throw javax.ws.rs.NotFoundException in case the role is not found

                Set<RoleRepresentation> roleComposites = accountClient.roles().get(MANAGE_ACCOUNT).getRoleComposites();
                boolean success = false;
                for (RoleRepresentation roleComposite : roleComposites) {
                    if (roleComposite.getName().equals(MANAGE_ACCOUNT_LINKS)) {
                        success = true;
                    }
                }
                if (!success) {
                    fail("'manage-account' role of client 'account' should have composite role 'manage-account-links'.");
                }
            }
        }
    }

    protected void testExtractRealmKeysMasterRealm(RealmResource masterRealm) {
        log.info("testing extract realm keys");
        String expectedMasterRealmKey = "MIIEowIBAAKCAQEAiU54OXoCbHy0L0gHn1yasctcnKHRU1pHFIJnWvaI7rClJydet9dDJaiYXOxMKseiBm3eYznfN3cPyU8udYmRnMuKjiocZ77LT2IEttAjXb6Ggazx7loriFHRy0IOJeX4KxXhAPWmxqa3mkFNfLBEvFqVaBgUDHQ60cmnPvNSHYudBTW9K80s8nvmP2pso7HTwWJ1+Xatj1Ey/gTmB3CXlyqBegGWC9TeuErEYpYhdh+11TVWasgMBZyUCtL3NRPaBuhaPg1LpW8lWGk05nS+YM6dvTk3Mppv+z2RygEpxyO09oT3b4G+Zfwit1STqn0AvDTGzINdoKcNtFScV0j8TwIDAQABAoIBAHcbPKsPLZ8SJfOF1iblW8OzFulAbaaSf2pJHIMJrQrw7LKkMkPjVXoLX+/rgr7xYZmWIP2OLBWfEHCeYTzQUyHiZpSf7vgHx7Fa45/5uVQOe/ttHIiYa37bCtP4vvEdJkOpvP7qGPvljwsebqsk9Ns28LfVez66bHOjK5Mt2yOIulbTeEs7ch//h39YwKJv96vc+CHbV2O6qoOxZessO6y+287cOBvbFXmS2GaGle5Nx/EwncBNS4b7czoetmm70+9ht3yX+kxaP311YUT31KQjuaJt275kOiKsrXr27PvgO++bsIyGuSzqyS7G7fmxF2zUyphEqEpalyDGMKMnrAECgYEA1fCgFox03rPDjm0MhW/ThoS2Ld27sbWQ6reS+PBMdUTJZVZIU1D2//h6VXDnlddhk6avKjA4smdy1aDKzmjz3pt9AKn+kgkXqtTC2fD3wp+fC9hND0z+rQPGe/Gk7ZUnTdsqnfyowxr+woIgzdnRukOUrG+xQiP3RUUT7tt6NQECgYEApEz2xvgqMm+9/f/YxjLdsFUfLqc4WlafB863stYEVqlCYy5ujyo0VQ0ahKSKJkLDnf52+aMUqPOpwaGePpu3O6VkvpcKfPY2MUlZW7/6Sa9et9hxNkdTS7Gui2d1ELpaCBe1Bc62sk8EA01iHXE1PpvyUqDWrhNh+NrDICA9oU8CgYBgGDYACtTP11TmW2r9YK5VRLUDww30k4ZlN1GnyV++aMhBYVEZQ0u+y+A/EnijIFwu0vbo70H4OGknNZMCxbeMbLDoJHM5KyZbUDe5ZvgSjloFGwH59m6KTiDQOUkIgi9mVCQ/VGaFRFHcElEjxUvj60kTbxPijn8ZuR5r8l9hAQKBgQCQ9jL5pHWeoIayN20smi6M6N2lTPbkhe60dcgQatHTIG2pkosLl8IqlHAkPgSB84AiwyR351JQKwRJCm7TcJI/dxMnMZ6YWKfB3qSP1hdfsfJRJQ/mQxIUBAYrizF3e+P5peka4aLCOgMhYsJBlePThMZN7wja99EGPwXQL4IQ8wKBgB8Nis1lQK6Z30GCp9u4dYleGfEP71Lwqvk/eJb89/uz0fjF9CTpJMULFc+nA5u4yHP3LFnRg3zCU6aEwfwUyk4GH9lWGV/qIAisQtgrCEraVe4qxz0DVE59C7qjO26IhU2U66TEzPAqvQ3zqey+woDn/cz/JMWK1vpcSk+TKn3K";
        String realmId = masterRealm.toRepresentation().getId();
        List<ComponentRepresentation> components = masterRealm.components().query(realmId, KeyProvider.class.getName());
        assertEquals(3, components.size());

        components = masterRealm.components().query(realmId, KeyProvider.class.getName(), "rsa");
        assertEquals(1, components.size());

        ComponentRepresentation component = testingClient.server(MASTER).fetch(RunHelpers.internalComponent(components.get(0).getId()));
        assertEquals(expectedMasterRealmKey, component.getConfig().getFirst("privateKey"));

        components = masterRealm.components().query(realmId, KeyProvider.class.getName(), "hmac-generated");
        assertEquals(1, components.size());

    }

    protected void testExtractRealmKeysMigrationRealm(RealmResource migrationRealm) {
        log.info("testing extract realm keys");
        String expectedMigrationRealmKey = "MIIEpAIBAAKCAQEApt6gCllWkVTZ7fy/oRIx6Bxjt9x3eKKyKGFXvN4iaafrNqpYU9lcqPngWJ9DyXGqUf8RpjPaQWiLWLxjw3xGBqLk2E1/Frb9e/dy8rj//fHGq6bujN1iguzyFwxPGT5Asd7jflRI3qU04M8JE52PArqPhGL2Fn+FiSK5SWRIGm+hVL7Ck/E/tVxM25sFG1/UTQqvrROm4q76TmP8FsyZaTLVf7cCwW2QPIX0N5HTVb3QbBb5KIsk4kKmk/g7uUxS9r42tu533LISzRr5CTyWZAL2XFRuF2RrKdE8gwqkEubw6sDmB2mE0EoPdY1DUhBQgVP/5rwJrCtTsUBR2xdEYQIDAQABAoIBAFbbsNBSOlZBpYJUOmcb8nBQPrOYhXN8tGGCccn0klMOvcdhmcJjdPDbyCQ5Gm7DxJUTwNsTSHsdcNMKlJ9Pk5+msJnKlOl87KrXXbTsCQvlCrWUmb0nCzz9GvJWTOHl3oT3cND0DE4gDksqWR4luCgCdevCGzgQvrBoK6wBD+r578uEW3iw10hnJ0+wnGiw8IvPzE1a9xbY4HD8/QrYdaLxuLb/aC1PDuzrz0cOjnvPkrws5JrbUSnbFygJiOv1z4l2Q00uGIxlHtXdwQBnTZZjVi4vOec2BYSHffgwDYEZIglw1mnrV7y0N1nnPbtJK/cegIkXoBQHXm8Q99TrWMUCgYEA9au86qcwrXZZg5H4BpR5cpy0MSkcKDbA1aRL1cAyTCqJxsczlAtLhFADF+NhnlXj4y7gwDEYWrz064nF73I+ZGicvCiyOy+tCTugTyTGS+XR948ElDMS6PCUUXsotS3dKa0b3c9wd2mxeddTjq/ArfgEVZJ6fE1KtjLt9dtfA+8CgYEAreK3JsvjR5b/Xct28TghYUU7Qnasombb/shqqy8FOMjYUr5OUm/OjNIgoCqhOlE8oQDJ4dOZofNSa7tL+oM8Gmbal+E3fRzxnx/9/EC4QV6sVaPLTIyk7EPfKTcZuzH7+BNZtAziTxJw9d6YJQRbkpg92EZIEoR8iDj2Xs5xrK8CgYEAwMVWwwYX8zT3vn7ukTM2LRH7bsvkVUXJgJqgCwT6Mrv6SmkK9vL5+cPS+Y6pjdW1sRGauBSOGL1Grf/4ug/6F03jFt4UJM8fRyxreU7Q7sNSQ6AMpsGA6BnHODycz7ZCYa59PErG5FyiL4of/cm5Nolz1TXQOPNpWZiTEqVlZC8CgYA4YPbjVF4nuxSnU64H/hwMjsbtAM9uhI016cN0J3W4+J3zDhMU9X1x+Tts0wWdg/N1fGz4lIQOl3cUyRCUc/KL2OdtMS+tmDHbVyMho9ZaE5kq10W2Vy+uDz+O/HeSU12QDK4cC8Vgv+jyPy7zaZtLR6NduUPrBRvfiyCOkr8WrwKBgQCY0h4RCdNFhr0KKLLmJipAtV8wBCGcg1jY1KoWKQswbcykfBKwHbF6EooVqkRW0ITjWB7ZZCf8TnSUxe0NXCUAkVBrhzS4DScgtoSZYOOUaSHgOxpfwgnQ3oYotKi98Yg3IsaLs1j4RuPG5Sp1z6o+ELP1uvr8azyn9YlLa+523Q==";
        String realmId = migrationRealm.toRepresentation().getId();
        List<ComponentRepresentation> components = migrationRealm.components().query(realmId, KeyProvider.class.getName());
        assertEquals(4, components.size());

        components = migrationRealm.components().query(realmId, KeyProvider.class.getName(), "rsa");
        assertEquals(1, components.size());

        ComponentRepresentation component = testingClient.server(MIGRATION).fetch(RunHelpers.internalComponent(components.get(0).getId()));
        assertEquals(expectedMigrationRealmKey, component.getConfig().getFirst("privateKey"));

        components = migrationRealm.components().query(realmId, KeyProvider.class.getName(), "hmac-generated-hs512");
        assertEquals(1, components.size());
    }

    protected void testLdapKerberosMigration_2_5_0() {
        log.info("testing ldap kerberos migration");
        RealmRepresentation realmRep = migrationRealm2.toRepresentation();
        List<ComponentRepresentation> components = migrationRealm2.components().query(realmRep.getId(), UserStorageProvider.class.getName());
        assertEquals(2, components.size());
        boolean testedLdap = false;
        boolean testedKerberos = false;

        for (ComponentRepresentation component : components) {
            if (component.getName().equals("ldap-provider")) {
                assertEquals("2", component.getConfig().getFirst(PrioritizedComponentModel.PRIORITY));
                assertEquals("READ_ONLY", component.getConfig().getFirst(LDAPConstants.EDIT_MODE));
                assertEquals("true", component.getConfig().getFirst(LDAPConstants.SYNC_REGISTRATIONS));
                assertEquals(LDAPConstants.VENDOR_RHDS, component.getConfig().getFirst(LDAPConstants.VENDOR));
                assertEquals("uid", component.getConfig().getFirst(LDAPConstants.USERNAME_LDAP_ATTRIBUTE));
                assertEquals("uid", component.getConfig().getFirst(LDAPConstants.RDN_LDAP_ATTRIBUTE));
                assertEquals("nsuniqueid", component.getConfig().getFirst(LDAPConstants.UUID_LDAP_ATTRIBUTE));
                assertEquals("inetOrgPerson, organizationalPerson", component.getConfig().getFirst(LDAPConstants.USER_OBJECT_CLASSES));
                assertEquals("http://localhost", component.getConfig().getFirst(LDAPConstants.CONNECTION_URL));
                assertEquals("dn", component.getConfig().getFirst(LDAPConstants.USERS_DN));
                assertEquals(LDAPConstants.AUTH_TYPE_NONE, component.getConfig().getFirst(LDAPConstants.AUTH_TYPE));
                assertEquals("true", component.getConfig().getFirst(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION));
                assertEquals("realm", component.getConfig().getFirst(KerberosConstants.KERBEROS_REALM));
                assertEquals("principal", component.getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL));
                assertEquals("keytab", component.getConfig().getFirst(KerberosConstants.KEYTAB));
                testedLdap = true;
            } else if (component.getName().equals("kerberos-provider")) {
                assertEquals("3", component.getConfig().getFirst(PrioritizedComponentModel.PRIORITY));
                assertEquals("realm", component.getConfig().getFirst(KerberosConstants.KERBEROS_REALM));
                assertEquals("principal", component.getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL));
                assertEquals("keytab", component.getConfig().getFirst(KerberosConstants.KEYTAB));
            }
        }
    }

    private void testResourceWithMultipleUris() {
        ClientsResource clients = migrationRealm.clients();
        ClientRepresentation clientRepresentation = clients.findByClientId("authz-servlet").get(0);
        ResourceRepresentation resource = clients.get(clientRepresentation.getId()).authorization().resources().findByName("Protected Resource").get(0);
        assertThat(resource.getUris(), containsInAnyOrder("/*"));
    }

    protected void testAuthorizationServices(RealmResource... realms) {
        log.info("testing authorization services");
        for (RealmResource realm : realms) {
            //test setup of authorization services
            for (String roleName : Constants.AUTHZ_DEFAULT_AUTHORIZATION_ROLES) {
                RoleResource role = realm.roles().get(roleName); //throws javax.ws.rs.NotFoundException if not found

                assertFalse("Role shouldn't be composite should be false.", role.toRepresentation().isComposite());

                assertThat("role should be added to default roles for new users", realm.roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.toRepresentation().getRealm().toLowerCase()).getRoleComposites().stream()
                        .map(RoleRepresentation::getName).collect(Collectors.toSet()), hasItem(roleName));
            }
            //test admin roles - master admin client
            List<ClientRepresentation> clients = realm.clients().findByClientId(realm.toRepresentation().getRealm() + "-realm");
            if (!clients.isEmpty()) {
                ClientResource masterAdminClient = realm.clients().get(clients.get(0).getId());
                masterAdminClient.roles().get(AdminRoles.VIEW_AUTHORIZATION).toRepresentation();
                masterAdminClient.roles().get(AdminRoles.MANAGE_AUTHORIZATION).toRepresentation();
                //test admin roles - admin role composite
                Set<String> roleNames = new HashSet<>();
                for (RoleRepresentation role : realm.roles().get(AdminRoles.ADMIN).getRoleComposites()) {
                    roleNames.add(role.getName());
                }
                assertTrue(AdminRoles.VIEW_AUTHORIZATION + " should be composite role of " + AdminRoles.ADMIN, roleNames.contains(AdminRoles.VIEW_AUTHORIZATION));
                assertTrue(AdminRoles.MANAGE_AUTHORIZATION + " should be composite role of " + AdminRoles.ADMIN, roleNames.contains(AdminRoles.MANAGE_AUTHORIZATION));
            }
        }
    }

    protected void testNameOfOTPRequiredAction(RealmResource... realms) {
        log.info("testing OTP Required Action");
        for (RealmResource realm : realms) {
            RequiredActionProviderRepresentation otpAction = realm.flows().getRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP.name());

            assertEquals("The name of CONFIGURE_TOTP required action should be 'Configure OTP'.", "Configure OTP", otpAction.getName());
        }
    }

    protected void testIdentityProviderAuthenticator(RealmResource... realms) {
        log.info("testing identity provider authenticator");
        for (RealmResource realm : realms) {
            boolean success = false;
            for (AuthenticationFlowRepresentation flow : realm.flows().getFlows()) {
                if (flow.getAlias().equals(DefaultAuthenticationFlows.BROWSER_FLOW)) {
                    for (AuthenticationExecutionExportRepresentation execution : flow.getAuthenticationExecutions()) {
                        if ("identity-provider-redirector".equals(execution.getAuthenticator())) {
                            assertEquals("Requirement should be ALTERNATIVE.", AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), execution.getRequirement());
                            assertTrue("Priority should be 25.", execution.getPriority() == 25);
                            success = true;
                        }
                    }
                }
            }
            if (!success) {
                fail("BROWSER_FLOW should contain execution: 'identity-provider-redirector' authenticator.");
            }
        }
    }

    protected void testUpdateProtocolMappers(RealmResource... realms) {
        log.info("testing updated protocol mappers");
        for (RealmResource realm : realms) {
            for (ClientRepresentation client : realm.clients().findAll()) {
                if (client.getProtocolMappers() != null) {
                    for (ProtocolMapperRepresentation protocolMapper : client.getProtocolMappers()) {
                        testUpdateProtocolMapper(protocolMapper, client.getClientId());
                    }
                }
            }
            for (ClientScopeRepresentation clientScope : realm.clientScopes().findAll()) {
                if (clientScope.getProtocolMappers() != null) {
                    for (ProtocolMapperRepresentation protocolMapper : clientScope.getProtocolMappers()) {
                        testUpdateProtocolMapper(protocolMapper, clientScope.getName());
                    }
                }
            }
        }
    }

    protected void testUpdateProtocolMapper(ProtocolMapperRepresentation protocolMapper, String clientId) {
        if (protocolMapper.getConfig().get("id.token.claim") != null) {
            assertEquals("ProtocolMapper's config should contain key 'userinfo.token.claim'. But it doesn't for protocolMapper '"
                    + protocolMapper.getName() + "' of client/clientScope '" + clientId + "'",
                    protocolMapper.getConfig().get("id.token.claim"), protocolMapper.getConfig().get("userinfo.token.claim"));
        }
    }

    protected void testDuplicateEmailSupport(RealmResource... realms) {
        log.info("testing duplicate email");
        for (RealmResource realm : realms) {
            RealmRepresentation rep = realm.toRepresentation();
            assertTrue("LoginWithEmailAllowed should be enabled.", rep.isLoginWithEmailAllowed());
            assertFalse("DuplicateEmailsAllowed should be disabled.", rep.isDuplicateEmailsAllowed());
        }
    }

    protected void testOfflineTokenLogin() throws Exception {
        log.info("test login with old offline token");
        String oldOfflineToken = suiteContext.getMigrationContext().loadOfflineToken();
        Assert.assertNotNull(oldOfflineToken);

        oauth.realm(MIGRATION);
        oauth.client("migration-test-client", "secret");
        AccessTokenResponse response = oauth.doRefreshTokenRequest(oldOfflineToken);

        if (response.getError() != null) {
            String errorMessage = String.format("Error when refreshing offline token. Error: %s, Error details: %s, offline token from previous version: %s",
            response.getError(), response.getErrorDescription(), oldOfflineToken);
            log.error(errorMessage);
            Assert.fail(errorMessage);
        }

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals("offline-test-user", accessToken.getPreferredUsername());

        // KEYCLOAK-10029 - Doublecheck that refresh token in the response is also offline token. Doublecheck that it can be used to another successful refresh
        String newOfflineToken1 = response.getRefreshToken();
        assertOfflineToken(newOfflineToken1);

        response = oauth.doRefreshTokenRequest(newOfflineToken1);
        String newOfflineToken2 = response.getRefreshToken();
        assertOfflineToken(newOfflineToken2);
    }

    private void assertOfflineToken(String offlineToken) {
        RefreshToken offlineTokenParsed = oauth.parseRefreshToken(offlineToken);
        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineTokenParsed.getType());
        assertNull(offlineTokenParsed.getExp());
        assertTrue(TokenUtil.hasScope(offlineTokenParsed.getScope(), OAuth2Constants.OFFLINE_ACCESS));
    }

    private void testRealmDefaultClientScopes(RealmResource realm) {
        log.info("Testing default client scopes created in realm: " + realm.toRepresentation().getRealm());
        ExportImportUtil.testRealmDefaultClientScopes(realm);
    }

    private void testClientDefaultClientScopes(RealmResource realm) {
        log.info("Testing default client scopes transferred from client scope in realm: " + realm.toRepresentation().getRealm());
        ExportImportUtil.testClientDefaultClientScopes(realm);
    }

    private void testPostLogoutRedirectUrisSet(RealmResource realm) {
        log.info("Testing that POST_LOGOUT_REDIRECT_URI is set to '+' for all clients in " + realm.toRepresentation().getRealm());
        ExportImportUtil.testDefaultPostLogoutRedirectUris(realm);
    }

    private void testOfflineScopeAddedToClient() {
        log.infof("Testing offline_access optional scope present in realm %s for client migration-test-client", migrationRealm.toRepresentation().getRealm());

        List<ClientScopeRepresentation> optionalClientScopes = ApiUtil.findClientByClientId(this.migrationRealm, "migration-test-client").getOptionalClientScopes();

        boolean found = optionalClientScopes.stream().filter((ClientScopeRepresentation clientScope) -> {

            return "offline_access".equals(clientScope.getName());

        }).findFirst().isPresent();

        if (!found) {
            Assert.fail("Offline_access not found as optional scope of client migration-test-client");
        }

    }

    private void testRolesAndWebOriginsScopesAddedToClient() {
        log.infof("Testing roles and web-origins default scopes present in realm %s for client migration-test-client", migrationRealm.toRepresentation().getRealm());

        List<ClientScopeRepresentation> defaultClientScopes = ApiUtil.findClientByClientId(this.migrationRealm, "migration-test-client").getDefaultClientScopes();

        Set<String> defaultClientScopeNames = defaultClientScopes.stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toSet());

        if (!defaultClientScopeNames.contains(OIDCLoginProtocolFactory.ROLES_SCOPE)) {
            Assert.fail("Client scope 'roles' not found as default scope of client migration-test-client");
        }
        if (!defaultClientScopeNames.contains(OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE)) {
            Assert.fail("Client scope 'web-origins' not found as default scope of client migration-test-client");
        }

    }

    /**
     * Checks if the {@code microprofile-jwt} optional client scope has been added to the clients.
     */
    private void testMicroprofileJWTScopeAddedToClient() {
        log.infof("Testing microprofile-jwt optional scope present in realm %s for client migration-test-client", migrationRealm.toRepresentation().getRealm());

        List<ClientScopeRepresentation> optionalClientScopes = ApiUtil.findClientByClientId(this.migrationRealm, "migration-test-client").getOptionalClientScopes();

        Set<String> defaultClientScopeNames = optionalClientScopes.stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toSet());

        if (!defaultClientScopeNames.contains(OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE)) {
            Assert.fail("Client scope 'microprofile-jwt' not found as optional scope of client migration-test-client");
        }
    }

    private void testRequiredActionsPriority(RealmResource... realms) {
        log.info("testing required action's priority");
        for (RealmResource realm : realms) {
            log.info("Taking required actions from realm: " + realm.toRepresentation().getRealm());
            List<RequiredActionProviderRepresentation> actions = realm.flows().getRequiredActions();

            // Checking the priority
            int priority = 10;
            for (RequiredActionProviderRepresentation action : actions) {
                if (action.getAlias().equals("update_user_locale")) {
                    assertEquals(1000, action.getPriority());
                } else if (action.getAlias().equals("delete_credential")) {
                    assertEquals(110, action.getPriority());
                } else if (action.getAlias().equals("idp_link")) {
                    assertEquals(120, action.getPriority());
                } else {
                    assertEquals(priority, action.getPriority());
                }

                priority += 10;
            }
        }
    }

    protected void testCredentialsMigratedToNewFormat() {
        log.info("testing user's credentials migrated to new format with secretData and credentialData");

        // Try to login with password+otp after the migration
        try {
            oauth.realm(MIGRATION);
            oauth.client("migration-test-client", "secret");

            TimeBasedOTP otpGenerator = new TimeBasedOTP("HmacSHA1", 8, 40, 1);
            String otp = otpGenerator.generateTOTP("dSdmuHLQhkm54oIm0A0S");

            // Try invalid password first
            AccessTokenResponse response = oauth.passwordGrantRequest("migration-test-user", "password").otp(otp).send();
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());

            // Try invalid OTP then
            response = oauth.passwordGrantRequest("migration-test-user", "password2").otp("invalid").send();
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());

            // Try successful login now
            response = oauth.passwordGrantRequest("migration-test-user", "password2").otp(otp).send();
            Assert.assertNull(response.getError());
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertEquals("migration-test-user", accessToken.getPreferredUsername());
        } catch (Exception e) {
            throw new AssertionError("Failed to login with user 'migration-test-user' after migration", e);
        }
    }

    protected void testOTPAuthenticatorsMigratedToConditionalFlow() {
        log.info("testing optional authentication executions migrated");

        testOTPExecutionMigratedToConditionalFlow("browser", "forms - auth-otp-form - Conditional","OTP Form");
        testOTPExecutionMigratedToConditionalFlow("direct grant", "direct grant - direct-grant-validate-otp - Conditional","OTP");
        testOTPExecutionMigratedToConditionalFlow("reset credentials", "reset credentials - reset-otp - Conditional","Reset OTP");
        testOTPExecutionMigratedToConditionalFlow("first broker login", "Verify Existing Account by Re-authentication - auth-otp-form - Conditional","OTP Form");
    }


    private void testOTPExecutionMigratedToConditionalFlow(String topFlowAlias, String expectedOTPSubflowAlias, String expectedOTPExecutionDisplayName) {
        List<AuthenticationExecutionInfoRepresentation> authExecutions = migrationRealm.flows().getExecutions(topFlowAlias);

        int counter = -1;
        AuthenticationExecutionInfoRepresentation subflowExecution = null;
        for (AuthenticationExecutionInfoRepresentation ex : authExecutions) {
            counter++;
            if (expectedOTPSubflowAlias.equals(ex.getDisplayName())) {
                subflowExecution = ex;
                break;
            }
        }

        if (subflowExecution == null) {
            throw new AssertionError("Not found subflow with displayName '" + expectedOTPSubflowAlias + "' in the flow " + topFlowAlias);
        }

        Assert.assertEquals(AuthenticationExecutionModel.Requirement.CONDITIONAL.toString(), subflowExecution.getRequirement());

        AuthenticationExecutionInfoRepresentation childEx1 = authExecutions.get(counter + 1);
        Assert.assertEquals("Condition - user configured", childEx1.getDisplayName());
        Assert.assertEquals(AuthenticationExecutionModel.Requirement.REQUIRED.toString(), childEx1.getRequirement());
        Assert.assertEquals(0, childEx1.getIndex());
        Assert.assertEquals(subflowExecution.getLevel() + 1, childEx1.getLevel());

        AuthenticationExecutionInfoRepresentation childEx2 = authExecutions.get(counter + 2);
        Assert.assertEquals(expectedOTPExecutionDisplayName, childEx2.getDisplayName());
        Assert.assertEquals(AuthenticationExecutionModel.Requirement.REQUIRED.toString(), childEx2.getRequirement());
        Assert.assertEquals(1, childEx2.getIndex());
        Assert.assertEquals(subflowExecution.getLevel() + 1, childEx2.getLevel());
    }

    protected void testUserLocaleActionAdded(RealmResource realm) {
        RequiredActionProviderRepresentation rep = realm.flows().getRequiredAction("update_user_locale");

        assertNotNull(rep);
        assertEquals("update_user_locale", rep.getAlias());
        assertEquals("update_user_locale", rep.getProviderId());
        assertEquals("Update User Locale", rep.getName());
        assertEquals(1000, rep.getPriority());
        assertTrue(rep.isEnabled());
        assertFalse(rep.isDefaultAction());
    }

    protected void testMigrationTo2_x() throws Exception {
        testMigrationTo2_0_0();
        testMigrationTo2_1_0();
        testMigrationTo2_2_0();
        testMigrationTo2_3_0();
        testMigrationTo2_5_0();
    }

    protected void testMigrationTo3_x() {
        // NOTE:
        testMigrationTo3_0_0();
        testMigrationTo3_2_0();
        testMigrationTo3_4_0();
        testMigrationTo3_4_1();
        testMigrationTo3_4_2();
    }

    protected void testMigrationTo4_x(boolean supportsAuthzServices, boolean checkMigrationData) {
        testMigrationTo4_0_0();
        testMigrationTo4_2_0(supportsAuthzServices);
        testMigrationTo4_6_0(supportsAuthzServices, checkMigrationData);
    }

    protected void testMigrationTo4_x() {
        testMigrationTo4_x(true, true);
    }

    protected void testMigrationTo5_x() {
        // so far nothing
    }

    protected void testMigrationTo6_x() {
        testMigrationTo6_0_0();
    }

    protected void testMigrationTo8_x() {
        testMigrationTo8_0_0();
    }
    protected void testMigrationTo9_x() {
        testMigrationTo9_0_0();
    }

    // Realm attributes supported since Keycloak 3
    protected void testMigrationTo12_x(boolean testRealmAttributesMigration) {
        testMigrationTo12_0_0();
        testMigrationTo13_0_0(testRealmAttributesMigration);
        testMigrationTo14_0_0();
    }

    protected void testMigrationTo18_x() {
        testMigrationTo18_0_0();
    }

    protected void testMigrationTo19_x() {
        testMigrationTo19_0_0();
    }

    protected void testMigrationTo20_x() {
        testMigrationTo20_0_0();
    }

    protected void testMigrationTo21_x() {
        testMigrationTo21_0_2();
    }

    protected void testMigrationTo22_x() {
        testMigrationTo22_0_0();
    }

    protected void testMigrationTo23_x(boolean testUserProfileMigration) {
        testMigrationTo23_0_0(testUserProfileMigration);
    }

    protected void testMigrationTo24_x(boolean testUserProfileMigration) {
        testMigrationTo24_0_0(testUserProfileMigration, false);
    }

    protected void testMigrationTo24_x(boolean testUserProfileMigration, boolean testLdapUseTruststoreSpiMigration) {
        testMigrationTo24_0_0(testUserProfileMigration, testLdapUseTruststoreSpiMigration);
    }

    protected void testMigrationTo25_x() {
        testMigrationTo25_0_0();
    }

    protected void testMigrationTo7_x(boolean supportedAuthzServices) {
        if (supportedAuthzServices) {
            testDecisionStrategySetOnResourceServer();
        }
    }

    protected void testResourceTag() {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            URI url = suiteContext.getAuthServerInfo().getUriBuilder().path("/auth").build();
            String response = SimpleHttpDefault.doGet(url.toString(), client).asString();
            Matcher m = Pattern.compile("resources/([^/]*)/common").matcher(response);
            assertTrue(m.find());
            assertTrue(m.group(1).matches("[a-zA-Z0-9_\\-.~]{5}"));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    protected void testAlwaysDisplayInConsole() {
        for(ClientRepresentation clientRep : masterRealm.clients().findAll()) {
            Assert.assertFalse(clientRep.isAlwaysDisplayInConsole());
        }
    }

    protected void testDefaultRoles(RealmResource realm) {
        String realmName = realm.toRepresentation().getRealm().toLowerCase();
        assertThat(realm.roles().get("default-roles-" + realmName).getRoleComposites().stream()
                .map(RoleRepresentation::getName).collect(Collectors.toSet()),
            allOf(
                hasItem(realmName + "-test-realm-role"),
                hasItem(realmName + "-test-client-role"))
            );
    }

    protected void testDefaultRolesNameWhenTaken() {
        // 'default-roles-migration2' name is used, we test that 'default-roles-migration2-1' is created instead
        assertThat(migrationRealm2.toRepresentation().getDefaultRole().getName(), equalTo("default-roles-migration2-1"));
    }

    protected void testSamlAttributes(RealmResource realm) {
        log.info("Testing SAML ARTIFACT BINDING IDENTIFIER");

        realm.clients().findAll().stream()
                .filter(clientRepresentation -> Objects.equals("saml", clientRepresentation.getProtocol()))
                .forEach(clientRepresentation -> {
                    String clientId = clientRepresentation.getClientId();
                    assertThat(clientRepresentation.getAttributes(), hasEntry(SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER, ArtifactBindingUtils.computeArtifactBindingIdentifierString(clientId)));
                });
    }

    protected void testExtremelyLongClientAttribute(RealmResource realm) {
        log.info("Testing SAML certfificates attribute");

        realm.clients().findByClientId("migration-saml-client")
          .forEach(clientRepresentation -> {
                assertThat(clientRepresentation.getAttributes(), hasEntry("extremely_long_attribute",
                      "     00000     00010     00020     00030     00040     00050     00060     00070     00080     00090"
                    + "     00100     00110     00120     00130     00140     00150     00160     00170     00180     00190"
                    + "     00200     00210     00220     00230     00240     00250     00260     00270     00280     00290"
                    + "     00300     00310     00320     00330     00340     00350     00360     00370     00380     00390"
                    + "     00400     00410     00420     00430     00440     00450     00460     00470     00480     00490"
                    + "     00500     00510     00520     00530     00540     00550     00560     00570     00580     00590"
                    + "     00600     00610     00620     00630     00640     00650     00660     00670     00680     00690"
                    + "     00700     00710     00720     00730     00740     00750     00760     00770     00780     00790"
                    + "     00800     00810     00820     00830     00840     00850     00860     00870     00880     00890"
                    + "     00900     00910     00920     00930     00940     00950     00960     00970     00980     00990"
                    + "     01000     01010     01020     01030     01040     01050     01060     01070     01080     01090"
                    + "     01100     01110     01120     01130     01140     01150     01160     01170     01180     01190"
                    + "     01200     01210     01220     01230     01240     01250     01260     01270     01280     01290"
                    + "     01300     01310     01320     01330     01340     01350     01360     01370     01380     01390"
                    + "     01400     01410     01420     01430     01440     01450     01460     01470     01480     01490"
                    + "     01500     01510     01520     01530     01540     01550     01560     01570     01580     01590"
                    + "     01600     01610     01620     01630     01640     01650     01660     01670     01680     01690"
                    + "     01700     01710     01720     01730     01740     01750     01760     01770     01780     01790"
                    + "     01800     01810     01820     01830     01840     01850     01860     01870     01880     01890"
                    + "     01900     01910     01920     01930     01940     01950     01960     01970     01980     01990"
                    + "     02000     02010     02020     02030     02040     02050     02060     02070     02080     02090"
                    + "     02100     02110     02120     02130     02140     02150     02160     02170     02180     02190"
                    + "     02200     02210     02220     02230     02240     02250     02260     02270     02280     02290"
                    + "     02300     02310     02320     02330     02340     02350     02360     02370     02380     02390"
                    + "     02400     02410     02420     02430     02440     02450     02460     02470     02480     02490"
                    + "     02500     02510     02520     02530     02540     02550     02560     02570     02580     02590"
                    + "     02600     02610     02620     02630     02640     02650     02660     02670     02680     02690"
                    + "     02700     02710     02720     02730     02740     02750     02760     02770     02780     02790"
                    + "     02800     02810     02820     02830     02840     02850     02860     02870     02880     02890"
                    + "     02900     02910     02920     02930     02940     02950     02960     02970     02980     02990"
                    + "     03000     03010     03020     03030     03040     03050     03060     03070     03080     03090"
                    + "     03100     03110     03120     03130     03140     03150     03160     03170     03180     03190"
                    + "     03200     03210     03220     03230     03240     03250     03260     03270     03280     03290"
                    + "     03300     03310     03320     03330     03340     03350     03360     03370     03380     03390"
                    + "     03400     03410     03420     03430     03440     03450     03460     03470     03480     03490"
                    + "     03500     03510     03520     03530     03540     03550     03560     03570     03580     03590"
                    + "     03600     03610     03620     03630     03640     03650     03660     03670     03680     03690"
                    + "     03700     03710     03720     03730     03740     03750     03760     03770     03780     03790"
                    + "     03800     03810     03820     03830     03840     03850     03860     03870     03880     03890"
                    + "     03900     03910     03920     03930     03940     03950     03960     03970     03980"));
          });
    }

    protected void testRealmAttributesMigration() {
        log.info("testing realm attributes migration");
        Map<String, String> realmAttributes = migrationRealm.toRepresentation().getAttributes();
        assertEquals("custom_value", realmAttributes.get("custom_attribute"));
    }

    private void testUserProfileEnabledByDefault(RealmResource realm) {
        RealmRepresentation rep = realm.toRepresentation();
        Map<String, String> attributes = rep.getAttributes();
        String userProfileEnabled = attributes.get(REALM_USER_PROFILE_ENABLED);
        assertNull(userProfileEnabled);
    }

    private void testUnmanagedAttributePolicySet(RealmResource realm, UnmanagedAttributePolicy policy) {
        UPConfig upConfig = realm.users().userProfile().getConfiguration();
        assertEquals(policy, upConfig.getUnmanagedAttributePolicy());
    }

    /**
     * Checks if the {@code useTruststoreSpi} flag in the LDAP federation provider present in realm {@code Migration2}
     * was properly migrated from the old value {@code ldapsOnly} to {@code always}.
     * </p>
     * This provider was added to the file migration-realm-19.0.3.json as a disabled provider, so it doesn't get involved
     * in actual user searches and is there just to test the migration of the {@code useTruststoreSpi} config attribute.
     *
     * @param realm the migrated realm resource.
     */
    private void testLdapUseTruststoreSpiMigration(final RealmResource realm) {
        RealmRepresentation rep = realm.toRepresentation();
        List<ComponentRepresentation> componentsRep = realm.components().query(rep.getId(), UserStorageProvider.class.getName());
        assertThat(componentsRep.size(), equalTo(1));
        MultivaluedHashMap<String, String> config = componentsRep.get(0).getConfig();
        assertNotNull(config);
        assertThat(config.getFirst(LDAPConstants.USE_TRUSTSTORE_SPI), equalTo(LDAPConstants.USE_TRUSTSTORE_ALWAYS));
    }

    private void testHS512KeyCreated(RealmResource realm) {
        List<ComponentRepresentation> keyProviders = realm.components().query(realm.toRepresentation().getId(), KeyProvider.class.getName());
        Assert.assertTrue("Old HS256 key provider does not exists",
                keyProviders.stream().anyMatch(c -> "hmac-generated".equals(c.getProviderId())
                        && c.getConfig().getFirst("algorithm").equals(Algorithm.HS256)));
        Assert.assertTrue("New HS512 key provider does not exists",
                keyProviders.stream().anyMatch(c -> "hmac-generated".equals(c.getProviderId())
                        && c.getConfig().getFirst("algorithm").equals(Algorithm.HS512)));
        KeysMetadataRepresentation keysMetadata = realm.keys().getKeyMetadata();
        Assert.assertNotNull("Old HS256 key does not exist", keysMetadata.getActive().get(Algorithm.HS256));
        Assert.assertNotNull("New HS256 key does not exist", keysMetadata.getActive().get(Algorithm.HS512));
    }

    private void testClientAttributes(RealmResource realm) {
        List<ClientRepresentation> clients = realm.clients().findByClientId("migration-saml-client");
        Assert.assertEquals(1, clients.size());
        ClientRepresentation client = clients.get(0);
        Assert.assertNotNull(client.getAttributes().get("saml.artifact.binding.identifier"));
        Assert.assertNotNull(client.getAttributes().get("saml_idp_initiated_sso_url_name"));
        List<String> clientIds = realm.clients().query("saml.artifact.binding.identifier:\"" + client.getAttributes().get("saml.artifact.binding.identifier") + "\"")
                .stream().map(ClientRepresentation::getClientId)
                .collect(Collectors.toList());
        Assert.assertEquals(Collections.singletonList(client.getClientId()), clientIds);
        clientIds = realm.clients().query("saml_idp_initiated_sso_url_name:\"" + client.getAttributes().get("saml_idp_initiated_sso_url_name") + "\"")
                .stream().map(ClientRepresentation::getClientId)
                .collect(Collectors.toList());
        Assert.assertEquals(Collections.singletonList(client.getClientId()), clientIds);
    }

    private void testDeleteCredentialActionAvailable(RealmResource realm) {
        RequiredActionProviderRepresentation rep = realm.flows().getRequiredAction("delete_credential");
        assertNotNull(rep);
        assertEquals("delete_credential", rep.getAlias());
        assertEquals("delete_credential", rep.getProviderId());
        assertEquals("Delete Credential", rep.getName());
        assertEquals(110, rep.getPriority());
        assertTrue(rep.isEnabled());
        assertFalse(rep.isDefaultAction());
    }

    private void testIdpLinkActionAvailable(RealmResource realm) {
        RequiredActionProviderRepresentation rep = realm.flows().getRequiredAction("idp_link");
        assertNotNull(rep);
        assertEquals("idp_link", rep.getAlias());
        assertEquals("idp_link", rep.getProviderId());
        assertEquals("Linking Identity Provider", rep.getName());
        assertEquals(120, rep.getPriority());
        assertTrue(rep.isEnabled());
        assertFalse(rep.isDefaultAction());
    }

    private void testIdentityProviderConfigMigration(final RealmResource realm) {
        IdentityProviderRepresentation rep = realm.identityProviders().get("gitlab").toRepresentation();
        // gitlab identity provider should have it's hideOnLoginPage attribute migrated from the config to the provider itself.
        assertThat(rep.isHideOnLogin(), is(true));
        assertThat(rep.getConfig().containsKey(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR), is(false));
    }

    private void testLightweightClientAndFullScopeAllowed(RealmResource realm, String clientId) {
        ClientRepresentation clientRepresentation = realm.clients().findByClientId(clientId).get(0);
        assertTrue(clientRepresentation.isFullScopeAllowed());
        assertTrue(Boolean.parseBoolean(clientRepresentation.getAttributes().get(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED)));
    }

    private void testSamlEncryptionAttributes(RealmResource realm) {
        // check all the saml clients have the encryption attributes
        List<ClientRepresentation> samlClients = realm.clients().findAll().stream()
                .filter(client -> SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol()))
                .filter(client -> "true".equals(client.getAttributes().get(SamlConfigAttributes.SAML_ENCRYPT)))
                .collect(Collectors.toList());
        assertThat(samlClients.size(), is(1));
        for (ClientRepresentation client : samlClients) {
            assertThat(client.getAttributes().get(SamlConfigAttributes.SAML_ENCRYPTION_ALGORITHM), is(XMLCipher.AES_128));
            assertThat(client.getAttributes().get(SamlConfigAttributes.SAML_ENCRYPTION_KEY_ALGORITHM), is(XMLCipher.RSA_OAEP));
            assertThat(client.getAttributes().get(SamlConfigAttributes.SAML_ENCRYPTION_DIGEST_METHOD), is(XMLCipher.SHA1));
            assertThat(client.getAttributes().get(SamlConfigAttributes.SAML_ENCRYPTION_MASK_GENERATION_FUNTION), nullValue());
        }
    }
}
