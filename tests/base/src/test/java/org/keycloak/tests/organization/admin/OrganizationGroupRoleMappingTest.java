/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaRealmProvider;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils.OrganizationRoleClaims;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationGroupRoleMappingTest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String ORG_ID = "org-acme";
    private static final String ORG_ALIAS = "acme";

    @InjectRealm(config = OrganizationGroupRoleMappingRealmConfig.class, lifecycle = LifeCycle.METHOD)
    transient ManagedRealm realm;

    @InjectRunOnServer
    transient RunOnServerClient runOnServer;

    @BeforeEach
    public void createOrganization() {
        runOnServer.run(session -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            organizations.create(ORG_ID, "Acme", ORG_ALIAS);
        });
    }

    @Test
    public void testAssignAndRemoveOrganizationRoleOnOrganizationGroup() {
        runOnServer.run(session -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel group = organizations.createGroup(organization, "org-group", null);
            RoleModel orgRole = organization.addRole("admin");

            group.grantRole(orgRole);
            assertTrue(group.hasDirectRole(orgRole), "Group should have the organization role");
            assertThat(group.getRoleMappingsStream()
                    .filter(role -> role.isType(RoleModel.Type.ORGANIZATION))
                    .collect(Collectors.toSet()),
                    containsInAnyOrder(orgRole));

            group.deleteRoleMapping(orgRole);
            assertFalse(group.hasDirectRole(orgRole), "Group should no longer have the organization role");
        });
    }

    @Test
    public void testAssignAndRemoveRealmRoleFromOrganizationGroup() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel group = organizations.createGroup(organization, "org-group", null);
            RoleModel realmRole = session.roles().addRealmRole(realm, "business-role");

            group.grantRole(realmRole);
            assertTrue(group.hasDirectRole(realmRole), "Group should have the realm role");

            group.deleteRoleMapping(realmRole);
            assertFalse(group.hasDirectRole(realmRole), "Group should not have the realm role after removal");
        });
    }

    @Test
    public void testListOrganizationRolesAssignedToGroup() {
        runOnServer.run(session -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel group = organizations.createGroup(organization, "org-group", null);
            RoleModel adminRole = organization.addRole("admin");
            RoleModel memberRole = organization.addRole("member");

            group.grantRole(adminRole);
            group.grantRole(memberRole);

            Set<String> orgRoleNames = group.getRoleMappingsStream()
                    .filter(role -> role.isType(RoleModel.Type.ORGANIZATION))
                    .map(RoleModel::getName)
                    .collect(Collectors.toSet());

            assertThat(orgRoleNames, containsInAnyOrder("admin", "member"));
        });
    }

    @Test
    public void testOrganizationRolesFromGroupAreEffectiveAndIncludedInClaims() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel orgGroup = organizations.createGroup(organization, "dev-team", null);
            RoleModel developer = organization.addRole("developer");
            orgGroup.grantRole(developer);
            UserModel user = session.users().addUser(realm, "dev-user");
            organizations.addMember(organization, user);
            user.joinGroup(orgGroup);

            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            assertTrue(user.hasRole(developer));
            assertThat(claims.getOrganizationRoles(),
                    containsInAnyOrder(organization.getDefaultRole().getName(), developer.getName()));
            assertThat(claims.getRealmRoles(), empty());
        });
    }

    @Test
    public void testDirectAndGroupOrganizationRolesAreCombined() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);

            GroupModel orgGroup = organizations.createGroup(organization, "team", null);
            RoleModel teamRole = organization.addRole("team-role");
            orgGroup.grantRole(teamRole);
            UserModel user = session.users().addUser(realm, "power-user");
            organizations.addMember(organization, user);
            RoleModel manager = organization.addRole("manager");
            user.grantRole(manager);
            user.joinGroup(orgGroup);

            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            Set<String> roleNames = claims.getOrganizationRoles().stream()
                    .filter(name -> !name.equals(organization.getDefaultRole().getName()))
                    .collect(Collectors.toSet());

            assertTrue(user.hasRole(teamRole));
            assertThat(roleNames, containsInAnyOrder("manager", "team-role"));
            assertThat(claims.getRealmRoles(), empty());
        });
    }

    @Test
    public void testDefaultRoleCompositesAreInheritedThroughOrganizationMembership() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);

            RoleModel compositeRole = organization.addRole("admin-composite");
            RoleModel childRole1 = organization.addRole("read-users");
            RoleModel childRole2 = organization.addRole("write-users");
            compositeRole.addCompositeRole(childRole1);
            compositeRole.addCompositeRole(childRole2);

            organization.getDefaultRole().addCompositeRole(compositeRole);
            UserModel user = session.users().addUser(realm, "admin-user");
            organizations.addMember(organization, user);

            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            Set<String> roleNames = claims.getOrganizationRoles().stream()
                    .filter(name -> !name.equals(organization.getDefaultRole().getName()))
                    .collect(Collectors.toSet());

            assertThat(roleNames, containsInAnyOrder("admin-composite", "read-users", "write-users"));
        });
    }

    @Test
    public void testVisibleGroupCompositeRolesAreInheritedByMembers() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel group = organizations.createGroup(organization, "composite-team", null);
            RoleModel composite = organization.addRole("team-composite");
            RoleModel child = organization.addRole("team-composite-child");
            composite.addCompositeRole(child);
            group.grantRole(composite);

            UserModel user = session.users().addUser(realm, "composite-team-member");
            organizations.addMember(organization, user);
            user.joinGroup(group);

            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            assertTrue(user.hasRole(composite));
            assertTrue(user.hasRole(child));
            assertThat(claims.getOrganizationRoles(), containsInAnyOrder(
                    organization.getDefaultRole().getName(), composite.getName(), child.getName()));

            group.deleteRoleMapping(composite);
            assertFalse(user.hasRole(composite));
            assertFalse(user.hasRole(child));
            assertThat(OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session)
                    .getOrganizationRoles(), containsInAnyOrder(organization.getDefaultRole().getName()));
        });
    }

    @Test
    public void testOrganizationMemberOutsideGroupHasNoGroupRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel group = organizations.createGroup(organization, "other-team", null);
            RoleModel teamRole = organization.addRole("other-team-role");
            group.grantRole(teamRole);

            UserModel user = session.users().addUser(realm, "standalone-user");
            organizations.addMember(organization, user);

            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            Set<String> roleNames = claims.getOrganizationRoles().stream()
                    .filter(name -> !name.equals(organization.getDefaultRole().getName()))
                    .collect(Collectors.toSet());

            assertThat(roleNames, empty());
        });
    }

    @Test
    public void testOrganizationRoleInheritanceAcrossParentsAndBranches() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel grandparent = organizations.createGroup(organization, "division", null);
            GroupModel parentA = organizations.createGroup(organization, "team-a", grandparent);
            GroupModel parentB = organizations.createGroup(organization, "team-b", grandparent);
            GroupModel childA = organizations.createGroup(organization, "child-a", parentA);
            GroupModel childB = organizations.createGroup(organization, "child-b", parentB);
            GroupModel unjoinedSibling = organizations.createGroup(organization, "unjoined", grandparent);
            RoleModel shared = organization.addRole("division-role");
            RoleModel roleA = organization.addRole("team-a-role");
            RoleModel roleB = organization.addRole("team-b-role");
            RoleModel childRoleA = organization.addRole("child-a-role");
            RoleModel childRoleB = organization.addRole("child-b-role");
            RoleModel unjoinedRole = organization.addRole("unjoined-role");
            grandparent.grantRole(shared);
            parentA.grantRole(roleA);
            parentB.grantRole(roleB);
            childA.grantRole(childRoleA);
            childB.grantRole(childRoleB);
            unjoinedSibling.grantRole(unjoinedRole);

            UserModel user = session.users().addUser(realm, "multi-branch-user");
            organizations.addMember(organization, user);
            user.joinGroup(childA);
            user.joinGroup(childB);

            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            List<String> names = claims.getOrganizationRoles();
            assertThat(names, containsInAnyOrder(organization.getDefaultRole().getName(), shared.getName(),
                    roleA.getName(), roleB.getName(), childRoleA.getName(), childRoleB.getName()));
            assertEquals(1, Collections.frequency(names, shared.getName()));
            assertFalse(names.contains(unjoinedRole.getName()));
            assertTrue(user.hasRole(shared));
            assertTrue(user.hasRole(roleA));
            assertTrue(user.hasRole(roleB));
        });
    }

    @Test
    public void testIndependentSourcesKeepRoleUntilLastSourceIsRemoved() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel group = organizations.createGroup(organization, "source-team", null);
            RoleModel shared = organization.addRole("shared-role");
            UserModel user = session.users().addUser(realm, "multi-source-user");
            organizations.addMember(organization, user);
            user.joinGroup(group);
            user.grantRole(shared);
            group.grantRole(shared);
            organization.getDefaultRole().addCompositeRole(shared);

            assertEquals(1, Collections.frequency(
                    OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session)
                            .getOrganizationRoles(), shared.getName()));

            group.deleteRoleMapping(shared);
            user.deleteRoleMapping(shared);
            assertTrue(OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session)
                    .getOrganizationRoles().contains(shared.getName()));

            organization.getDefaultRole().removeCompositeRole(shared);
            assertFalse(OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session)
                    .getOrganizationRoles().contains(shared.getName()));
        });
    }

    @Test
    public void testOrganizationGroupRoleAndHierarchyIsolation() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = getOrganization(session);
            OrganizationModel other = organizations.create("org-other", "Other", "other");
            GroupModel acmeRoot = organizations.getOrganizationGroup(acme);
            GroupModel acmeParent = organizations.createGroup(acme, "acme-parent", null);
            GroupModel acmeChild = organizations.createGroup(acme, "acme-child", null);
            GroupModel otherGroup = organizations.createGroup(other, "other-group", null);
            GroupModel realmParent = session.groups().createGroup(realm, "realm-parent");
            GroupModel realmChild = session.groups().createGroup(realm, "realm-child");
            GroupModel orphan = session.groups().createGroup(realm, null, GroupModel.Type.ORGANIZATION,
                    "orphan-organization-group", null);
            RoleModel acmeRole = acme.addRole("acme-role");
            RoleModel otherRole = other.addRole("other-role");
            RoleModel realmRole = session.roles().addRealmRole(realm, "safe-realm-role");
            RealmModel otherRealm = session.realms().createRealm("organization-group-other-realm");
            RoleModel otherRealmRole = session.roles().addRealmRole(otherRealm, "other-realm-role");

            acmeChild.grantRole(acmeRole);
            acmeChild.grantRole(realmRole);
            acmeChild.setParent(acmeParent);
            realmChild.setParent(realmParent);
            String acmeParentId = acmeChild.getParentId();

            assertThrows(ModelException.class, () -> acmeChild.grantRole(otherRole));
            assertThrows(ModelException.class, () -> realmChild.grantRole(acmeRole));
            assertThrows(ModelException.class, () -> acmeChild.grantRole(otherRealmRole));
            assertThrows(ModelException.class, () -> directJpa(session).moveGroup(realm, acmeChild, otherGroup));
            assertEquals(acmeParentId, acmeChild.getParentId());
            assertThrows(ModelException.class, () -> acmeChild.setParent(otherGroup));
            assertThrows(ModelException.class, () -> acmeChild.setParent(realmParent));
            assertThrows(ModelException.class, () -> realmChild.setParent(acmeParent));
            assertThrows(ModelException.class, () -> acmeChild.setParent(null));
            assertThrows(ModelException.class, () -> directJpa(session).moveGroup(realm, acmeChild, null));
            assertThrows(ModelException.class, () -> acmeRoot.setParent(acmeChild));
            assertThrows(ModelException.class, () -> acmeParent.setParent(acmeChild));
            assertThrows(ModelException.class, () -> orphan.grantRole(realmRole));
            assertThrows(ModelException.class, () -> orphan.setParent(acmeParent));

            assertEquals(acmeParentId, acmeChild.getParentId());
            assertEquals(acmeRoot.getId(), acmeParent.getParentId());
            assertNull(acmeRoot.getParentId());
            assertTrue(acmeChild.hasDirectRole(acmeRole));
            assertTrue(acmeChild.hasDirectRole(realmRole));
            assertFalse(acmeChild.hasDirectRole(otherRole));
            directJpa(session).moveGroup(realm, acmeChild, acmeRoot);
            directJpa(session).moveGroup(realm, realmChild, null);
            directJpa(session).moveGroup(realm, realmChild, realmParent);
            assertEquals(acmeRoot.getId(), acmeChild.getParentId());
            assertEquals(realmParent.getId(), realmChild.getParentId());
            session.realms().removeRealm(otherRealm.getId());
        });
    }

    @Test
    public void testCachedOrganizationGroupAnchorsRejectLaterAdminComposites() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel child = organizations.createGroup(organization, "cached-anchor-child", null);
            RoleModel realmRole = session.roles().addRealmRole(realm, "cached-anchor-realm-role");
            ClientModel client = session.clients().addClient(realm, "cached-anchor-client");
            RoleModel clientRole = session.roles().addClientRole(client, "cached-anchor-client-role");

            organization.getDefaultRole().addCompositeRole(realmRole);
            child.grantRole(clientRole);
            return new String[] { organization.getDefaultRole().getId(), child.getId(), realmRole.getId(), clientRole.getId() };
        }, String[].class);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            assertTrue(realm.getRoleById(ids[0]).hasRole(realm.getRoleById(ids[2])));
            assertTrue(realm.getGroupById(ids[1]).hasDirectRole(realm.getRoleById(ids[3])));
            assertTrue(realm.getRoleById(ids[2]).getCompositesStream().findAny().isEmpty());
            assertTrue(realm.getRoleById(ids[3]).getCompositesStream().findAny().isEmpty());
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RoleModel role = realm.getRoleById(ids[2]);
            RoleModel admin = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID)
                    .getRole(AdminRoles.MANAGE_USERS);
            assertThrows(ModelException.class, () -> role.addCompositeRole(admin));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RoleModel role = realm.getRoleById(ids[3]);
            RoleModel admin = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID)
                    .getRole(AdminRoles.MANAGE_CLIENTS);
            assertThrows(ModelException.class, () -> role.addCompositeRole(admin));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            assertTrue(realm.getRoleById(ids[2]).getCompositesStream().findAny().isEmpty());
            assertTrue(realm.getRoleById(ids[3]).getCompositesStream().findAny().isEmpty());
            assertTrue(realm.getRoleById(ids[0]).hasRole(realm.getRoleById(ids[2])));
            assertTrue(realm.getGroupById(ids[1]).hasDirectRole(realm.getRoleById(ids[3])));
        });
    }

    private OrganizationModel getOrganization(KeycloakSession session) {
        return session.getProvider(OrganizationProvider.class).getById(ORG_ID);
    }

    private static JpaRealmProvider directJpa(KeycloakSession session) {
        return (JpaRealmProvider) session.getProvider(RealmProvider.class, JpaRealmProviderFactory.PROVIDER_ID);
    }

    public static final class OrganizationGroupRoleMappingRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
