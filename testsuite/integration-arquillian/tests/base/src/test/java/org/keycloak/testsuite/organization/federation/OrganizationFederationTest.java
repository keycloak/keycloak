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

package org.keycloak.testsuite.organization.federation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.federation.ldap.AbstractLDAPTest;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.runonserver.RunOnServer;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OrganizationFederationTest extends AbstractOrganizationTest {

    private static final File CONFIG_DIR = new File(System.getProperty("auth.server.config.dir", ""));

    @Before
    public void onBefore() {
        createOrganization("orga");
        createOrganization("orgb");
    }

    @After
    public void onAfter() {
        List<UserRepresentation> users = testRealm().users().search("member");

        if (!users.isEmpty()) {
            UserRepresentation member = users.get(0);
            testRealm().users().get(member.getId()).remove();
        }
    }

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        log.infof("Test realms imported");

        afterImportTestRealm();
    }

    protected void afterImportTestRealm() {
        copyPropertiesFiles();
        final String propertyFile = CONFIG_DIR.getAbsolutePath() + File.separator + "user-password.properties";
        testingClient.server().run(session -> {

            RealmModel appRealm = session.realms().getRealmByName(AbstractLDAPTest.TEST_REALM_NAME);

            // add user-prop provider with lower priority
            ComponentModel userPropProvider = new ComponentModel();
            userPropProvider.setName("user-props");
            userPropProvider.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
            userPropProvider.setProviderType(UserStorageProvider.class.getName());
            userPropProvider.setConfig(new MultivaluedHashMap<>());
            userPropProvider.getConfig().putSingle("priority", Integer.toString(0));
            userPropProvider.getConfig().putSingle("propertyFile", propertyFile);
            userPropProvider.getConfig().putSingle("federatedStorage", "true");
            appRealm.addComponentModel(userPropProvider);
        });
    }

    private void copyPropertiesFiles() throws RuntimeException {
        try {
            // copy files used by the following user-props user provider
            File stResDir = new File(getClass().getResource("/storage-test").toURI());
            if (stResDir.exists() && stResDir.isDirectory() && CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) {
                for (File f : stResDir.listFiles()) {
                    log.infof("Copying %s to %s", f.getName(), CONFIG_DIR.getAbsolutePath());
                    FileUtils.copyFileToDirectory(f, CONFIG_DIR);
                }
            } else {
                throw new RuntimeException("Property `auth.server.config.dir` must be set to run the test.");
            }
        } catch (IOException | RuntimeException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetByMember() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "thor");
            orgProvider.addMember(orga, member);
        });
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, "thor");
            Stream<OrganizationModel> memberOf = orgProvider.getByMember(member);
            List<OrganizationModel> results = memberOf.toList();
            assertEquals(1, results.size());
            assertEquals("orga", results.get(0).getAlias());
        });
    }

}
