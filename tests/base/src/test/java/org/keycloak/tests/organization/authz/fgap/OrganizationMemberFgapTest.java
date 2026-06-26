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

import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression tests for GitHub issue #50136: StackOverflowError when FGAP v2 and Organizations are both enabled
 * and a non-superadmin user performs operations on organization members via the admin API.
 */
@KeycloakIntegrationTest
public class OrganizationMemberFgapTest {

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private ClientResource clientResource;
    private String orgId;
    private String memberUserId;

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
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            orgId = ApiUtil.getCreatedId(response);
        }

        UserRepresentation member = UserBuilder.create()
                .username("orgmember")
                .email("orgmember@testorg.org")
                .password("password")
                .build();
        try (Response response = realm.admin().users().create(member)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            memberUserId = ApiUtil.getCreatedId(response);
        }
        try (Response response = realm.admin().organizations().get(orgId).members().addMember(memberUserId)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGetOrganizationMemberById() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = PermissionTestUtils.createUserPolicy(
                realm, clientResource, "Allow Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        PermissionTestUtils.createAllPermission(clientResource, USERS_RESOURCE_TYPE, policy, Set.of(VIEW));

        UserRepresentation user = realmAdminClient.realm(realm.getName()).users().get(memberUserId).toRepresentation();
        assertNotNull(user);
        assertEquals("orgmember", user.getUsername());
    }

    @Test
    public void testResetPasswordOfOrganizationMember() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = PermissionTestUtils.createUserPolicy(
                realm, clientResource, "Allow Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        PermissionTestUtils.createAllPermission(clientResource, USERS_RESOURCE_TYPE, policy, Set.of(VIEW, MANAGE));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("newpassword");

        realmAdminClient.realm(realm.getName()).users().get(memberUserId).resetPassword(credential);

        UserRepresentation user = realmAdminClient.realm(realm.getName()).users().get(memberUserId).toRepresentation();
        assertNotNull(user);
    }
}
