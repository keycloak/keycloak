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

package org.keycloak.testsuite.organization.admin;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileImportProviderFactory;
import org.keycloak.models.OrganizationModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestingExportImportResource;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.UserBuilder;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationExportTest extends AbstractOrganizationTest {

    @Test
    public void testExport() {
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        List<String> expectedOrganizations = new ArrayList<>();
        Map<String, List<String>> expectedManagedMembers = new HashMap<>();
        Map<String, List<String>> expectedUnmanagedMembers = new HashMap<>();

        for (int i = 0; i < 2; i++) {
            IdentityProviderRepresentation broker = bc.setUpIdentityProvider();
            broker.setAlias("broker-org-" + i);
            broker.setInternalId(null);
            String domain = "org-" + i + ".org";
            OrganizationRepresentation orgRep = createOrganization(testRealm(), getCleanup(), "org-" + i, broker, domain);
            OrganizationResource organization = testRealm().organizations().get(orgRep.getId());

            expectedOrganizations.add(orgRep.getName());

            for (int j = 0; j < 3; j++) {
                UserRepresentation member = addMember(organization, "realmuser-" + j + "@" + domain);
                expectedUnmanagedMembers.computeIfAbsent(orgRep.getName(), s -> new ArrayList<>()).add(member.getUsername());
            }

            UsersResource federatedUsers = providerRealm.users();

            for (int j = 0; j < 3; j++) {
                String email = "feduser" + j + "@" + domain;

                federatedUsers.create(UserBuilder.create()
                        .username(email)
                        .email(email)
                        .firstName("f")
                        .lastName("l")
                        .enabled(true)
                        .password("password")
                        .build()).close();

                expectedManagedMembers.computeIfAbsent(orgRep.getName(), s -> new ArrayList<>()).add(email);

                oauth.clientId("broker-app");
                loginPage.open(bc.consumerRealmName());
                log.debug("Logging in");
                loginPage.loginUsername(email);
                // user automatically redirected to the organization identity provider
                waitForPage(driver, "sign in to", true);
                Assert.assertTrue("Driver should be on the provider realm page right now",
                        driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
                // login to the organization identity provider and run the configured first broker login flow
                loginPage.login(email, bc.getUserPassword());
                assertIsMember(email, organization);
                testRealm().logoutAll();
                providerRealm.logoutAll();
            }
        }

        // export
        TestingExportImportResource exportImport = testingClient.testing().exportImport();
        exportImport.setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        exportImport.setAction(ExportImportConfig.ACTION_EXPORT);
        exportImport.setRealmName(testRealm().toRepresentation().getRealm());
        String targetFilePath = exportImport.getExportImportTestDirectory() + File.separator + "org-export.json";
        exportImport.setFile(targetFilePath);
        exportImport.runExport();

        // remove the realm and import it back
        testRealm().remove();
        exportImport = testingClient.testing().exportImport();
        exportImport.setProvider(SingleFileImportProviderFactory.PROVIDER_ID);
        exportImport.setAction(ExportImportConfig.ACTION_IMPORT);
        exportImport.setFile(targetFilePath);
        exportImport.runImport();
        getCleanup().addCleanup(() -> testRealm().remove());

        RealmRepresentation importedRealm = testRealm().toRepresentation();

        assertTrue(importedRealm.isOrganizationsEnabled());

        List<OrganizationRepresentation> organizations = testRealm().organizations().getAll();
        assertEquals(expectedOrganizations.size(), organizations.size());
        assertThat(organizations.stream().map(OrganizationRepresentation::getName).toList(), Matchers.containsInAnyOrder(expectedOrganizations.toArray()));

        for (OrganizationRepresentation orgRep : organizations) {
            OrganizationResource organization = testRealm().organizations().get(orgRep.getId());
            List<String> members = organization.members().getAll().stream().map(UserRepresentation::getEmail).toList();
            assertEquals(members.size(), expectedUnmanagedMembers.get(orgRep.getName()).size() + expectedManagedMembers.get(orgRep.getName()).size());
            assertTrue(members.containsAll(expectedUnmanagedMembers.get(orgRep.getName())));
            assertTrue(members.containsAll(expectedManagedMembers.get(orgRep.getName())));
        }

        // make sure a managed user can authenticate through the broker associated with an org
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        String email = expectedManagedMembers.values().stream().findAny().get().get(0);
        loginPage.loginUsername(email);
        // user automatically redirected to the organization identity provider
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        // login to the organization identity provider and run the configured first broker login flow
        loginPage.login(email, bc.getUserPassword());
        assertThat(appPage.getRequestType(),is(AppPage.RequestType.AUTH_RESPONSE));
    }

    @Test
    public void testPartialExport() {
        createOrganization();
        assertPartialExportImport(false, false);
        assertPartialExportImport(true, false);
        assertPartialExportImport(true, true);
        assertPartialExportImport(false, true);
    }

    private void assertPartialExportImport(boolean exportGroupsAndRoles, boolean exportClients) {
        RealmRepresentation export = testRealm().partialExport(exportGroupsAndRoles, exportClients);
        assertTrue(Optional.ofNullable(export.getGroups()).orElse(List.of()).stream().noneMatch(g -> g.getAttributes().containsKey(OrganizationModel.ORGANIZATION_ATTRIBUTE)));
        assertTrue(Optional.ofNullable(export.getOrganizations()).orElse(List.of()).isEmpty());
        assertTrue(Optional.ofNullable(export.getIdentityProviders()).orElse(List.of()).stream().noneMatch(g -> g.getConfig().containsKey(OrganizationModel.ORGANIZATION_ATTRIBUTE)));
        PartialImportRepresentation rep = new PartialImportRepresentation();
        rep.setUsers(export.getUsers());
        rep.setClients(export.getClients());
        rep.setRoles(export.getRoles());
        rep.setIdentityProviders(export.getIdentityProviders());
        rep.setGroups(export.getGroups());
        testRealm().partialImport(rep).close();
    }
}
