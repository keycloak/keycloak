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
package org.keycloak.tests.organization.authz;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@KeycloakIntegrationTest
public class OrganizationGroupMapperPermissionsTest extends AbstractOrganizationTest {

    @InjectRealm(config = OrgGroupMapperPermissionsRealmConfig.class)
    ManagedRealm realm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testOrgGroupMapperRequiresManageOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());
        String idpAlias = organizationName + "-identity-provider";

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("test-perm-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        IdentityProviderMapperRepresentation mapper = createOrgGroupMapper(idpAlias, groupPath, orgRep.getId());

        // manage-idps-only has manage-identity-providers but NOT manage-organizations — should be forbidden
        try (Keycloak manageIdpsOnly = adminClientFactory.create()
                .realm(realm.getName()).username("manage-idps-only").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            RealmResource resource = manageIdpsOnly.realm(realm.getName());
            try (Response response = resource.identityProviders().get(idpAlias).addMapper(mapper)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }

        // manage-idps-and-orgs has both manage-identity-providers AND manage-organizations — should succeed
        try (Keycloak manageIdpsAndOrgs = adminClientFactory.create()
                .realm(realm.getName()).username("manage-idps-and-orgs").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            RealmResource resource = manageIdpsAndOrgs.realm(realm.getName());
            try (Response response = resource.identityProviders().get(idpAlias).addMapper(mapper)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
        }
    }

    @Test
    public void testOrgGroupMapperUpdateRequiresManageOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());
        String idpAlias = organizationName + "-identity-provider";

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("test-update-perm-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        // create mapper as admin
        IdentityProviderMapperRepresentation mapper = createOrgGroupMapper(idpAlias, groupPath, orgRep.getId());
        String mapperId;
        try (Response response = realm.admin().identityProviders().get(idpAlias).addMapper(mapper)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            mapperId = ApiUtil.getCreatedId(response);
        }

        IdentityProviderMapperRepresentation createdMapper = realm.admin().identityProviders()
                .get(idpAlias).getMapperById(mapperId);

        // manage-idps-only cannot update org group mapper
        try (Keycloak manageIdpsOnly = adminClientFactory.create()
                .realm(realm.getName()).username("manage-idps-only").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            RealmResource resource = manageIdpsOnly.realm(realm.getName());
            try {
                resource.identityProviders().get(idpAlias).update(mapperId, createdMapper);
                org.junit.jupiter.api.Assertions.fail("Expected ForbiddenException");
            } catch (jakarta.ws.rs.ForbiddenException expected) {
            }
        }

        // manage-idps-and-orgs can update org group mapper
        try (Keycloak manageIdpsAndOrgs = adminClientFactory.create()
                .realm(realm.getName()).username("manage-idps-and-orgs").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            RealmResource resource = manageIdpsAndOrgs.realm(realm.getName());
            resource.identityProviders().get(idpAlias).update(mapperId, createdMapper);
        }
    }

    @Test
    public void testOrgGroupMapperDeleteRequiresManageOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());
        String idpAlias = organizationName + "-identity-provider";

        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName("test-delete-perm-group");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        String groupPath = orgResource.groups().group(groupId).toRepresentation(false).getPath();

        // create mapper as admin
        IdentityProviderMapperRepresentation mapper = createOrgGroupMapper(idpAlias, groupPath, orgRep.getId());
        String mapperId;
        try (Response response = realm.admin().identityProviders().get(idpAlias).addMapper(mapper)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            mapperId = ApiUtil.getCreatedId(response);
        }

        // manage-idps-only cannot delete org group mapper
        try (Keycloak manageIdpsOnly = adminClientFactory.create()
                .realm(realm.getName()).username("manage-idps-only").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            RealmResource resource = manageIdpsOnly.realm(realm.getName());
            try {
                resource.identityProviders().get(idpAlias).delete(mapperId);
                org.junit.jupiter.api.Assertions.fail("Expected ForbiddenException");
            } catch (jakarta.ws.rs.ForbiddenException expected) {
            }
        }

        // manage-idps-and-orgs can delete org group mapper
        try (Keycloak manageIdpsAndOrgs = adminClientFactory.create()
                .realm(realm.getName()).username("manage-idps-and-orgs").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            RealmResource resource = manageIdpsAndOrgs.realm(realm.getName());
            resource.identityProviders().get(idpAlias).delete(mapperId);
        }
    }

    @Test
    public void testRealmGroupMapperDoesNotRequireManageOrganization() {
        createOrganization();
        String idpAlias = organizationName + "-identity-provider";

        // create a realm-level group
        GroupRepresentation realmGroup = new GroupRepresentation();
        realmGroup.setName("test-realm-group");
        String groupId;
        try (Response response = realm.admin().groups().add(realmGroup)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.groups().group(groupId).remove());
        }

        String groupPath = realm.admin().groups().group(groupId).toRepresentation().getPath();

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("realm-group-mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-group-idp-mapper");
        mapper.setIdentityProviderAlias(idpAlias);
        mapper.getConfig().put(IdentityProviderMapperModel.SYNC_MODE, "FORCE");
        mapper.getConfig().put(ConfigConstants.GROUP, groupPath);

        // manage-idps-only should succeed for realm group mapper (no org permission needed)
        try (Keycloak manageIdpsOnly = adminClientFactory.create()
                .realm(realm.getName()).username("manage-idps-only").password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            RealmResource resource = manageIdpsOnly.realm(realm.getName());
            try (Response response = resource.identityProviders().get(idpAlias).addMapper(mapper)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
        }
    }

    private IdentityProviderMapperRepresentation createOrgGroupMapper(String idpAlias, String groupPath, String orgId) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("org-group-mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-group-idp-mapper");
        mapper.setIdentityProviderAlias(idpAlias);
        mapper.getConfig().put(IdentityProviderMapperModel.SYNC_MODE, "FORCE");
        mapper.getConfig().put(ConfigConstants.GROUP, groupPath);
        mapper.getConfig().put(ConfigConstants.GROUP_TYPE, GroupModel.Type.ORGANIZATION.name());
        mapper.getConfig().put(ConfigConstants.ORGANIZATION_ID, orgId);
        return mapper;
    }

    public static class OrgGroupMapperPermissionsRealmConfig extends OrganizationRealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            super.configure(realm);
            realm.users(UserBuilder.create("manage-idps-only")
                    .password("password")
                    .name("manage", "idps-only")
                    .email("manage-idps-only@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_ORGANIZATIONS,
                            AdminRoles.MANAGE_IDENTITY_PROVIDERS));
            realm.users(UserBuilder.create("manage-idps-and-orgs")
                    .password("password")
                    .name("manage", "idps-and-orgs")
                    .email("manage-idps-and-orgs@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.MANAGE_ORGANIZATIONS,
                            AdminRoles.MANAGE_IDENTITY_PROVIDERS));
            return realm;
        }
    }
}
