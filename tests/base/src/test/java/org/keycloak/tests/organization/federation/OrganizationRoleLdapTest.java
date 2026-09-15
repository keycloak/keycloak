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

package org.keycloak.tests.organization.federation;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationRoleResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
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
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.runonserver.LdapHelper;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

@KeycloakIntegrationTest
public class OrganizationRoleLdapTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private EmbeddedLdap ldapServer;

    @BeforeEach
    public void configureLdap() throws Exception {
        realm.dirty();
        Properties properties = new Properties();
        properties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        properties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_HOST, "127.0.0.1");
        properties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, "0");
        properties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:organization/roles-ldap.ldif");
        ldapServer = new EmbeddedLdap(properties);
        ldapServer.init();
        ldapServer.start();
        Map<String, String> config = Map.of(
                LDAPConstants.CONNECTION_URL, "ldap://127.0.0.1:" + ldapServer.getListeningPort(),
                LDAPConstants.BASE_DN, "dc=keycloak,dc=org",
                LDAPConstants.USERS_DN, "ou=People,dc=keycloak,dc=org",
                LDAPConstants.BIND_DN, "uid=admin,ou=system",
                LDAPConstants.BIND_CREDENTIAL, "secret",
                LDAPConstants.CONNECTION_POOLING, "false",
                LDAPConstants.USERNAME_LDAP_ATTRIBUTE, "uid",
                LDAPConstants.RDN_LDAP_ATTRIBUTE, "uid",
                LDAPConstants.UUID_LDAP_ATTRIBUTE, "entryUUID",
                LDAPConstants.USER_OBJECT_CLASSES, "inetOrgPerson, organizationalPerson");
        runOnServer.fetchString(LdapHelper.createLDAPProvider(config, true));
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
            LDAPTestUtils.addOrUpdateGroupMapper(realm, model, LDAPGroupMapperMode.LDAP_ONLY, "description");
            LDAPObject user = LDAPTestUtils.addLDAPUser(provider, realm, "johnkeycloak", "John", "Doe", "john@example.org", null, "1234");
            LDAPObject group = LDAPTestUtils.createLDAPGroup(session, realm, model, "group1");
            LDAPUtils.addMember(provider, MembershipType.DN, LDAPConstants.MEMBER, "uid", group, user);
            ComponentModel mapper = LDAPTestUtils.getSubcomponentByName(realm, model, "groupsMapper");
            new GroupLDAPStorageMapperFactory().create(session, mapper).syncDataFromFederationProviderToKeycloak(realm);
        });
    }

    @AfterEach
    public void stopLdap() throws Exception {
        if (ldapServer != null) {
            ldapServer.stop();
        }
    }

    private static final class EmbeddedLdap extends LDAPEmbeddedServer {
        private EmbeddedLdap(Properties properties) {
            super(properties);
        }

        private int getListeningPort() {
            return ((InetSocketAddress) ldapServer.getTransports()[0].getAcceptor().getLocalAddress()).getPort();
        }
    }

    @Test
    public void shouldGrantAndRevokeOrganizationRolesForLdapMembers() {
        runOnServer.run(session -> {
            RealmModel appRealm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(appRealm);

            // ensure groups mapper is in LDAP_ONLY mode - we want to check that upon joining the org, the org group is NOT pushed to LDAP.
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "groupsMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, GroupMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString());
            appRealm.updateComponent(mapperModel);

            // check that the LDAP provider is working - i.e. users are available and groups have been properly synced.
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            assertThat(john, notNullValue());
            GroupModel testGroup = KeycloakModelUtils.findGroupByPath(session, appRealm, "/group1");
            assertThat(testGroup, notNullValue());
        });

        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation orgRepresentation = organization.toRepresentation();
        UserRepresentation ldapUser = realm.admin().users().searchByUsername("johnkeycloak", true).get(0);
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
        realm.admin().roles().create(inheritedRealmRole);
        inheritedRealmRole = realm.admin().roles().get(inheritedRealmRole.getName()).toRepresentation();
        organization.groups().group(inheritedRoleGroup.getId()).roles().realmLevel().add(List.of(inheritedRealmRole));
        organization.groups().group(inheritedRoleGroup.getId()).addMember(ldapUser.getId());

        AtomicReference<String> inheritedGroupId = new AtomicReference<>(inheritedRoleGroup.getId());
        AtomicReference<String> inheritedRoleId = new AtomicReference<>(inheritedRealmRole.getId());
        AtomicReference<String> inheritedOrganizationRoleId = new AtomicReference<>(inheritedOrganizationRole.getId());
        AtomicReference<String> defaultRoleId = new AtomicReference<>(defaultOrganizationRole.getId());
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
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
        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation clientRole = new RoleRepresentation("ldap-client-role", null, true);
        realm.admin().clients().get(clientId).roles().create(clientRole);
        clientRole = realm.admin().clients().get(clientId).roles().get(clientRole.getName()).toRepresentation();
        realm.admin().users().get(ldapUser.getId()).roles().clientLevel(clientId).add(List.of(clientRole));
        assertThat(realm.admin().users().get(ldapUser.getId()).roles().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(clientRole.getId()));
        assertThat(realm.admin().users().get(ldapUser.getId()).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).filter(organizationRoleId::equals).toList(), hasSize(0));

        organizationRoleResource.deleteUserMembers(List.of(roleMember));
        assertThat(organizationRoleResource.getUserMembers(), hasSize(0));
        organizationRoleResource.addUserMembers(List.of(roleMember));

        // check that the org group was NOT pushed to LDAP as a result of joining the org.
        AtomicReference<String> orgId = new AtomicReference<>(orgRepresentation.getId());
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            assertThat(LDAPTestUtils.getLdapGroupByName(session, realm, "groupsMapper", orgId.get()), is(nullValue()));
            assertThat(LDAPTestUtils.getLdapGroupByName(session, realm, "groupsMapper", "ldap-role-group"), is(nullValue()));
        });

        // make the user leave the organization and check it was successful.
        try (Response response = organization.members().removeMember(ldapUser.getId())) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        List<MemberRepresentation> orgMembers = organization.members().list(-1, -1);
        assertThat(orgMembers, hasSize(0));
        assertThat(organizationRoleResource.getUserMembers(), hasSize(0));
        assertThat(realm.admin().users().get(ldapUser.getId()).roles().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(clientRole.getId()));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
            assertFalse(john.hasRole(realm.getRoleById(defaultRoleId.get())));
            assertFalse(john.hasRole(realm.getRoleById(inheritedRoleId.get())));
            assertFalse(john.hasRole(realm.getRoleById(inheritedOrganizationRoleId.get())));
        });
    }

    @Test
    public void shouldResolveRolesThroughLdapGroupMembership() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
            UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
            GroupModel group = KeycloakModelUtils.findGroupByPath(session, realm, "/group1");
            assertTrue(john.getGroupsStream().anyMatch(group::equals));
            assertTrue(john.isMemberOf(group));
            RoleModel inheritedRole = realm.addRole("ldap-group-inherited-role");
            group.grantRole(inheritedRole);
            assertTrue(john.hasRole(inheritedRole));

            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, model, "groupsMapper");
            GroupLDAPStorageMapper mapper = LDAPTestUtils.getGroupMapper(mapperModel, provider, realm);
            LDAPObject ldapUser = provider.loadLDAPUserByUsername(realm, "johnkeycloak");
            UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, "johnkeycloak");
            UserModel delegate = mapper.new LDAPGroupMappingsUserDelegate(realm, localUser, ldapUser);
            assertTrue(delegate.isMemberOf(group));
            assertTrue(delegate.hasRole(inheritedRole));
        });
    }

    @Test
    public void shouldIncludeHardcodedGroupsOnceInRoleMappings() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserStorageProviderModel model = new UserStorageProviderModel(LDAPTestUtils.getLdapProviderModel(realm));
            // This scenario exercises the hardcoded mapper independently of LDAP_ONLY group mappings.
            realm.removeComponent(LDAPTestUtils.getSubcomponentByName(realm, model, "groupsMapper"));
            model.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            model.setImportEnabled(false);
            model.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.name());
            realm.updateComponent(model);
            GroupModel parent = realm.createGroup("parent_group");
            GroupModel group = realm.createGroup("hardcoded_group");
            parent.addChild(group);
            RoleModel role = realm.getClientByClientId("admin-cli").addRole("hardcoded-client-role");
            parent.grantRole(role);
            LDAPTestUtils.addOrUpdateHardcodedGroupMapper(realm, model);
        });
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
            GroupModel group = KeycloakModelUtils.findGroupByPath(session, realm, "/parent_group/hardcoded_group");
            assertThat(group, notNullValue());
            assertTrue(john.getGroupsStream().anyMatch(group::equals));
            assertEquals(1L, john.getRoleMappingsGroupsStream().filter(group::equals).count());
            assertTrue(john.isMemberOf(group));
            assertTrue(john.isMemberOf(group.getParent()));
            assertTrue(john.hasRole(realm.getClientByClientId("admin-cli").getRole("hardcoded-client-role")));
        });
    }

}
