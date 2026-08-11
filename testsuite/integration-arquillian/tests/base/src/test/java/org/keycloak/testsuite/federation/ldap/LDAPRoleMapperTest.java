/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.federation.ldap;

import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 *
 * @author rmartinc
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPRoleMapperTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        runOnServer.run(prepareRolesLDAPTest());
    }

    @Test
    public void test01RoleMapperRealmRoles() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // check users
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assertions.assertNotNull(john);
            assertThat(john.getRealmRoleMappingsStream().map(RoleModel::getName).collect(Collectors.toSet()), Matchers.containsInAnyOrder("group1", "group2"));
            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
            Assertions.assertNotNull(mary);
            assertThat(mary.getRealmRoleMappingsStream().map(RoleModel::getName).collect(Collectors.toSet()), Matchers.containsInAnyOrder("group1", "group2"));
            UserModel rob = session.users().getUserByUsername(appRealm, "robkeycloak");
            Assertions.assertNotNull(rob);
            assertThat(rob.getRealmRoleMappingsStream().map(RoleModel::getName).collect(Collectors.toSet()), Matchers.containsInAnyOrder("group1"));
            UserModel james = session.users().getUserByUsername(appRealm, "jameskeycloak");
            Assertions.assertNotNull(james);
            assertThat(james.getRealmRoleMappingsStream().collect(Collectors.toSet()), Matchers.empty());

            // check groups
            RoleModel group1 = appRealm.getRole("group1");
            Assertions.assertNotNull(group1);
            assertThat(session.users().getRoleMembersStream(appRealm, group1).map(UserModel::getUsername).collect(Collectors.toSet()),
                    Matchers.containsInAnyOrder("johnkeycloak", "marykeycloak", "robkeycloak"));
            RoleModel group2 = appRealm.getRole("group2");
            Assertions.assertNotNull(group2);
            assertThat(session.users().getRoleMembersStream(appRealm, group2).map(UserModel::getUsername).collect(Collectors.toSet()),
                    Matchers.containsInAnyOrder("johnkeycloak", "marykeycloak"));
            RoleModel group3 = appRealm.getRole("group3");
            Assertions.assertNotNull(group3);
            assertThat(session.users().getRoleMembersStream(appRealm, group3).collect(Collectors.toSet()), Matchers.empty());
        });
    }

    @Test
    public void test02RoleMapperClientRoles() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // create a client to set the roles in it
            ClientModel rolesClient = session.clients().addClient(appRealm, "role-mapper-client");

            try {
                ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "rolesMapper");
                LDAPTestUtils.updateConfigOptions(mapperModel,
                        RoleMapperConfig.USE_REALM_ROLES_MAPPING, "false",
                        RoleMapperConfig.CLIENT_ID, rolesClient.getClientId());
                appRealm.updateComponent(mapperModel);

                // synch to the client to create the roles at the client
                new RoleLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(appRealm);

                // check users
                UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
                Assertions.assertNotNull(john);
                assertThat(john.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.containsInAnyOrder("group1", "group2"));
                UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
                Assertions.assertNotNull(mary);
                assertThat(mary.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.containsInAnyOrder("group1", "group2"));
                UserModel rob = session.users().getUserByUsername(appRealm, "robkeycloak");
                Assertions.assertNotNull(rob);
                assertThat(rob.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.containsInAnyOrder("group1"));
                UserModel james = session.users().getUserByUsername(appRealm, "jameskeycloak");
                Assertions.assertNotNull(james);
                assertThat(james.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.empty());

                // check groups
                RoleModel group1 = rolesClient.getRole("group1");
                Assertions.assertNotNull(group1);
                assertThat(session.users().getRoleMembersStream(appRealm, group1).map(UserModel::getUsername).collect(Collectors.toSet()),
                        Matchers.containsInAnyOrder("johnkeycloak", "marykeycloak", "robkeycloak"));
                RoleModel group2 = rolesClient.getRole("group2");
                Assertions.assertNotNull(group2);
                assertThat(session.users().getRoleMembersStream(appRealm, group2).map(UserModel::getUsername).collect(Collectors.toSet()),
                        Matchers.containsInAnyOrder("johnkeycloak", "marykeycloak"));
                RoleModel group3 = rolesClient.getRole("group3");
                Assertions.assertNotNull(group3);
                assertThat(session.users().getRoleMembersStream(appRealm, group3).collect(Collectors.toSet()), Matchers.empty());

            } finally {
                appRealm.removeClient(rolesClient.getId());
            }
        });
    }

    @Test
    public void test03RoleMapperClientRoles() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // create a client to set the roles in it
            ClientModel rolesClient = session.clients().addClient(appRealm, "role-mapper-client");
            final String clientId = rolesClient.getClientId();

            try {
                ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "rolesMapper");
                LDAPTestUtils.updateConfigOptions(mapperModel,
                        RoleMapperConfig.USE_REALM_ROLES_MAPPING, "false",
                        RoleMapperConfig.CLIENT_ID, clientId);
                appRealm.updateComponent(mapperModel);

                rolesClient.setClientId(clientId + "-suffix");
                rolesClient.updateClient();

                // synch to the client to create the roles at the client
                new RoleLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(appRealm);

                // check users
                UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
                Assertions.assertNotNull(john);
                assertThat(john.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.empty());
                UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
                Assertions.assertNotNull(mary);
                assertThat(mary.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.empty());
                UserModel rob = session.users().getUserByUsername(appRealm, "robkeycloak");
                Assertions.assertNotNull(rob);
                assertThat(rob.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.empty());
                UserModel james = session.users().getUserByUsername(appRealm, "jameskeycloak");
                Assertions.assertNotNull(james);
                assertThat(james.getClientRoleMappingsStream(rolesClient).map(RoleModel::getName).collect(Collectors.toSet()), Matchers.empty());

                // check groups
                assertThat(rolesClient.getRole("group1"), nullValue());
                assertThat(rolesClient.getRole("group2"), nullValue());
                assertThat(rolesClient.getRole("group3"), nullValue());

            } finally {
                appRealm.removeClient(rolesClient.getId());
            }
        });
    }

    /**
     * Prepare groups LDAP tests. Creates some LDAP mappers as well as some built-in Groups and users in LDAP
     */
    public static RunOnServer prepareRolesLDAPTest() {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            // Add role mapper
            LDAPTestUtils.addOrUpdateRoleMapper(realm, ldapModel, LDAPGroupMapperMode.LDAP_ONLY);

            // Remove all LDAP groups and users
            LDAPTestUtils.removeAllLDAPGroups(session, realm, ldapModel, "rolesMapper");
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, realm);

            // Add some LDAP users for testing
            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");
            LDAPObject mary = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "5678");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, mary, "Password1");
            LDAPObject rob = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "robkeycloak", "Rob", "Brown", "rob@email.org", null, "8910");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, rob, "Password1");
            LDAPObject james = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "jameskeycloak", "James", "Brown", "james@email.org", null, "8910");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, james, "Password1");

            // Add some groups for testing
            LDAPObject group1 = LDAPTestUtils.createLDAPGroup("rolesMapper", session, realm, ldapModel, "group1");
            LDAPObject group2 = LDAPTestUtils.createLDAPGroup("rolesMapper", session, realm, ldapModel, "group2");
            LDAPObject group3 = LDAPTestUtils.createLDAPGroup("rolesMapper", session, realm, ldapModel, "group3");

            // add the users to the groups
            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, john);
            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, mary);
            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group1, rob);

            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group2, john);
            LDAPUtils.addMember(ldapFedProvider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group2, mary);

            // Sync LDAP groups to Keycloak DB roles
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ldapModel, "rolesMapper");
            new RoleLDAPStorageMapperFactory().create(session, mapperModel).syncDataFromFederationProviderToKeycloak(realm);
        };
    }
}
