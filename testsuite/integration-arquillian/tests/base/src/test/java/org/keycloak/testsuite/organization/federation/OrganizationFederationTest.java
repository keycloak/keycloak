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
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.federation.ldap.AbstractLDAPTest;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrganizationFederationTest extends AbstractOrganizationTest {

    private static final File CONFIG_DIR = new File(System.getProperty("auth.server.config.dir", ""));

    @Before
    public void onBefore() {
        createOrganization("orga");
        createOrganization("orgb");
    }

    @After
    public void onAfter() {
        List<UserRepresentation> users = managedRealm.admin().users().search("member");

        if (!users.isEmpty()) {
            UserRepresentation member = users.get(0);
            managedRealm.admin().users().get(member.getId()).remove();
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

    @Test
    public void testGetMembersIncludesFederatedMembers() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orga = orgProvider.getByDomainName("orga.org");
            RealmModel realm = session.getContext().getRealm();
            orgProvider.addManagedMember(orga, session.users().addUser(realm, "local-a"));
            orgProvider.addMember(orga, session.users().addUser(realm, "local-b"));
            orgProvider.addMember(orga, session.users().getUserByUsername(realm, "thor"));
            orgProvider.addMember(orga, session.users().getUserByUsername(realm, "zeus"));
        });

        try {
            getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
                OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
                OrganizationModel orga = orgProvider.getByDomainName("orga.org");

                assertEquals(4, orgProvider.getMembersCount(orga));

                // local members first (sorted by username), then federated members
                assertEquals(List.of("local-a", "local-b", "thor", "zeus"), usernames(orgProvider, orga, Map.of(), null, null, null));

                // paging across local and federated members
                assertEquals(List.of("local-b", "thor"), usernames(orgProvider, orga, Map.of(), null, 1, 2));
                assertEquals(List.of("thor"), usernames(orgProvider, orga, Map.of(), null, 2, 1));
                assertEquals(List.of("zeus"), usernames(orgProvider, orga, Map.of(), null, 3, 10));
                assertEquals(List.of(), usernames(orgProvider, orga, Map.of(), null, 4, 10));

                // search
                assertEquals(List.of("thor"), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "HO"), false, null, null));
                assertEquals(List.of("local-a", "local-b"), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "local"), false, null, null));
                assertEquals(List.of("thor"), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "thor"), true, null, null));
                assertEquals(List.of("thor"), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "t*r"), false, null, null));
                assertEquals(List.of("local-a", "local-b", "thor", "zeus"), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "*"), false, null, null));
                assertEquals(List.of(), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "r*t"), false, null, null));
                assertEquals(List.of(), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "*".repeat(5000) + "x"), false, null, null));
                assertEquals(List.of(), usernames(orgProvider, orga, Map.of(UserModel.SEARCH, "tho"), true, null, null));

                // federated memberships are always unmanaged
                assertEquals(List.of("local-a"), usernames(orgProvider, orga, Map.of(MembershipType.NAME, MembershipType.MANAGED.name()), null, null, null));
                assertEquals(List.of("local-b", "thor", "zeus"), usernames(orgProvider, orga, Map.of(MembershipType.NAME, MembershipType.UNMANAGED.name()), null, null, null));
            });

            // members from a disabled provider can not be resolved, so they are left out of both the list and the count
            setUserPropsProviderEnabled(false);
            getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
                OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
                OrganizationModel orga = orgProvider.getByDomainName("orga.org");

                assertEquals(2, orgProvider.getMembersCount(orga));
                assertEquals(List.of("local-a", "local-b"), usernames(orgProvider, orga, Map.of(), null, null, null));
                assertEquals(List.of(), usernames(orgProvider, orga, Map.of(), null, 2, 10));
            });
        } finally {
            setUserPropsProviderEnabled(true);
            getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
                RealmModel realm = session.getContext().getRealm();
                for (String username : List.of("local-a", "local-b")) {
                    UserModel user = session.users().getUserByUsername(realm, username);
                    if (user != null) {
                        session.users().removeUser(realm, user);
                    }
                }
            });
        }
    }

    private void setUserPropsProviderEnabled(boolean enabled) {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserStorageProviderModel provider = realm.getComponentsStream(realm.getId(), UserStorageProvider.class.getName())
                    .filter(component -> "user-props".equals(component.getName()))
                    .map(UserStorageProviderModel::new)
                    .findFirst()
                    .orElseThrow();
            provider.setEnabled(enabled);
            realm.updateComponent(provider);
        });
    }

    private static List<String> usernames(OrganizationProvider orgProvider, OrganizationModel org, Map<String, String> filters, Boolean exact, Integer first, Integer max) {
        return orgProvider.getMembersStream(org, filters, exact, first, max).map(UserModel::getUsername).toList();
    }

}
