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

import java.util.Properties;
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.hamcrest.Matchers;
import org.junit.Assume;
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
    public static LDAPRule ldapRule = new LDAPRule() {
        @Override
        protected LDAPEmbeddedServer createServer() {
            // Retain LDAPRule's normal embedded-server settings and expose its search limit to this test.
            super.createServer();
            return new SizeLimitedLDAPEmbeddedServer(defaultProperties);
        }
    };

    private static class SizeLimitedLDAPEmbeddedServer extends LDAPEmbeddedServer {

        SizeLimitedLDAPEmbeddedServer(Properties properties) {
            super(properties);
        }

        long getSearchSizeLimit() {
            return ldapServer.getMaxSizeLimit();
        }

        void setSearchSizeLimit(long limit) {
            ldapServer.setMaxSizeLimit(limit);
        }

        boolean isAccessControlEnabled() {
            return directoryService.isAccessControlEnabled();
        }

        void setAccessControlEnabled(boolean enabled) {
            directoryService.setAccessControlEnabled(enabled);
        }
    }

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

    @Test
    public void test04DropOnlyRolesOwnedByThisRealmMapper() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel,
                    RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true",
                    RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "false");
            realm.updateComponent(mapperModel);
            RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
            RoleModel localRole = realm.addRole("local-role-remains");
            RoleModel otherMapperRole = realm.addRole("other-mapper-role-remains");
            otherMapperRole.setSingleAttribute("kc.ldap.role.provider.id", ctx.getLdapModel().getId());
            otherMapperRole.setSingleAttribute("kc.ldap.role.mapper.id", "a-different-mapper");

            LDAPObject ldapRole = mapper.createLDAPRole("deleted-ldap-realm-role");
            try {
                mapper.syncDataFromFederationProviderToKeycloak(realm);
                RoleModel managedRole = realm.getRole("deleted-ldap-realm-role");
                Assertions.assertNotNull(managedRole);
                Assertions.assertEquals(mapperModel.getId(), managedRole.getFirstAttribute("kc.ldap.role.mapper.id"));

                ctx.getLdapProvider().getLdapIdentityStore().remove(ldapRole);
                mapper.syncDataFromFederationProviderToKeycloak(realm);
                Assertions.assertNotNull(realm.getRole("deleted-ldap-realm-role")); // default off

                LDAPTestUtils.updateConfigOptions(mapperModel, RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "true");
                realm.updateComponent(mapperModel);
                mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
                Assertions.assertEquals(1, mapper.syncDataFromFederationProviderToKeycloak(realm).getRemoved());
                Assertions.assertNull(realm.getRole("deleted-ldap-realm-role"));
                Assertions.assertNotNull(realm.getRole(localRole.getName()));
                Assertions.assertNotNull(realm.getRole(otherMapperRole.getName()));
            } finally {
                try (LDAPQuery query = mapper.createRoleQuery(false)) {
                    query.getResultList().stream()
                            .filter(role -> "deleted-ldap-realm-role".equals(role.getAttributeAsString("cn")))
                            .forEach(role -> ctx.getLdapProvider().getLdapIdentityStore().remove(role));
                }
                RoleModel remaining = realm.getRole("deleted-ldap-realm-role");
                if (remaining != null) {
                    realm.removeRole(remaining);
                }
                realm.removeRole(localRole);
                realm.removeRole(otherMapperRole);
                LDAPTestUtils.updateConfigOptions(mapperModel, RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "false");
                realm.updateComponent(mapperModel);
            }
        });
    }

    @Test
    public void test05DropClientRoleWithoutAdoptingExistingRole() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ClientModel client = session.clients().addClient(realm, "role-cleanup-client");
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
            RoleModel localRole = client.addRole("local-role-collision");

            try {
                LDAPTestUtils.updateConfigOptions(mapperModel,
                        RoleMapperConfig.USE_REALM_ROLES_MAPPING, "false",
                        RoleMapperConfig.CLIENT_ID, client.getClientId(),
                        RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "true");
                realm.updateComponent(mapperModel);
                RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
                LDAPObject ldapRole = mapper.createLDAPRole("deleted-ldap-client-role");
                LDAPObject collidingRole = mapper.createLDAPRole("local-role-collision");
                try {
                    mapper.syncDataFromFederationProviderToKeycloak(realm);
                    Assertions.assertNull(localRole.getFirstAttribute("kc.ldap.role.mapper.id"));
                    Assertions.assertNotNull(client.getRole("deleted-ldap-client-role"));
                    ctx.getLdapProvider().getLdapIdentityStore().remove(ldapRole);
                    ctx.getLdapProvider().getLdapIdentityStore().remove(collidingRole);
                    Assertions.assertEquals(1, mapper.syncDataFromFederationProviderToKeycloak(realm).getRemoved());
                    Assertions.assertNull(client.getRole("deleted-ldap-client-role"));
                    Assertions.assertNotNull(client.getRole("local-role-collision"));
                } finally {
                    try (LDAPQuery query = mapper.createRoleQuery(false)) {
                        query.getResultList().stream()
                                .filter(role -> "deleted-ldap-client-role".equals(role.getAttributeAsString("cn"))
                                        || "local-role-collision".equals(role.getAttributeAsString("cn")))
                                .forEach(role -> ctx.getLdapProvider().getLdapIdentityStore().remove(role));
                    }
                }
            } finally {
                realm.removeClient(client.getId());
                LDAPTestUtils.updateConfigOptions(mapperModel,
                        RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true",
                        RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "false");
                realm.updateComponent(mapperModel);
            }
        });
    }

    @Test
    public void test06IncompleteRoleSearchDoesNotDeleteManagedRoles() {
        Assume.assumeTrue("Requires the embedded LDAP server", ldapRule.isEmbeddedServer());
        SizeLimitedLDAPEmbeddedServer server = (SizeLimitedLDAPEmbeddedServer) ldapRule.getLdapEmbeddedServer();
        long originalLimit = server.getSearchSizeLimit();
        boolean originalAccessControl = server.isAccessControlEnabled();
        String originalBindDn = ldapRule.getConfig().get(LDAPConstants.BIND_DN);

        try {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel realm = ctx.getRealm();
                ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
                LDAPTestUtils.updateConfigOptions(mapperModel,
                        RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true",
                        RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "true");
                realm.updateComponent(mapperModel);
                RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
                mapper.createLDAPRole("size-limit-preserved-role");
                mapper.syncDataFromFederationProviderToKeycloak(realm);
                RoleModel managedRole = realm.getRole("size-limit-preserved-role");
                Assertions.assertNotNull(managedRole);
                Assertions.assertEquals(mapperModel.getId(), managedRole.getFirstAttribute("kc.ldap.role.mapper.id"));
            });

            // ApacheDS exempts its administrator from server size limits. Bind as the fixture's
            // non-administrator account with access control disabled to exercise an incomplete search.
            server.setAccessControlEnabled(false);
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                ctx.getLdapModel().getConfig().putSingle(LDAPConstants.BIND_DN, "uid=keycloak-admin,dc=keycloak,dc=org");
                ctx.getLdapModel().getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, "secret");
                ctx.getRealm().updateComponent(ctx.getLdapModel());
            });
            server.setSearchSizeLimit(1);
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel realm = ctx.getRealm();
                ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
                RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);

                Assertions.assertThrows(ModelException.class, () -> mapper.syncDataFromFederationProviderToKeycloak(realm));
                Assertions.assertNotNull(realm.getRole("size-limit-preserved-role"));
                Assertions.assertNotNull(realm.getRole("group1"));
                Assertions.assertNotNull(realm.getRole("group2"));
                Assertions.assertNotNull(realm.getRole("group3"));
            });
        } finally {
            server.setSearchSizeLimit(originalLimit);
            try {
                testingClient.server().run(session -> {
                    LDAPTestContext ctx = LDAPTestContext.init(session);
                    RealmModel realm = ctx.getRealm();
                    ctx.getLdapModel().getConfig().putSingle(LDAPConstants.BIND_DN, originalBindDn);
                    realm.updateComponent(ctx.getLdapModel());
                    ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
                    LDAPTestUtils.updateConfigOptions(mapperModel, RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "false");
                    realm.updateComponent(mapperModel);
                    RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
                    try (LDAPQuery query = mapper.createRoleQuery(false)) {
                        query.getResultList().stream()
                                .filter(role -> "size-limit-preserved-role".equals(role.getAttributeAsString("cn")))
                                .forEach(role -> ctx.getLdapProvider().getLdapIdentityStore().remove(role));
                    }
                    RoleModel managedRole = realm.getRole("size-limit-preserved-role");
                    if (managedRole != null) {
                        realm.removeRole(managedRole);
                    }
                });
            } finally {
                server.setAccessControlEnabled(originalAccessControl);
            }
        }
    }

    @Test
    public void test07ImportCreatedRoleIsOwnedAndRemoved() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "rolesMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel,
                    RoleMapperConfig.MODE, LDAPGroupMapperMode.IMPORT.toString(),
                    RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "true");
            ctx.getRealm().updateComponent(mapperModel);
        });

        try {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel realm = ctx.getRealm();
                ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
                RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
                LDAPObject ldapRole = mapper.createLDAPRole("import-owned-role");
                LDAPObject ldapUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), realm,
                        "role-import-owner", "Role", "Import", "role-import-owner@example.org", null, "1234");
                mapper.addRoleMappingInLDAP("import-owned-role", ldapUser);
                Assertions.assertNull(realm.getRole("import-owned-role"));

                UserModel importedUser = session.users().getUserByUsername(realm, "role-import-owner");
                Assertions.assertNotNull(importedUser);
                RoleModel importedRole = realm.getRole("import-owned-role");
                Assertions.assertNotNull(importedRole);
                Assertions.assertTrue(importedUser.hasRole(importedRole));
                Assertions.assertEquals(ctx.getLdapModel().getId(), importedRole.getFirstAttribute("kc.ldap.role.provider.id"));
                Assertions.assertEquals(mapperModel.getId(), importedRole.getFirstAttribute("kc.ldap.role.mapper.id"));

                ctx.getLdapProvider().getLdapIdentityStore().remove(ldapRole);
                Assertions.assertEquals(1, mapper.syncDataFromFederationProviderToKeycloak(realm).getRemoved());
                Assertions.assertNull(realm.getRole("import-owned-role"));
            });
        } finally {
            testingClient.server().run(session -> {
                LDAPTestContext ctx = LDAPTestContext.init(session);
                RealmModel realm = ctx.getRealm();
                ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
                LDAPTestUtils.updateConfigOptions(mapperModel,
                        RoleMapperConfig.MODE, LDAPGroupMapperMode.LDAP_ONLY.toString(),
                        RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "false");
                realm.updateComponent(mapperModel);
                RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
                LDAPObject ldapRole = mapper.loadLDAPRoleByName("import-owned-role");
                if (ldapRole != null) {
                    ctx.getLdapProvider().getLdapIdentityStore().remove(ldapRole);
                }
                RoleModel role = realm.getRole("import-owned-role");
                if (role != null) {
                    realm.removeRole(role);
                }
                UserModel user = session.users().getUserByUsername(realm, "role-import-owner");
                if (user != null) {
                    session.users().removeUser(realm, user);
                }
                LDAPObject remainingUser = ctx.getLdapProvider().loadLDAPUserByUsername(realm, "role-import-owner");
                if (remainingUser != null) {
                    ctx.getLdapProvider().getLdapIdentityStore().remove(remainingUser);
                }
            });
        }
    }

    @Test
    public void test08LazyRoleMappingIsOwnedAndRemoved() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            ComponentModel mapperModel = LDAPTestUtils.getSubcomponentByName(realm, ctx.getLdapModel(), "rolesMapper");
            LDAPTestUtils.updateConfigOptions(mapperModel, RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "true");
            realm.updateComponent(mapperModel);
            RoleLDAPStorageMapper mapper = (RoleLDAPStorageMapper) new RoleLDAPStorageMapperFactory().create(session, mapperModel);
            try {
                LDAPObject ldapRole = mapper.createLDAPRole("lazy-owned-role");
                LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(realm, "johnkeycloak");
                mapper.addRoleMappingInLDAP("lazy-owned-role", ldapUser);
                Assertions.assertNull(realm.getRole("lazy-owned-role"));

                UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
                Assertions.assertNotNull(john);
                Assertions.assertTrue(john.getRealmRoleMappingsStream()
                        .anyMatch(role -> "lazy-owned-role".equals(role.getName())));
                RoleModel lazyRole = realm.getRole("lazy-owned-role");
                Assertions.assertNotNull(lazyRole);
                Assertions.assertEquals(ctx.getLdapModel().getId(), lazyRole.getFirstAttribute("kc.ldap.role.provider.id"));
                Assertions.assertEquals(mapperModel.getId(), lazyRole.getFirstAttribute("kc.ldap.role.mapper.id"));

                ctx.getLdapProvider().getLdapIdentityStore().remove(ldapRole);
                Assertions.assertEquals(1, mapper.syncDataFromFederationProviderToKeycloak(realm).getRemoved());
                Assertions.assertNull(realm.getRole("lazy-owned-role"));
            } finally {
                LDAPTestUtils.updateConfigOptions(mapperModel, RoleMapperConfig.DROP_NON_EXISTING_ROLES_DURING_SYNC, "false");
                realm.updateComponent(mapperModel);
                LDAPObject ldapRole = mapper.loadLDAPRoleByName("lazy-owned-role");
                if (ldapRole != null) {
                    ctx.getLdapProvider().getLdapIdentityStore().remove(ldapRole);
                }
                RoleModel role = realm.getRole("lazy-owned-role");
                if (role != null) {
                    realm.removeRole(role);
                }
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
