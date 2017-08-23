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
package org.keycloak.testsuite.federation.storage;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.policy.HashAlgorithmPasswordPolicyProviderFactory;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FederatedStorageExportImportTest {
    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {

        }
    });

    public static String basePath = null;

    @BeforeClass
    public static void setDirs() {
        basePath = new File(System.getProperty("project.build.directory", "target")).getAbsolutePath();

    }

    @After
    public void cleanup() {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("exported");
        if (realm != null) {
            session.realms().removeRealm(realm.getId());
        }
        keycloakRule.stopSession(session, true);
    }

    protected PasswordHashProvider getHashProvider(KeycloakSession session, PasswordPolicy policy) {
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, policy.getHashAlgorithm());
        if (hash == null) {
            return session.getProvider(PasswordHashProvider.class, PasswordPolicy.HASH_ALGORITHM_DEFAULT);
        }
        return hash;
    }


    @Test
    public void testSingleFile() throws Exception {
        clearExportImportProperties();
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = new RealmManager(session).createRealm("exported");
        String realmId = realm.getId();
        RoleModel role = realm.addRole("test-role");
        GroupModel group = realm.createGroup("test-group");
        String groupId = group.getId();
        String userId = "f:1:path";
        List<String> attrValues = new LinkedList<>();
        attrValues.add("1");
        attrValues.add("2");
        session.userFederatedStorage().setSingleAttribute(realm, userId, "single1", "value1");
        session.userFederatedStorage().setAttribute(realm, userId, "list1", attrValues);
        session.userFederatedStorage().addRequiredAction(realm, userId, "UPDATE_PASSWORD");
        CredentialModel credential = new CredentialModel();
        getHashProvider(session, realm.getPasswordPolicy()).encode("password", realm.
                getPasswordPolicy().getHashIterations(), credential);
        session.userFederatedStorage().createCredential(realm, userId, credential);
        session.userFederatedStorage().grantRole(realm, userId, role);
        session.userFederatedStorage().joinGroup(realm, userId, group);
        keycloakRule.stopSession(session, true);




        String targetFilePath = basePath + File.separator + "singleFile-full.json";
        System.out.println("export file: " + targetFilePath);
        session = keycloakRule.startSession();
        ExportImportConfig.setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        ExportImportConfig.setFile(targetFilePath);
        ExportImportConfig.setRealmName("exported");
        ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
        new ExportImportManager(session).runExport();
        session.realms().removeRealm(realmId);
        keycloakRule.stopSession(session, true);

        session = keycloakRule.startSession();
        Assert.assertNull(session.realms().getRealmByName("exported"));
        ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
        new ExportImportManager(session).runImport();
        keycloakRule.stopSession(session, true);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("exported");
        Assert.assertNotNull(realm);
        role = realm.getRole("test-role");
        group = realm.getGroupById(groupId);

        Assert.assertEquals(1, session.userFederatedStorage().getStoredUsersCount(realm));
        MultivaluedHashMap<String, String> attributes = session.userFederatedStorage().getAttributes(realm, userId);
        Assert.assertEquals(3, attributes.size());
        Assert.assertEquals("value1", attributes.getFirst("single1"));
        Assert.assertTrue(attributes.getList("list1").contains("1"));
        Assert.assertTrue(attributes.getList("list1").contains("2"));
        Assert.assertTrue(session.userFederatedStorage().getRequiredActions(realm, userId).contains("UPDATE_PASSWORD"));
        Assert.assertTrue(session.userFederatedStorage().getRoleMappings(realm, userId).contains(role));
        Assert.assertTrue(session.userFederatedStorage().getGroups(realm, userId).contains(group));
        List<CredentialModel> creds = session.userFederatedStorage().getStoredCredentials(realm, userId);
        Assert.assertEquals(1, creds.size());
        Assert.assertTrue(getHashProvider(session, realm.getPasswordPolicy()).verify("password", creds.get(0)));

        keycloakRule.stopSession(session, true);

    }

    @Test
    public void testDir() throws Exception {
        clearExportImportProperties();
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = new RealmManager(session).createRealm("exported");
        String realmId = realm.getId();
        RoleModel role = realm.addRole("test-role");
        GroupModel group = realm.createGroup("test-group");
        String groupId = group.getId();
        String userId = "f:1:path";
        List<String> attrValues = new LinkedList<>();
        attrValues.add("1");
        attrValues.add("2");
        session.userFederatedStorage().setSingleAttribute(realm, userId, "single1", "value1");
        session.userFederatedStorage().setAttribute(realm, userId, "list1", attrValues);
        session.userFederatedStorage().addRequiredAction(realm, userId, "UPDATE_PASSWORD");
        CredentialModel credential = new CredentialModel();
        getHashProvider(session, realm.getPasswordPolicy()).encode("password", realm.
                getPasswordPolicy().getHashIterations(), credential);
        session.userFederatedStorage().createCredential(realm, userId, credential);
        session.userFederatedStorage().grantRole(realm, userId, role);
        session.userFederatedStorage().joinGroup(realm, userId, group);
        session.userFederatedStorage().setNotBeforeForUser(realm, userId, 50);
        keycloakRule.stopSession(session, true);




        String targetFilePath = basePath + File.separator + "dirExport";
        session = keycloakRule.startSession();
        ExportImportConfig.setProvider(DirExportProviderFactory.PROVIDER_ID);
        ExportImportConfig.setDir(targetFilePath);
        ExportImportConfig.setRealmName("exported");
        ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
        new ExportImportManager(session).runExport();
        session.realms().removeRealm(realmId);
        keycloakRule.stopSession(session, true);

        session = keycloakRule.startSession();
        Assert.assertNull(session.realms().getRealmByName("exported"));
        ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
        new ExportImportManager(session).runImport();
        keycloakRule.stopSession(session, true);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("exported");
        Assert.assertNotNull(realm);
        role = realm.getRole("test-role");
        group = realm.getGroupById(groupId);

        Assert.assertEquals(1, session.userFederatedStorage().getStoredUsersCount(realm));
        MultivaluedHashMap<String, String> attributes = session.userFederatedStorage().getAttributes(realm, userId);
        Assert.assertEquals(3, attributes.size());
        Assert.assertEquals("value1", attributes.getFirst("single1"));
        Assert.assertTrue(attributes.getList("list1").contains("1"));
        Assert.assertTrue(attributes.getList("list1").contains("2"));
        Assert.assertTrue(session.userFederatedStorage().getRequiredActions(realm, userId).contains("UPDATE_PASSWORD"));
        Assert.assertTrue(session.userFederatedStorage().getRoleMappings(realm, userId).contains(role));
        Assert.assertTrue(session.userFederatedStorage().getGroups(realm, userId).contains(group));
        Assert.assertEquals(50, session.userFederatedStorage().getNotBeforeOfUser(realm, userId));
        List<CredentialModel> creds = session.userFederatedStorage().getStoredCredentials(realm, userId);
        Assert.assertEquals(1, creds.size());
        Assert.assertTrue(getHashProvider(session, realm.getPasswordPolicy()).verify("password", creds.get(0)));

        keycloakRule.stopSession(session, true);

    }

    public void clearExportImportProperties() {
        // Clear export/import properties after test
        Properties systemProps = System.getProperties();
        Set<String> propsToRemove = new HashSet<String>();

        for (Object key : systemProps.keySet()) {
            if (key.toString().startsWith(ExportImportConfig.PREFIX)) {
                propsToRemove.add(key.toString());
            }
        }

        for (String propToRemove : propsToRemove) {
            systemProps.remove(propToRemove);
        }
    }




}
