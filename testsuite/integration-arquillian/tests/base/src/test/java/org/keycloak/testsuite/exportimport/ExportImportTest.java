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

package org.keycloak.testsuite.exportimport;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.junit.After;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.Profile.Feature;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.client.resources.TestingExportImportResource;
import org.keycloak.testsuite.runonserver.RunHelpers;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.junit.BeforeClass;

/**
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ExportImportTest extends AbstractKeycloakTest {

    @BeforeClass
    public static void checkNotMapStorage() {
        // Disabled temporarily, re-enable once export/import functionality is implemented for map storage
        ProfileAssume.assumeFeatureDisabled(Feature.MAP_STORAGE);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealm1 = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealm1.getUsers().add(makeUser("user1"));
        testRealm1.getUsers().add(makeUser("user2"));
        testRealm1.getUsers().add(makeUser("user3"));

        testRealm1.getUsers().add(
                UserBuilder.create()
                        .username("user-requiredOTP")
                        .email("User-requiredOTP" + "@test.com")
                        .password("password")
                        .requiredAction(UserModel.RequiredAction.CONFIGURE_TOTP.name())
                        .build()
        );
        testRealm1.getUsers().add(
                UserBuilder.create()
                        .username("user-requiredWebAuthn")
                        .email("User-requiredWebAuthn" + "@test.com")
                        .password("password")
                        .requiredAction(WebAuthnRegisterFactory.PROVIDER_ID)
                        .build()
        );

        testRealm1.getSmtpServer().put("password", "secret");

        setEventsConfig(testRealm1);
        testRealms.add(testRealm1);

        RealmRepresentation testRealm2 = loadJson(getClass().getResourceAsStream("/model/testrealm.json"), RealmRepresentation.class);
        testRealm2.setId("test-realm");
        testRealms.add(testRealm2);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    public void beforeAbstractKeycloakTestRealmImport() {
        removeAllRealmsDespiteMaster();
    }

    private void setEventsConfig(RealmRepresentation realm) {
        realm.setEventsEnabled(true);
        realm.setAdminEventsEnabled(true);
        realm.setAdminEventsDetailsEnabled(true);
        realm.setEventsExpiration(600);
        realm.setEnabledEventTypes(Arrays.asList("REGISTER", "REGISTER_ERROR", "LOGIN", "LOGIN_ERROR", "LOGOUT_ERROR"));
    }

    private void checkEventsConfig(RealmEventsConfigRepresentation config) {
        Assert.assertTrue(config.isEventsEnabled());
        Assert.assertTrue(config.isAdminEventsEnabled());
        Assert.assertTrue(config.isAdminEventsDetailsEnabled());
        Assert.assertEquals((Long) 600L, config.getEventsExpiration());
        Assert.assertNames(new HashSet(config.getEnabledEventTypes()),"REGISTER", "REGISTER_ERROR", "LOGIN", "LOGIN_ERROR", "LOGOUT_ERROR");
    }

    private UserRepresentation makeUser(String userName) {
        return UserBuilder.create()
                .username(userName)
                .email(userName + "@test.com")
                .password("password")
                .build();
    }

    @After
    public void clearExportImportProps() throws LifecycleException {
        clearExportImportProperties();
    }

    @Test
    public void testDirFullExportImport() throws Throwable {
        testingClient.testing().exportImport().setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = testingClient.testing().exportImport().getExportImportTestDirectory()+ File.separator + "dirExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        testingClient.testing().exportImport().setDir(targetDirPath);
        testingClient.testing().exportImport().setUsersPerFile(ExportImportConfig.DEFAULT_USERS_PER_FILE);

        testFullExportImport();

        RealmResource testRealmRealm = adminClient.realm("test-realm");
        ExportImportUtil.assertDataImportedInRealm(adminClient, testingClient, testRealmRealm.toRepresentation());

        // There should be 6 files in target directory (3 realm, 3 user)
        assertEquals(6, new File(targetDirPath).listFiles().length);
    }

    @Test
    public void testDirRealmExportImport() throws Throwable {
        testingClient.testing()
                .exportImport()
                .setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "dirRealmExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        testingClient.testing().exportImport().setDir(targetDirPath);
        testingClient.testing().exportImport().setUsersPerFile(5);

        testRealmExportImport();

        RealmResource testRealmRealm = adminClient.realm("test-realm");
        ExportImportUtil.assertDataImportedInRealm(adminClient, testingClient, testRealmRealm.toRepresentation());

        // There should be 4 files in target directory (1 realm, 12 users, 5 users per file)
        // (+ additional user service-account-test-app-authz that should not be there ???)
        File[] files = new File(targetDirPath).listFiles();
        assertEquals(4, files.length);
    }

    @Test
    public void testSingleFileFullExportImport() throws Throwable {
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "singleFile-full.json";
        testingClient.testing().exportImport().setFile(targetFilePath);

        testFullExportImport();
    }

    @Test
    public void testSingleFileRealmExportImport() throws Throwable {
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "singleFile-realm.json";
        testingClient.testing().exportImport().setFile(targetFilePath);

        testRealmExportImport();
    }

    @Test
    public void testSingleFileRealmWithoutBuiltinsImport() throws Throwable {
        // Remove test realm
        removeRealm("test-realm");

        // Set the realm, which doesn't have builtin clients/roles inside JSON
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        URL url = ExportImportTest.class.getResource("/model/testrealm.json");
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        testingClient.testing().exportImport().setFile(targetFilePath);

        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();

        RealmResource testRealmRealm = adminClient.realm("test-realm");

        ExportImportUtil.assertDataImportedInRealm(adminClient, testingClient, testRealmRealm.toRepresentation());
    }

    @Test
    public void testImportFromPartialExport() {
        // import a realm with clients without roles
        importRealmFromFile("/import/partial-import.json");
        Assert.assertTrue("Imported realm hasn't been found!", isRealmPresent("partial-import"));
        addTestRealmToTestRealmReps("partial-import");

        // import a realm with clients without roles
        importRealmFromFile("/import/import-without-roles.json");
        Assert.assertTrue("Imported realm hasn't been found!", isRealmPresent("import-without-roles"));
        addTestRealmToTestRealmReps("import-without-roles");

        // import a realm with roles without clients
        importRealmFromFile("/import/import-without-clients.json");
        Assert.assertTrue("Imported realm hasn't been found!", isRealmPresent("import-without-clients"));
        addTestRealmToTestRealmReps("import-without-clients");
    }

    @Test
    public void testImportWithNullAuthenticatorConfigAndNoDefaultBrowserFlow() {
        importRealmFromFile("/import/testrealm-authenticator-config-null.json");
        Assert.assertTrue("Imported realm hasn't been found!", isRealmPresent("cez"));
    }

    @Test
    public void testImportIgnoreExistingMissingClientId() {
        TestingExportImportResource resource = testingClient.testing().exportImport();

        resource.setStrategy(Strategy.IGNORE_EXISTING);
        resource.setProvider(DirExportProviderFactory.PROVIDER_ID);

        String targetDirPath = resource.getExportImportTestDirectory() + File.separator + "dirRealmExport";
        File dest = new File(targetDirPath);
        try {
            DirExportProvider.recursiveDeleteDir(dest);
            resource.setDir(targetDirPath);

            resource.setAction(ExportImportConfig.ACTION_EXPORT);

            URL url = ExportImportTest.class.getResource("/model/testrealm.json");
            File testRealm = new File(url.getFile());
            assertThat(testRealm, Matchers.notNullValue());

            File newFile = new File("target", "test-new-realm.json");

            try {
                FileUtils.copyFile(testRealm, newFile);
                FileUtils.copyFileToDirectory(newFile, dest);
            } catch (IOException e) {
                Assert.fail("Cannot copy file. Details: " + e.getMessage());
            }

            File existingFile = FileUtils.getFile(dest, newFile.getName());
            assertThat(existingFile, Matchers.notNullValue());

            resource.runExport();
            resource.setAction(ExportImportConfig.ACTION_IMPORT);

            try {
                resource.runImport();
                resource.runImport();
            } catch (Exception e) {
                Assert.fail("Error with realm importing twice. Details: " + e.getMessage());
            }
        } finally {
            DirExportProvider.recursiveDeleteDir(dest);
        }
    }

    private boolean isRealmPresent(String realmName) {
        return adminClient.realms().findAll().stream().anyMatch(realm -> realmName.equals(realm.getRealm()));
    }

    /*
     * non-JavaDoc
     *
     * Adds a testTealm to TestContext.testRealmReps (which are after testClass removed)
     *
     * It prevents from affecting other tests. (auth-server-undertow)
     *
     */
    private void addTestRealmToTestRealmReps(String realm) {
        testContext.addTestRealmToTestRealmReps(adminClient.realms().realm(realm).toRepresentation());
    }

    private void testFullExportImport() throws LifecycleException {
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().exportImport().setRealmName("");

        testingClient.testing().exportImport().runExport();

        removeRealm("test");
        removeRealm("test-realm");
        Assert.assertNames(adminClient.realms().findAll(), "master");

        Map<String, RequiredActionProviderRepresentation> requiredActionsBeforeImport = new HashMap<>();
        adminClient.realm("master").flows().getRequiredActions().stream()
                .forEach(action -> {
                    requiredActionsBeforeImport.put(action.getAlias(), action);
                });

        assertNotAuthenticated("test", "test-user@localhost", "password");
        assertNotAuthenticated("test", "user1", "password");
        assertNotAuthenticated("test", "user2", "password");
        assertNotAuthenticated("test", "user3", "password");
        assertNotAuthenticated("test", "user-requiredOTP", "password");
        assertNotAuthenticated("test", "user-requiredWebAuthn", "password");


        // Configure import
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();

        // Ensure data are imported back
        Assert.assertNames(adminClient.realms().findAll(), "master", "test", "test-realm");

        assertAuthenticated("test", "test-user@localhost", "password");
        assertAuthenticated("test", "user1", "password");
        assertAuthenticated("test", "user2", "password");
        assertAuthenticated("test", "user3", "password");
        assertAuthenticated("test", "user-requiredOTP", "password");
        assertAuthenticated("test", "user-requiredWebAuthn", "password");

        RealmResource testRealmRealm = adminClient.realm("test");
        assertTrue(testRealmRealm.users().search("user-requiredOTP").get(0)
                .getRequiredActions().get(0).equals(UserModel.RequiredAction.CONFIGURE_TOTP.name()));
        assertTrue(testRealmRealm.users().search("user-requiredWebAuthn").get(0)
                .getRequiredActions().get(0).equals(WebAuthnRegisterFactory.PROVIDER_ID));

        // KEYCLOAK-6050 Check SMTP password is exported/imported
        assertEquals("secret", testingClient.server("test").fetch(RunHelpers.internalRealm()).getSmtpServer().get("password"));

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

    private void testRealmExportImport() throws LifecycleException {
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().exportImport().setRealmName("test");

        testingClient.testing().exportImport().runExport();

        List<ComponentRepresentation> components = adminClient.realm("test").components().query();
        KeysMetadataRepresentation keyMetadata = adminClient.realm("test").keys().getKeyMetadata();
        String sampleRealmRoleId = adminClient.realm("test").roles().get("sample-realm-role").toRepresentation().getId();
        Map<String, List<String>> roleAttributes = adminClient.realm("test").roles().get("attribute-role").toRepresentation().getAttributes();
        String testAppId = adminClient.realm("test").clients().findByClientId("test-app").get(0).getId();
        String sampleClientRoleId = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getId();
        String sampleClientRoleAttribute = adminClient.realm("test").clients().get(testAppId).roles().get("sample-client-role").toRepresentation().getAttributes().get("sample-client-role-attribute").get(0);

        // Delete some realm (and some data in admin realm)
        adminClient.realm("test").remove();

        Assert.assertNames(adminClient.realms().findAll(), "test-realm", "master");

        assertNotAuthenticated("test", "test-user@localhost", "password");
        assertNotAuthenticated("test", "user1", "password");
        assertNotAuthenticated("test", "user2", "password");
        assertNotAuthenticated("test", "user3", "password");
        assertNotAuthenticated("test", "user-requiredOTP", "password");
        assertNotAuthenticated("test", "user-requiredWebAuthn", "password");

        // Configure import
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();

        // Ensure data are imported back, but just for "test" realm
        Assert.assertNames(adminClient.realms().findAll(), "master", "test", "test-realm");

        assertAuthenticated("test", "test-user@localhost", "password");
        assertAuthenticated("test", "user1", "password");
        assertAuthenticated("test", "user2", "password");
        assertAuthenticated("test", "user3", "password");
        assertAuthenticated("test", "user-requiredOTP", "password");
        assertAuthenticated("test", "user-requiredWebAuthn", "password");

        RealmResource testRealmRealm = adminClient.realm("test");
        assertTrue(testRealmRealm.users().search("user-requiredOTP").get(0)
                .getRequiredActions().get(0).equals(UserModel.RequiredAction.CONFIGURE_TOTP.name()));
        assertTrue(testRealmRealm.users().search("user-requiredWebAuthn").get(0)
                .getRequiredActions().get(0).equals(WebAuthnRegisterFactory.PROVIDER_ID));


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

    private void assertAuthenticated(String realmName, String username, String password) {
        assertAuth(true, realmName, username, password);
    }

    private void assertNotAuthenticated(String realmName, String username, String password) {
        assertAuth(false, realmName, username, password);
    }

    private void assertAuth(boolean expectedResult, String realmName, String username, String password) {
        assertEquals(expectedResult, testingClient.testing().validCredentials(realmName, username, password));
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

    private void importRealmFromFile(String path) {
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        URL url = ExportImportTest.class.getResource(path);
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        testingClient.testing().exportImport().setFile(targetFilePath);

        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();
    }
}
