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

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.junit.After;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.UserBuilder;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ExportImportTest extends AbstractExportImportTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealm1 = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealm1.getUsers().add(makeUser("user1"));
        testRealm1.getUsers().add(makeUser("user2"));
        testRealm1.getUsers().add(makeUser("user3"));
        testRealms.add(testRealm1);

        RealmRepresentation testRealm2 = loadJson(getClass().getResourceAsStream("/model/testrealm.json"), RealmRepresentation.class);
        testRealm2.setId("test-realm");
        testRealms.add(testRealm2);
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
        testingClient.testing().setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = testingClient.testing().getExportImportTestDirectory()+ File.separator + "dirExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        testingClient.testing().setDir(targetDirPath);
        testingClient.testing().setUsersPerFile(ExportImportConfig.DEFAULT_USERS_PER_FILE);

        testFullExportImport();

        // There should be 6 files in target directory (3 realm, 3 user)
        Assert.assertEquals(6, new File(targetDirPath).listFiles().length);
    }

    @Test
    public void testDirRealmExportImport() throws Throwable {
        testingClient.testing().setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = testingClient.testing().getExportImportTestDirectory() + File.separator + "dirRealmExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        testingClient.testing().setDir(targetDirPath);
        testingClient.testing().setUsersPerFile(3);

        testRealmExportImport();

        // There should be 3 files in target directory (1 realm, 3 user)
        File[] files = new File(targetDirPath).listFiles();
        Assert.assertEquals(4, files.length);
    }

    @Test
    public void testSingleFileFullExportImport() throws Throwable {
        testingClient.testing().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().getExportImportTestDirectory() + File.separator + "singleFile-full.json";
        testingClient.testing().setFile(targetFilePath);

        testFullExportImport();
    }

    @Test
    public void testSingleFileRealmExportImport() throws Throwable {
        testingClient.testing().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().getExportImportTestDirectory() + File.separator + "singleFile-realm.json";
        testingClient.testing().setFile(targetFilePath);

        testRealmExportImport();
    }

    @Test
    public void testSingleFileRealmWithoutBuiltinsImport() throws Throwable {
        // Remove test realm
        removeRealm("test-realm");

        // Set the realm, which doesn't have builtin clients/roles inside JSON
        testingClient.testing().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        URL url = ExportImportTest.class.getResource("/model/testrealm.json");
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        testingClient.testing().setFile(targetFilePath);

        testingClient.testing().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().runImport();

        RealmResource testRealmRealm = adminClient.realm("test-realm");

        ExportImportUtil.assertDataImportedInRealm(adminClient, testingClient, testRealmRealm.toRepresentation());
    }

    @Test
    public void testComponentExportImport() throws Throwable {
        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm("component-realm");
        adminClient.realms().create(realmRep);
        Assert.assertEquals(4, adminClient.realms().findAll().size());
        RealmResource realm = adminClient.realm("component-realm");
        realmRep = realm.toRepresentation();
        ComponentRepresentation component = new ComponentRepresentation();
        component.setProviderId("dummy");
        component.setProviderType("dummyType");
        component.setName("dummy-name");
        component.setParentId(realmRep.getId());
        component.setConfig(new MultivaluedHashMap<>());
        component.getConfig().add("name", "value");
        realm.components().add(component);


        testingClient.testing().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);

        String targetFilePath = testingClient.testing().getExportImportTestDirectory() + File.separator + "singleFile-realm.json";
        testingClient.testing().setFile(targetFilePath);
        testingClient.testing().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().setRealmName("component-realm");

        testingClient.testing().runExport();

        // Delete some realm (and some data in admin realm)
        adminClient.realm("component-realm").remove();

        Assert.assertEquals(3, adminClient.realms().findAll().size());

        // Configure import
        testingClient.testing().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().runImport();

        realmRep = realm.toRepresentation();

        List<ComponentRepresentation> components = realm.components().query();

        Assert.assertEquals(1, components.size());

        component = components.get(0);

        Assert.assertEquals("dummy-name", component.getName());
        Assert.assertEquals("dummyType", component.getProviderType());
        Assert.assertEquals("dummy", component.getProviderId());
        Assert.assertEquals(realmRep.getId(), component.getParentId());
        Assert.assertEquals(1, component.getConfig().size());
        Assert.assertEquals("value", component.getConfig().getFirst("name"));

        adminClient.realm("component-realm").remove();
    }




    private void removeRealm(String realmName) {
        adminClient.realm(realmName).remove();
    }

    private void testFullExportImport() throws LifecycleException {
        testingClient.testing().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().setRealmName("");

        testingClient.testing().runExport();

        removeRealm("test");
        removeRealm("test-realm");
        Assert.assertEquals(1, adminClient.realms().findAll().size());

        assertNotAuthenticated("test", "test-user@localhost", "password");
        assertNotAuthenticated("test", "user1", "password");
        assertNotAuthenticated("test", "user2", "password");
        assertNotAuthenticated("test", "user3", "password");

        // Configure import
        testingClient.testing().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().runImport();

        // Ensure data are imported back
        Assert.assertEquals(3, adminClient.realms().findAll().size());

        assertAuthenticated("test", "test-user@localhost", "password");
        assertAuthenticated("test", "user1", "password");
        assertAuthenticated("test", "user2", "password");
        assertAuthenticated("test", "user3", "password");
    }

    private void testRealmExportImport() throws LifecycleException {
        testingClient.testing().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().setRealmName("test");

        testingClient.testing().runExport();

        // Delete some realm (and some data in admin realm)
        adminClient.realm("test").remove();

        Assert.assertEquals(2, adminClient.realms().findAll().size());

        assertNotAuthenticated("test", "test-user@localhost", "password");
        assertNotAuthenticated("test", "user1", "password");
        assertNotAuthenticated("test", "user2", "password");
        assertNotAuthenticated("test", "user3", "password");

        // Configure import
        testingClient.testing().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().runImport();

        // Ensure data are imported back, but just for "test" realm
        Assert.assertEquals(3, adminClient.realms().findAll().size());

        assertAuthenticated("test", "test-user@localhost", "password");
        assertAuthenticated("test", "user1", "password");
        assertAuthenticated("test", "user2", "password");
        assertAuthenticated("test", "user3", "password");
    }

    private void assertAuthenticated(String realmName, String username, String password) {
        assertAuth(true, realmName, username, password);
    }

    private void assertNotAuthenticated(String realmName, String username, String password) {
        assertAuth(false, realmName, username, password);
    }

    private void assertAuth(boolean expectedResult, String realmName, String username, String password) {
        Assert.assertEquals(expectedResult, testingClient.testing().validCredentials(realmName, username, password));
    }

    private static String getExportImportTestDirectory() {
        String dirPath = null;
        String relativeDirExportImportPath = "testsuite" + File.separator +
                                             "integration-arquillian" + File.separator +
                                             "tests" + File.separator +
                                             "base" + File.separator +
                                             "target" + File.separator +
                                             "export-import";

        if (System.getProperties().containsKey("maven.home")) {
            dirPath = System.getProperty("user.dir").replaceFirst("testsuite.integration.*", Matcher.quoteReplacement(relativeDirExportImportPath));
        } else {
            for (String c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                if (c.contains(File.separator + "testsuite" + File.separator + "integration-arquillian" + File.separator)) {
                    dirPath = c.replaceFirst("testsuite.integration-arquillian.*", Matcher.quoteReplacement(relativeDirExportImportPath));
                }
            }
        }

        String absolutePath = new File(dirPath).getAbsolutePath();
        return absolutePath;
    }

}
