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
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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

    @InjectClient(attachTo = Constants.ADMIN_PERMISSIONS_CLIENT_ID)
    ManagedClient client;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private String orgId;
    private String memberUserId;

    @BeforeEach
    public void setup() {
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

        UserRepresentation member = UserConfigBuilder.create()
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
        UserPolicyRepresentation policy = createUserPolicy(
                realm, client, "Allow Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        createAllPermission(client, USERS_RESOURCE_TYPE, policy, Set.of(VIEW));

        UserRepresentation user = realmAdminClient.realm(realm.getName()).users().get(memberUserId).toRepresentation();
        assertNotNull(user);
        assertEquals("orgmember", user.getUsername());
    }

    @Test
    public void testResetPasswordOfOrganizationMember() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(
                realm, client, "Allow Admin " + KeycloakModelUtils.generateId(), myadmin.getId());
        createAllPermission(client, USERS_RESOURCE_TYPE, policy, Set.of(VIEW, MANAGE));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("newpassword");

        realmAdminClient.realm(realm.getName()).users().get(memberUserId).resetPassword(credential);

        UserRepresentation user = realmAdminClient.realm(realm.getName()).users().get(memberUserId).toRepresentation();
        assertNotNull(user);
    }

    private UserPolicyRepresentation createUserPolicy(ManagedRealm realm, ManagedClient client, String name, String... userIds) {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName(name);
        for (String userId : userIds) {
            policy.addUser(userId);
        }
        try (Response response = client.admin().authorization().policies().user().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> {
                UserPolicyRepresentation userPolicy = r.clients().get(client.getId()).authorization().policies().user().findByName(name);
                if (userPolicy != null) {
                    r.clients().get(client.getId()).authorization().policies().user().findById(userPolicy.getId()).remove();
                }
            });
        }
        return policy;
    }

    private ScopePermissionRepresentation createAllPermission(ManagedClient client, String resourceType, AbstractPolicyRepresentation policy, Set<String> scopes) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(resourceType);
        permission.setScopes(scopes);
        permission.addPolicy(policy.getName());

        ScopePermissionsResource scopePermissions = client.admin().authorization().permissions().scope();
        try (Response response = scopePermissions.create(permission)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }
        return permission;
    }

    public static class OrganizationFgapConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser("myadmin")
                    .name("My", "Admin")
                    .email("myadmin@localhost")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.QUERY_USERS,
                            AdminRoles.QUERY_GROUPS,
                            AdminRoles.QUERY_CLIENTS);
            realm.addClient("myclient")
                    .secret("mysecret")
                    .directAccessGrantsEnabled(true);
            return realm.adminPermissionsEnabled(true)
                    .organizationsEnabled(true);
        }
    }
}
