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

package org.keycloak.tests.organization.authz.fgap;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationMembersResource;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ROLES_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Roles attached to an organization invitation need the same permissions as mapping them directly to the user.
 */
@KeycloakIntegrationTest
public class OrganizationInvitationRoleFgapTest {

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectMailServer
    MailServer mailServer;

    private ClientResource clientResource;
    private UserPolicyRepresentation policy;
    private String orgId;
    private RoleRepresentation role;

    @BeforeEach
    public void setup() {
        clientResource = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);

        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName("testorg");
        orgRep.setAlias("testorg");
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName("testorg.org");
        orgRep.addDomain(domain);
        try (Response response = realm.admin().organizations().create(orgRep)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgId = ApiUtil.getCreatedId(response);
        }

        realm.admin().roles().create(new RoleRepresentation("invitation-role", "", false));
        role = realm.admin().roles().get("invitation-role").toRepresentation();

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        policy = PermissionTestUtils.createUserPolicy(realm, clientResource, "Allow My Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        PermissionTestUtils.createPermission(clientResource, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW, MANAGE), policy);
        PermissionTestUtils.createPermission(clientResource, role.getId(), ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE), policy);
    }

    @Test
    public void testInviteExistingUserRequiresMapRolesOnUser() {
        String userId;
        try (Response response = realm.admin().users().create(UserBuilder.create().username("invitee").email("invitee@testorg.org").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        try (Response response = adminMembers().inviteExistingUser(userId, List.of(role.getId()))) {
            assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
        assertThat(realm.admin().organizations().get(orgId).invitations().list(), empty());

        PermissionTestUtils.createPermission(clientResource, userId, USERS_RESOURCE_TYPE, Set.of(MAP_ROLES), policy);

        try (Response response = adminMembers().inviteExistingUser(userId, List.of(role.getId()))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        assertThat(realm.admin().organizations().get(orgId).invitations().list().get(0).getRoles(), hasSize(1));
    }

    @Test
    public void testInviteNewUserRequiresMapRolesOnAllUsers() {
        try (Response response = adminMembers().inviteUser("new@testorg.org", "New", "User", null, List.of(role.getId()))) {
            assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
        assertThat(realm.admin().organizations().get(orgId).invitations().list(), empty());

        PermissionTestUtils.createAllPermission(clientResource, USERS_RESOURCE_TYPE, policy, Set.of(MAP_ROLES));

        try (Response response = adminMembers().inviteUser("new@testorg.org", "New", "User", null, List.of(role.getId()))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        assertThat(realm.admin().organizations().get(orgId).invitations().list().get(0).getRoles(), hasSize(1));
    }

    private OrganizationMembersResource adminMembers() {
        return realmAdminClient.realm(realm.getName()).organizations().get(orgId).members();
    }
}
