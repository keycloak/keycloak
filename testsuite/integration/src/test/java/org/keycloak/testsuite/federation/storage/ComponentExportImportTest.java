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
import org.keycloak.component.ComponentModel;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ComponentExportImportTest {
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
        RealmModel realm = session.realms().getRealmByName("exported-component");
        if (realm != null) {
            session.realms().removeRealm(realm.getId());
        }
        keycloakRule.stopSession(session, true);
    }


    @Test
    public void testSingleFile() throws Exception {
        clearExportImportProperties();
        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = new RealmManager(session).createRealm("exported-component");
        String realmId = realm.getId();
        ComponentModel component = new ComponentModel();
        component.setParentId(realm.getId());
        component.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        component.setProviderType(UserStorageProvider.class.getName());
        component.setName("parent");
        component.setSubType("subtype");
        component.put("attr", "value");
        component = realm.addComponentModel(component);
        ComponentModel subComponent = new ComponentModel();
        subComponent.setParentId(component.getId());
        subComponent.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        subComponent.setProviderType(UserStorageProvider.class.getName());
        subComponent.setName("child");
        subComponent.setSubType("subtype2");
        subComponent.put("attr", "value2");
        subComponent = realm.addComponentModel(subComponent);
        keycloakRule.stopSession(session, true);




        String targetFilePath = basePath + File.separator + "singleFile-full.json";
        System.out.println("export file: " + targetFilePath);
        session = keycloakRule.startSession();
        ExportImportConfig.setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        ExportImportConfig.setFile(targetFilePath);
        ExportImportConfig.setRealmName("exported-component");
        ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
        new ExportImportManager(session).runExport();
        session.realms().removeRealm(realmId);
        keycloakRule.stopSession(session, true);

        session = keycloakRule.startSession();
        Assert.assertNull(session.realms().getRealmByName("exported-component"));
        ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
        new ExportImportManager(session).runImport();
        keycloakRule.stopSession(session, true);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("exported-component");
        Assert.assertNotNull(realm);
        component = realm.getComponent(component.getId());
        Assert.assertNotNull(component);
        Assert.assertEquals(component.getParentId(), realm.getId());
        Assert.assertEquals(component.getName(), "parent");
        Assert.assertEquals(component.getSubType(), "subtype");
        Assert.assertEquals(component.getProviderId(), UserMapStorageFactory.PROVIDER_ID);
        Assert.assertEquals(component.getProviderType(), UserStorageProvider.class.getName());
        Assert.assertEquals(component.getConfig().getFirst("attr"), "value");
        subComponent = realm.getComponents(component.getId()).get(0);
        Assert.assertEquals(subComponent.getParentId(), component.getId());
        Assert.assertEquals(subComponent.getName(), "child");
        Assert.assertEquals(subComponent.getSubType(), "subtype2");
        Assert.assertEquals(subComponent.getProviderId(), UserMapStorageFactory.PROVIDER_ID);
        Assert.assertEquals(subComponent.getProviderType(), UserStorageProvider.class.getName());
        Assert.assertEquals(subComponent.getConfig().getFirst("attr"), "value2");

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
