/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationRoleResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.runonserver.LdapHelper;

import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrganizationMemberWithLdapTest extends AbstractOrganizationTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    public void importTestRealms() {
        super.importTestRealms();

        // add an LDAP provider with a group mapper
        Map<String, String> cfg = ldapRule.getConfig();
        runOnServer.fetchString(LdapHelper.createLDAPProvider(cfg, true));
        runOnServer.run(LdapHelper.prepareGroupsLDAPTest());
    }

    @Test
    public void testLdapUserJoiningAndLeavingOrganization() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // ensure groups mapper is in LDAP_ONLY mode - we want to check that upon joining the org, the org group is NOT pushed to LDAP.
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            // check that the LDAP provider is working - i.e. users are available and groups have been properly synced.
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            assertThat(john, notNullValue());
            GroupModel testGroup = KeycloakModelUtils.findGroupByPath(session, appRealm, "/group1");
            assertThat(testGroup, notNullValue());
        });

        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation orgRepresentation = organization.toRepresentation();
        UserRepresentation ldapUser = managedRealm.admin().users().searchByUsername("johnkeycloak", true).get(0);
        RoleRepresentation defaultOrganizationRole = organization.roles().getDefault().toRepresentation();
        RoleRepresentation organizationRole = new RoleRepresentation("ldap-organization-role", null, false);
        String organizationRoleId;
        try (Response response = organization.roles().create(organizationRole)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            organizationRoleId = ApiUtil.getCreatedId(response);
        }
        OrganizationRoleResource organizationRoleResource = organization.roles().get(organizationRoleId);
        UserRepresentation roleMember = new UserRepresentation();
        roleMember.setId(ldapUser.getId());
        assertThrows(BadRequestException.class, () -> organizationRoleResource.addUserMembers(List.of(roleMember)));

        // make the LDAP user join the organization and check it was successful.
        try (Response response = organization.members().addMember(ldapUser.getId())) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        List<OrganizationRepresentation> orgMemberships = organization.members().member(ldapUser.getId()).getOrganizations(true);
        assertThat(orgMemberships, notNullValue());
        assertThat(orgMemberships, hasSize(1));
        assertThat(orgMemberships.get(0).getId(), equalTo(orgRepresentation.getId()));
        organizationRoleResource.addUserMembers(List.of(roleMember));
        assertThat(organizationRoleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                contains(ldapUser.getId()));

        RoleRepresentation inheritedOrganizationRole = new RoleRepresentation("ldap-group-role", null, false);
        try (Response response = organization.roles().create(inheritedOrganizationRole)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            inheritedOrganizationRole.setId(ApiUtil.getCreatedId(response));
        }
        GroupRepresentation inheritedRoleGroup = new GroupRepresentation();
        inheritedRoleGroup.setName("ldap-role-group");
        try (Response response = organization.groups().addTopLevelGroup(inheritedRoleGroup)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            inheritedRoleGroup.setId(ApiUtil.getCreatedId(response));
        }
        var inheritedRoleMappings = organization.groups().group(inheritedRoleGroup.getId()).roles();
        var inheritedRoles = List.of(inheritedOrganizationRole);
        assertThat(inheritedRoleMappings.getAvailableOrganizationRoleMappings().stream()
                .map(RoleRepresentation::getId).toList(), hasItem(inheritedOrganizationRole.getId()));
        inheritedRoleMappings.addOrganizationRoleMappings(inheritedRoles);
        assertFalse(inheritedRoleMappings.getAvailableOrganizationRoleMappings().stream()
                .anyMatch(role -> inheritedOrganizationRole.getId().equals(role.getId())));

        RoleRepresentation inheritedRealmRole = new RoleRepresentation("ldap-inherited-realm-role", null, false);
        managedRealm.admin().roles().create(inheritedRealmRole);
        inheritedRealmRole = managedRealm.admin().roles().get(inheritedRealmRole.getName()).toRepresentation();
        organization.groups().group(inheritedRoleGroup.getId()).roles().realmLevel().add(List.of(inheritedRealmRole));
        organization.groups().group(inheritedRoleGroup.getId()).addMember(ldapUser.getId());

        AtomicReference<String> inheritedGroupId = new AtomicReference<>(inheritedRoleGroup.getId());
        AtomicReference<String> inheritedRoleId = new AtomicReference<>(inheritedRealmRole.getId());
        AtomicReference<String> inheritedOrganizationRoleId = new AtomicReference<>(inheritedOrganizationRole.getId());
        AtomicReference<String> defaultRoleId = new AtomicReference<>(defaultOrganizationRole.getId());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            LDAPTestContext context = LDAPTestContext.init(session);
            RealmModel realm = context.getRealm();
            UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
            GroupModel group = realm.getGroupById(inheritedGroupId.get());
            RoleModel role = realm.getRoleById(inheritedRoleId.get());
            RoleModel defaultRole = realm.getRoleById(defaultRoleId.get());

            assertThat(john.getFederationLink(), notNullValue());
            assertFalse(john.getGroupsStream().anyMatch(candidate -> inheritedGroupId.get().equals(candidate.getId())));
            assertTrue(john.getRoleMappingsGroupsStream()
                    .anyMatch(candidate -> inheritedGroupId.get().equals(candidate.getId())));
            assertTrue(john.isMemberOf(group));
            assertTrue(john.hasRole(role));
            assertTrue(john.hasRole(realm.getRoleById(inheritedOrganizationRoleId.get())));
            assertTrue(RoleUtils.getDeepUserRoleMappings(john).contains(role));
            assertTrue(john.hasRole(defaultRole));
            assertFalse(john.hasDirectRole(defaultRole));
        });

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("ldap-organization-role-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = managedRealm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation clientRole = new RoleRepresentation("ldap-client-role", null, true);
        managedRealm.admin().clients().get(clientId).roles().create(clientRole);
        clientRole = managedRealm.admin().clients().get(clientId).roles().get(clientRole.getName()).toRepresentation();
        managedRealm.admin().users().get(ldapUser.getId()).roles().clientLevel(clientId).add(List.of(clientRole));
        assertThat(managedRealm.admin().users().get(ldapUser.getId()).roles().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(clientRole.getId()));
        assertThat(managedRealm.admin().users().get(ldapUser.getId()).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).filter(organizationRoleId::equals).toList(), hasSize(0));

        organizationRoleResource.deleteUserMembers(List.of(roleMember));
        assertThat(organizationRoleResource.getUserMembers(), hasSize(0));
        organizationRoleResource.addUserMembers(List.of(roleMember));

        // check that the org group was NOT pushed to LDAP as a result of joining the org.
        AtomicReference<String> orgId = new AtomicReference<>(orgRepresentation.getId());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            LDAPTestContext context = LDAPTestContext.init(session);
            assertThat(LDAPTestUtils.getLdapGroupByName(session, context.getRealm(), "groupsMapper", orgId.get()), is(nullValue()));
            assertThat(LDAPTestUtils.getLdapGroupByName(session, context.getRealm(), "groupsMapper", "ldap-role-group"), is(nullValue()));
        });

        // make the user leave the organization and check it was successful.
        try (Response response = organization.members().removeMember(ldapUser.getId())) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        List<MemberRepresentation> orgMembers = organization.members().list(-1, -1);
        assertThat(orgMembers, hasSize(0));
        assertThat(organizationRoleResource.getUserMembers(), hasSize(0));
        assertThat(managedRealm.admin().users().get(ldapUser.getId()).roles().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(clientRole.getId()));

        testingClient.server(TEST_REALM_NAME).run(session -> {
            LDAPTestContext context = LDAPTestContext.init(session);
            RealmModel realm = context.getRealm();
            UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
            assertFalse(john.hasRole(realm.getRoleById(defaultRoleId.get())));
            assertFalse(john.hasRole(realm.getRoleById(inheritedRoleId.get())));
            assertFalse(john.hasRole(realm.getRoleById(inheritedOrganizationRoleId.get())));
        });
    }

}
