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
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    public void testAssignOrganizationRoleToOrganizationGroup() {
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
                    hasSize(1));
        });
    }

    @Test
    public void testRemoveOrganizationRoleFromOrganizationGroup() {
        runOnServer.run(session -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            GroupModel group = organizations.createGroup(organization, "org-group", null);
            RoleModel orgRole = organization.addRole("admin");

            group.grantRole(orgRole);
            assertTrue(group.hasDirectRole(orgRole), "Group should have the organization role");

            group.deleteRoleMapping(orgRole);
            assertFalse(group.hasDirectRole(orgRole), "Group should not have the organization role after removal");
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
    public void testOrganizationRolesFromGroupInheritedByUserInToken() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            
            // Create organization group with roles
            GroupModel orgGroup = organizations.createGroup(organization, "dev-team", null);
            RoleModel adminRole = organization.addRole("admin");
            RoleModel devRole = organization.addRole("developer");
            
            orgGroup.grantRole(adminRole);
            orgGroup.grantRole(devRole);
            
            // Create user and add to organization
            UserModel user = session.users().addUser(realm, "dev-user");
            organizations.addMember(organization, user);
            
            // Add user to group
            user.joinGroup(orgGroup);
            
            // Verify org roles from group are resolved in token claims
            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            Set<String> roleNames = claims.getOrganizationRoles().stream()
                    .filter(name -> !name.equals(organization.getDefaultRole().getName()))
                    .collect(Collectors.toSet());
            
            assertThat(roleNames, containsInAnyOrder("admin", "developer"));
        });
    }

    @Test
    public void testDirectAndGroupOrganizationRolesCombined() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            
            // Create organization group with role
            GroupModel orgGroup = organizations.createGroup(organization, "team", null);
            RoleModel teamLead = organization.addRole("team-lead");
            orgGroup.grantRole(teamLead);
            
            // Create user with direct role and group membership
            UserModel user = session.users().addUser(realm, "power-user");
            organizations.addMember(organization, user);
            
            RoleModel manager = organization.addRole("manager");
            user.grantRole(manager);
            user.joinGroup(orgGroup);
            
            // Verify both direct and group roles are included
            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            Set<String> roleNames = claims.getOrganizationRoles().stream()
                    .filter(name -> !name.equals(organization.getDefaultRole().getName()))
                    .collect(Collectors.toSet());
            
            assertThat(roleNames, containsInAnyOrder("manager", "team-lead"));
        });
    }

    @Test
    public void testCompositeOrganizationRolesInGroupInheritedByUser() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            
            // Create composite role
            RoleModel compositeRole = organization.addRole("admin-composite");
            RoleModel childRole1 = organization.addRole("read-users");
            RoleModel childRole2 = organization.addRole("write-users");
            compositeRole.addCompositeRole(childRole1);
            compositeRole.addCompositeRole(childRole2);
            
            // Assign composite to group
            GroupModel orgGroup = organizations.createGroup(organization, "admins", null);
            orgGroup.grantRole(compositeRole);
            
            // Add user to group
            UserModel user = session.users().addUser(realm, "admin-user");
            organizations.addMember(organization, user);
            user.joinGroup(orgGroup);
            
            // Verify composite roles are expanded
            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            Set<String> roleNames = claims.getOrganizationRoles().stream()
                    .filter(name -> !name.equals(organization.getDefaultRole().getName()))
                    .collect(Collectors.toSet());
            
            assertThat(roleNames, containsInAnyOrder("admin-composite", "read-users", "write-users"));
        });
    }

    @Test
    public void testUserWithoutGroupMembershipHasNoGroupRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session);
            
            // Create organization group with roles
            GroupModel orgGroup = organizations.createGroup(organization, "team", null);
            RoleModel teamRole = organization.addRole("team-member");
            orgGroup.grantRole(teamRole);
            
            // Create user without group membership
            UserModel user = session.users().addUser(realm, "standalone-user");
            organizations.addMember(organization, user);
            
            // Verify no group roles are resolved
            OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(organization, user, session);
            Set<String> roleNames = claims.getOrganizationRoles().stream()
                    .filter(name -> !name.equals(organization.getDefaultRole().getName()))
                    .collect(Collectors.toSet());
            
            assertThat(roleNames, empty());
        });
    }

    private OrganizationModel getOrganization(KeycloakSession session) {
        return session.getProvider(OrganizationProvider.class).getById(ORG_ID);
    }

    public static final class OrganizationGroupRoleMappingRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
