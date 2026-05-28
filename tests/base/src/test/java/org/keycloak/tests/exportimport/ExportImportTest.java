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

package org.keycloak.tests.exportimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.JsonTestUtils;
import org.keycloak.testsuite.util.runonserver.ExportImportHelper;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class ExportImportTest {

    @InjectRealm(ref = "test", fromJson = "/testrealm.json", config = ExportImportRealmConfig.class)
    ManagedRealm managedTestRealm2;

    @InjectRealm(ref="master", attachTo="master")
    ManagedRealm managedMasterRealm;

    @InjectRunOnServer(realmRef="master")
    RunOnServerClient runOnServerMaster;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    private static final String TEST_REALM = "test-realm";

    @BeforeEach
    public void ensureTestRealmExists() {
        if (!isRealmPresent(TEST_REALM)) {
            RealmRepresentation testRealm = loadJson(
                    getClass().getResourceAsStream("/model/testrealm.json"),
                    RealmRepresentation.class
            );
            testRealm.setId(TEST_REALM);
            testRealm.setRealm(TEST_REALM);
            adminClient.realms().create(testRealm);
        }
    }

    public void removeAllRealmsExceptMasterTestAndTestRealm() {
        List<RealmRepresentation> realms = adminClient.realms().findAll();
        for (RealmRepresentation realm : realms) {
            String realmName = realm.getRealm();
            if (!realmName.equals("master") && !realmName.equals("test") && !realmName.equals(TEST_REALM)) {
                removeRealm(realmName);
            }
        }
    }

    private static void setEventsConfig(RealmRepresentation realm) {
        realm.setEventsEnabled(true);
        realm.setAdminEventsEnabled(true);
        realm.setAdminEventsDetailsEnabled(true);
        realm.setEventsExpiration(600);
        realm.setEnabledEventTypes(Arrays.asList("REGISTER", "REGISTER_ERROR", "LOGIN", "LOGIN_ERROR", "LOGOUT_ERROR"));
    }

    private static void checkEventsConfig(RealmEventsConfigRepresentation config) {
        Assertions.assertTrue(config.isEventsEnabled());
        Assertions.assertTrue(config.isAdminEventsEnabled());
        Assertions.assertTrue(config.isAdminEventsDetailsEnabled());
        Assertions.assertEquals((Long) 600L, config.getEventsExpiration());
        Assertions.assertTrue(config.getEnabledEventTypes().containsAll(
                List.of("REGISTER", "REGISTER_ERROR", "LOGIN", "LOGIN_ERROR", "LOGOUT_ERROR")
        ));
    }

    private static UserRepresentation makeUser(String userName) {
        return UserBuilder.create()
                .username(userName)
                .email(userName + "@test.com")
                .password("password")
                .build();
    }

    private static void setLocalizationTexts(RealmRepresentation realm) {
        Map<String, Map<String, String>> localizationTexts = new HashMap<>();
        Map<String, String> enMap = new HashMap<>();
        enMap.put("key1", "value1");
        enMap.put("key2", "value2");
        localizationTexts.put("en", enMap);
        realm.setLocalizationTexts(localizationTexts);
    }

    @AfterEach
    public void clearExportImportProps() {
        clearExportImportProperties();
    }

    @Test
    public void testDirFullExportImport() throws Throwable {
        removeAllRealmsExceptMasterTestAndTestRealm();
        runOnServerMaster.run(ExportImportHelper.setProvider(DirExportProviderFactory.PROVIDER_ID));
        String targetDirPath = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","")+ File.separator + "dirExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        runOnServerMaster.run(ExportImportHelper.setDir(targetDirPath));
        runOnServerMaster.run(ExportImportHelper.setUsersPerFile(ExportImportConfig.DEFAULT_USERS_PER_FILE));

        testFullExportImport();

        RealmResource testRealmRealm = adminClient.realm(TEST_REALM);
        ExportImportUtil.assertDataImportedInRealm(adminClient, runOnServerMaster, testRealmRealm.toRepresentation());

        // There should be 6 files in target directory (3 realm, 3 user)
        assertEquals(6, new File(targetDirPath).listFiles().length);
    }

    @Test
    public void testDirRealmExportImport() throws Throwable {
        runOnServerMaster.run(ExportImportHelper.setProvider(DirExportProviderFactory.PROVIDER_ID));
        String targetDirPath = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "dirRealmExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        runOnServerMaster.run(ExportImportHelper.setDir(targetDirPath));
        runOnServerMaster.run(ExportImportHelper.setUsersPerFile(5));

        testRealmExportImport();

        RealmResource testRealmRealm = adminClient.realm(TEST_REALM);
        ExportImportUtil.assertDataImportedInRealm(adminClient, runOnServerMaster, testRealmRealm.toRepresentation());

        // There should be 5 files in target directory (1 realm, 16 users, 5 users per file)
        // (+ additional user service-account-test-app-authz that should not be there ???)
        File[] files = new File(targetDirPath).listFiles();
        assertEquals(5, files.length);
    }

    @Test
    public void testSingleFileFullExportImport() throws Throwable {
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        String targetFilePath = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "singleFile-full.json";
        runOnServerMaster.run(ExportImportHelper.setFile(targetFilePath));

        testFullExportImport();
        assertExportContainsGoogleClientSecret(targetFilePath);
    }

    private static void assertExportContainsGoogleClientSecret(String targetFilePath) throws IOException {
        assertTrue(new File(targetFilePath).exists(), "Expected an export file to exist");

        Map<String, RealmRepresentation> realms;
        try (FileInputStream fis = new FileInputStream(targetFilePath)) {
            realms = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, fis);
        }
        List<IdentityProviderRepresentation> idps = realms.get("test-realm").getIdentityProviders();
        IdentityProviderRepresentation googleIdp = idps.stream().filter(idp -> idp.getAlias().equals("google1")).findFirst().get();
        assertNotNull(googleIdp);
        assertEquals("googleSecret", googleIdp.getConfig().get("clientSecret"));
    }

    @Test
    public void testSingleFileRealmExportImport() {
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        String targetFilePath = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "singleFile-realm.json";
        runOnServerMaster.run(ExportImportHelper.setFile(targetFilePath));

        testRealmExportImport();
    }

    @Test
    public void testSingleFileRealmWithoutBuiltinsImport() throws Throwable {
        // Remove test realm
        removeRealm(TEST_REALM);

        // Set the realm, which doesn't have builtin clients/roles inside JSON
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        URL url = ExportImportTest.class.getResource("/model/testrealm.json");
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        runOnServerMaster.run(ExportImportHelper.setFile(targetFilePath));

        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));

        runOnServerMaster.run(ExportImportHelper.runImport());

        RealmResource testRealmRealm = adminClient.realm(TEST_REALM);

        ExportImportUtil.assertDataImportedInRealm(adminClient, runOnServerMaster, testRealmRealm.toRepresentation());
        removeRealm(TEST_REALM);
    }

    @Test
    public void testImportFromPartialExport() {
        // import a realm with clients without roles
        importRealmFromFile("/import/partial-import.json");
        Assertions.assertTrue(isRealmPresent("partial-import"), "Imported realm hasn't been found!");

        // import a realm with clients without roles
        importRealmFromFile("/import/import-without-roles.json");
        Assertions.assertTrue(isRealmPresent("import-without-roles"), "Imported realm hasn't been found!");

        // import a realm with roles without clients
        importRealmFromFile("/import/import-without-clients.json");
        Assertions.assertTrue(isRealmPresent("import-without-clients"), "Imported realm hasn't been found!");

        // Cleanup - remove imported test realms to prevent affecting other tests
        removeRealm("partial-import");
        removeRealm("import-without-roles");
        removeRealm("import-without-clients");

    }

    @Test
    public void testImportFromRealmWithPartialAuthenticationFlows() {
        // import a realm with no built-in authentication flows
        importRealmFromFile("/import/partial-authentication-flows-import.json");
        Assertions.assertTrue(isRealmPresent("partial-authentication-flows-import"), "Imported realm hasn't been found!");
    }

    @Test
    public void testImportWithNullAuthenticatorConfigAndNoDefaultBrowserFlow() {
        importRealmFromFile("/import/testrealm-authenticator-config-null.json");
        Assertions.assertTrue(isRealmPresent("cez"), "Imported realm hasn't been found!");
    }

    @Test
    public void testExportUserProfileConfig() throws IOException {
        RealmResource realmRes = adminClient.realm(TEST_REALM);

        //add some non-default config
        UPConfig persistedConfig = UserProfileUtil.setUserProfileConfiguration(realmRes, UserProfileUtil.CONFIGURATION_FOR_USER_EDIT);

        //export
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));
        runOnServerMaster.run(ExportImportHelper.setRealmName(TEST_REALM));
        String targetFilePath = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "singleFile-userProfile.json";
        runOnServerMaster.run(ExportImportHelper.setFile(targetFilePath));
        runOnServerMaster.run(ExportImportHelper.runExport());

        //remove realm
        removeRealm(TEST_REALM);

        //import
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));
        runOnServerMaster.run(ExportImportHelper.runImport());
        realmRes = adminClient.realm(TEST_REALM);

        List<ComponentRepresentation> userProfileComponents = realmRes.components().query(TEST_REALM, "org.keycloak.userprofile.UserProfileProvider");
        assertThat(userProfileComponents, notNullValue());
        assertThat(userProfileComponents, hasSize(1));
        MultivaluedHashMap<String, String> config = userProfileComponents.get(0).getConfig();
        assertThat(config, notNullValue());
        assertThat(config.size(), equalTo(1));
        JsonTestUtils.assertJsonEquals(config.getFirst(DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY), JsonSerialization.writeValueAsString(persistedConfig), UPConfig.class);
    }

    @Test
    public void testImportIgnoreExistingMissingClientId() {
        runOnServerMaster.run(ExportImportHelper.setStrategy(Strategy.IGNORE_EXISTING));
        runOnServerMaster.run(ExportImportHelper.setProvider(DirExportProviderFactory.PROVIDER_ID));

        String targetDirPath = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "dirRealmExport";
        File dest = new File(targetDirPath);
        try {
            DirExportProvider.recursiveDeleteDir(dest);
            runOnServerMaster.run(ExportImportHelper.setDir(targetDirPath));

            runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));

            URL url = ExportImportTest.class.getResource("/model/testrealm.json");
            File testRealm = new File(url.getFile());
            assertThat(testRealm, Matchers.notNullValue());

            File newFile = new File("target", "test-realm-realm.json");

            try {
                FileUtils.copyFile(testRealm, newFile);
                FileUtils.copyFileToDirectory(newFile, dest);
            } catch (IOException e) {
                Assertions.fail("Cannot copy file. Details: " + e.getMessage());
            }

            File existingFile = FileUtils.getFile(dest, newFile.getName());
            assertThat(existingFile, Matchers.notNullValue());

            runOnServerMaster.run(ExportImportHelper.runExport());
            runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));

            try {
                runOnServerMaster.run(ExportImportHelper.runImport());
                runOnServerMaster.run(ExportImportHelper.runImport());
            } catch (Exception e) {
                Assertions.fail("Error with realm importing twice. Details: " + e.getMessage());
            }
        } finally {
            DirExportProvider.recursiveDeleteDir(dest);
        }
    }

    // TODO: This test is disabled because it causes uncaught server errors
    // The test verifies that import works when filename doesn't match realm name
    // See: @UncaughtServerErrorExpected in original Arquillian test
    // Re-enable when the server-side bug is fixed or test framework supports expected errors
    // @Test
    public void testImportNameMismatch() {
        runOnServerMaster.run(ExportImportHelper.setStrategy(Strategy.IGNORE_EXISTING));
        runOnServerMaster.run(ExportImportHelper.setProvider(DirExportProviderFactory.PROVIDER_ID));

        String targetDirPath = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "dirRealmExport";
        File dest = new File(targetDirPath);
        try {
            DirExportProvider.recursiveDeleteDir(dest);
            runOnServerMaster.run(ExportImportHelper.setDir(targetDirPath));

            runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));

            URL url = ExportImportTest.class.getResource("/model/testrealm.json");
            File testRealm = new File(url.getFile());
            assertThat(testRealm, Matchers.notNullValue());

            File newFile = new File("target", "test-new-realm.json");

            try {
                FileUtils.copyFile(testRealm, newFile);
                FileUtils.copyFileToDirectory(newFile, dest);
            } catch (IOException e) {
                Assertions.fail("Cannot copy file. Details: " + e.getMessage());
            }

            File existingFile = FileUtils.getFile(dest, newFile.getName());
            assertThat(existingFile, Matchers.notNullValue());

            runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));

            runOnServerMaster.run(ExportImportHelper.runImport());
        } finally {
            DirExportProvider.recursiveDeleteDir(dest);
        }
    }

    private boolean isRealmPresent(String realmName) {
        return adminClient.realms().findAll().stream().anyMatch(realm -> realmName.equals(realm.getRealm()));
    }

    private void testFullExportImport() {
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));
        runOnServerMaster.run(ExportImportHelper.setRealmName(""));

        runOnServerMaster.run(ExportImportHelper.runExport());

        removeRealm("test");
        removeRealm(TEST_REALM);
        assertRealmNames(adminClient.realms().findAll(), "master");

        Map<String, RequiredActionProviderRepresentation> requiredActionsBeforeImport = new HashMap<>();
        adminClient.realm("master").flows().getRequiredActions().stream()
                .forEach(action -> {
                    requiredActionsBeforeImport.put(action.getAlias(), action);
                });

        assertNotAuthenticated("test-user@localhost", "password");
        assertNotAuthenticated("user1", "password");
        assertNotAuthenticated("user2", "password");
        assertNotAuthenticated("user3", "password");
        assertNotAuthenticated("user-requiredOTP", "password");
        assertNotAuthenticated("user-requiredWebAuthn", "password");

        // Configure import
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));

        runOnServerMaster.run(ExportImportHelper.runImport());

        // Ensure data are imported back
        assertRealmNames(adminClient.realms().findAll(), "master", "test", TEST_REALM);

        assertAuthenticated("test-user@localhost", "password");
        assertAuthenticated("user1", "password");
        assertAuthenticated("user2", "password");
        assertAuthenticated("user3", "password");
        assertAuthenticated("user-requiredOTP", "password");
        assertAuthenticated("user-requiredWebAuthn", "password");

        RealmResource testRealmRealm = adminClient.realm("test");
        assertTrue(testRealmRealm.users().search("user-requiredOTP").get(0)
                .getRequiredActions().get(0).equals(UserModel.RequiredAction.CONFIGURE_TOTP.name()));
        assertTrue(testRealmRealm.users().search("user-requiredWebAuthn").get(0)
                .getRequiredActions().get(0).equals(WebAuthnRegisterFactory.PROVIDER_ID));

        // KEYCLOAK-6050 Check SMTP password is exported/imported
        String smtpPassword = runOnServerMaster.fetchString(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            return realm.getSmtpConfig().get("password");
        });
        assertEquals("secret", smtpPassword);

        // KEYCLOAK-8176 Check required actions are exported/imported properly
        List<RequiredActionProviderRepresentation> requiredActionsAfterImport = adminClient.realm("master").flows().getRequiredActions();
        assertThat(requiredActionsAfterImport.size(), is(equalTo(requiredActionsBeforeImport.size())));
        requiredActionsAfterImport.stream()
                .forEach((action) -> {
                    RequiredActionProviderRepresentation beforeImportAction = requiredActionsBeforeImport.get(action.getAlias());
                    assertThat(action.getName(), is(equalTo(beforeImportAction.getName())));
                    assertThat(action.getProviderId(), is(equalTo(beforeImportAction.getProviderId())));
                    assertThat(action.getPriority(), is(equalTo(beforeImportAction.getPriority())));
                });
    }

    private void testRealmExportImport() {
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));
        runOnServerMaster.run(ExportImportHelper.setRealmName("test"));

        String[] authFlowObjectIdsBeforeImport = getSomeAuthenticationFlowsObjectIds();

        runOnServerMaster.run(ExportImportHelper.runExport());

        List<ComponentRepresentation> components = adminClient.realm("test").components().query();
        KeysMetadataRepresentation keyMetadata = adminClient.realm("test").keys().getKeyMetadata();
        String sampleRealmRoleId = adminClient.realm("test").roles().get("sample-realm-role").toRepresentation().getId();
        Map<String, List<String>> roleAttributes = adminClient.realm("test").roles().get("attribute-role").toRepresentation().getAttributes();
        String testAppId = adminClient.realm("test").clients().findByClientId("test-app").get(0).getId();
        String sampleClientRoleId = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getId();
        String sampleClientRoleAttribute = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getAttributes().get("sample-client-role-attribute").get(0);

        // Delete some realm (and some data in admin realm)
        adminClient.realm("test").remove();

        assertRealmNames(adminClient.realms().findAll(), TEST_REALM, "master");

        assertNotAuthenticated("test-user@localhost", "password");
        assertNotAuthenticated("user1", "password");
        assertNotAuthenticated("user2", "password");
        assertNotAuthenticated("user3", "password");
        assertNotAuthenticated("user-requiredOTP", "password");
        assertNotAuthenticated("user-requiredWebAuthn", "password");

        // Configure import
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));

        runOnServerMaster.run(ExportImportHelper.runImport());

        // Ensure data are imported back, but just for "test" realm
        assertRealmNames(adminClient.realms().findAll(), "master", "test", TEST_REALM);

        assertAuthenticated("test-user@localhost", "password");
        assertAuthenticated("user1", "password");
        assertAuthenticated("user2", "password");
        assertAuthenticated("user3", "password");
        assertAuthenticated("user-requiredOTP", "password");
        assertAuthenticated("user-requiredWebAuthn", "password");

        RealmResource testRealmRealm = adminClient.realm("test");
        assertTrue(testRealmRealm.users().search("user-requiredOTP").get(0)
                .getRequiredActions().get(0).equals(UserModel.RequiredAction.CONFIGURE_TOTP.name()));
        assertTrue(testRealmRealm.users().search("user-requiredWebAuthn").get(0)
                .getRequiredActions().get(0).equals(WebAuthnRegisterFactory.PROVIDER_ID));

        String[] authFlowObjectIdsAfterImport = getSomeAuthenticationFlowsObjectIds();
        // Test that IDs of authentication-flows (both top level and nested) and authenticationConfiguration was preserved
        Assertions.assertArrayEquals(authFlowObjectIdsBeforeImport, authFlowObjectIdsAfterImport);

        List<ComponentRepresentation> componentsImported = adminClient.realm("test").components().query();
        assertComponents(components, componentsImported);

        KeysMetadataRepresentation keyMetadataImported = adminClient.realm("test").keys().getKeyMetadata();
        assertEquals(keyMetadata.getActive(), keyMetadataImported.getActive());

        String importedSampleRealmRoleId = adminClient.realm("test").roles().get("sample-realm-role").toRepresentation().getId();
        assertEquals(sampleRealmRoleId, importedSampleRealmRoleId);

        Map<String, List<String>> importedRoleAttributes = adminClient.realm("test").roles().get("attribute-role").toRepresentation().getAttributes();
        Assert.assertRoleAttributes(roleAttributes, importedRoleAttributes);

        String importedSampleClientRoleId = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getId();
        assertEquals(sampleClientRoleId, importedSampleClientRoleId);

        String importedSampleClientRoleAttribute = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getAttributes().get("sample-client-role-attribute").get(0);
        assertEquals(sampleClientRoleAttribute, importedSampleClientRoleAttribute);

        checkEventsConfig(adminClient.realm("test").getRealmEventsConfig());
    }

    private void assertAuthenticated(String username, String password) {
        assertAuth(true, username, password);
    }

    private void assertNotAuthenticated(String username, String password) {
        assertAuth(false, username, password);
    }

    private void assertAuth(boolean expectedResult, String username, String password) {
        assertEquals(expectedResult, runOnServerMaster.fetch(validCredentials(username, password)));
    }

    private void assertComponents(List<ComponentRepresentation> expected, List<ComponentRepresentation> actual) {
        expected.sort((o1, o2) -> o1.getId().compareTo(o2.getId()));
        actual.sort((o1, o2) -> o1.getId().compareTo(o2.getId()));

        assertEquals(expected.size(), actual.size());
        for (int i = 0 ; i < expected.size(); i++) {
            ComponentRepresentation e = expected.get(i);
            ComponentRepresentation a = actual.get(i);

            assertEquals(e.getId(), a.getId());
            assertEquals(e.getName(), a.getName());
            assertEquals(e.getProviderId(), a.getProviderId());
            assertEquals(e.getProviderType(), a.getProviderType());
            assertEquals(e.getParentId(), a.getParentId());
            assertEquals(e.getSubType(), a.getSubType());
            Assert.assertNames(e.getConfig().keySet(), a.getConfig().keySet().toArray(new String[] {}));

            // Compare config values without take order into account
            for (Map.Entry<String, List<String>> entry : e.getConfig().entrySet()) {
                List<String> eList = entry.getValue();
                List<String> aList = a.getConfig().getList(entry.getKey());
                Assert.assertNames(eList, aList.toArray(new String[] {}));
            }
        }
    }

    // Get IDs of some objects (top authentication flow, nested authentication flow, authentication config) to be able to test if IDs are same after re-import
    private String[] getSomeAuthenticationFlowsObjectIds() {
        String firstBrokerLoginFlowID = adminClient.realm("test").flows().getFlows().stream()
                .filter(flow -> "first broker login".equals(flow.getAlias()))
                .findFirst().get().getId();

        List<AuthenticationExecutionInfoRepresentation> authExecutions = adminClient.realm("test").flows().getExecutions("User creation or linking");
        Assertions.assertEquals("idp-create-user-if-unique", authExecutions.get(0).getProviderId());

        String authConfigId = authExecutions.get(0).getAuthenticationConfig();
        Assertions.assertEquals("create unique user config", adminClient.realm("test").flows().getAuthenticatorConfig(authConfigId).getAlias());

        String handleExistingAccountSubflowId = authExecutions.get(1).getFlowId();
        Assertions.assertEquals("Handle Existing Account", adminClient.realm("test").flows().getFlow(handleExistingAccountSubflowId).getAlias());

        return new String[] {firstBrokerLoginFlowID, handleExistingAccountSubflowId, authConfigId };
    }

    private void clearExportImportProperties() {
        // Clear export/import properties after test
        Properties systemProps = System.getProperties();
        Set<String> propsToRemove = new HashSet<>();

        for (Object key : systemProps.keySet()) {
            if (key.toString().startsWith(ExportImportConfig.PREFIX)) {
                propsToRemove.add(key.toString());
            }
        }

        for (String propToRemove : propsToRemove) {
            systemProps.remove(propToRemove);
        }
    }

    protected void removeRealm(String realmName) {
        try {
            adminClient.realm(realmName).remove();
        } catch (Exception e) {
            // Realm doesn't exist
        }
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

    private void assertRealmNames(List<RealmRepresentation> realms, String... expectedNames) {
        Set<String> actualNames = realms.stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet());
        assertThat(actualNames, hasItems(expectedNames));
    }

    private void importRealmFromFile(String path) {
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        URL url = ExportImportTest.class.getResource(path);
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        runOnServerMaster.run(ExportImportHelper.setFile(targetFilePath));

        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));

        runOnServerMaster.run(ExportImportHelper.runImport());
    }

    private static FetchOnServerWrapper<Boolean> validCredentials(String userName, String password) {
        return new FetchOnServerWrapper<>() {

            @Override
            public FetchOnServer getRunOnServer() {
                return session -> {
                    RealmModel realm = session.realms().getRealmByName("test");
                    if (realm == null) {
                        return false;
                    }
                    UserProvider userProvider = session.getProvider(UserProvider.class);
                    UserModel user = userProvider.getUserByUsername(realm, userName);
                    return user.credentialManager().isValid(UserCredentialModel.password(password));
                };
            }

            @Override
            public Class<Boolean> getResultClass() {
                return Boolean.class;
            }
        };
    }

    public static class ExportImportRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // Build the current representation (which was loaded from /testrealm.json)
            RealmRepresentation testRealm1 = realm.build();

            if (testRealm1.getUsers() == null) {
                testRealm1.setUsers(new ArrayList<>());
            }

            testRealm1.getUsers().add(makeUser("user1"));
            testRealm1.getUsers().add(makeUser("user2"));
            testRealm1.getUsers().add(makeUser("user3"));

            testRealm1.getUsers().add(UserBuilder.create("user-requiredOTP")
                    .email("User-requiredOTP@test.com")
                    .password("password")
                    .requiredActions(UserModel.RequiredAction.CONFIGURE_TOTP.name())
                    .enabled(true).build());

            testRealm1.getUsers().add(UserBuilder.create("user-requiredWebAuthn")
                    .email("User-requiredWebAuthn@test.com")
                    .password("password")
                    .requiredActions(WebAuthnRegisterFactory.PROVIDER_ID)
                    .enabled(true).build());

            if (testRealm1.getSmtpServer() != null) {
                testRealm1.getSmtpServer().put("password", "secret");
            }
            setEventsConfig(testRealm1);
            setLocalizationTexts(testRealm1);

            setEventsConfig(testRealm1);
            setLocalizationTexts(testRealm1);
            return RealmBuilder.update(testRealm1);
        }
    }
}
