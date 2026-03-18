/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationAdminRolesPermissionsTest extends AbstractOrganizationTest {

    @InjectRealm(config = OrganizationAdminPermissionsRealmConfig.class)
    ManagedRealm realm;

    @InjectMailServer
    MailServer mailServer;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testManageRealmRole() {
        try (
                Keycloak manageRealmAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build();
                Keycloak userAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("test-user").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build();
        ) {
            RealmResource realmAdminResource = manageRealmAdminClient.realm(realm.getName());
            RealmResource realmUserResource = userAdminClient.realm(realm.getName());

            /* Org */
            //create org
            OrganizationRepresentation orgRep = createRepresentation("testOrg", "testOrg.org");
            String orgId;
            try (
                    Response userResponse = realmUserResource.organizations().create(orgRep);
                    Response adminResponse = realmAdminResource.organizations().create(orgRep)
            ) {
                assertThat(userResponse.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
                assertThat(adminResponse.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(adminResponse);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            //search for org
            try {
                realmUserResource.organizations().search("testOrg.org");
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}
            assertThat(realmAdminResource.organizations().search("testOrg.org"), Matchers.notNullValue());

            //get org
            try {
                realmUserResource.organizations().get(orgId).toRepresentation();
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}
            assertThat(realmAdminResource.organizations().get(orgId).toRepresentation(), Matchers.notNullValue());

            //update org
            try (Response userResponse = realmUserResource.organizations().get(orgId).update(orgRep)) {
                assertThat(userResponse.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }

            //delete org
            try (Response userResponse = realmUserResource.organizations().get(orgId).delete()) {
                assertThat(userResponse.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }

            /* IdP */
            IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
            idpRep.setAlias("dummy");
            idpRep.setProviderId("oidc");
            realmAdminResource.identityProviders().create(idpRep).close();

            //create IdP
            try (
                    Response userResponse = realmUserResource.organizations().get(orgId).identityProviders().addIdentityProvider(idpRep.getAlias());
                    Response adminResponse = realmAdminResource.organizations().get(orgId).identityProviders().addIdentityProvider(idpRep.getAlias())
            ) {
                assertThat(userResponse.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
                assertThat(adminResponse.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
                realm.cleanup().add(r -> r.organizations().get(orgId).identityProviders().get(idpRep.getAlias()).delete().close());
            }

            //get IdP
            try {
                //we should get 403, not 400 or 404 etc.
                realmUserResource.organizations().get("non-existing").identityProviders().get(idpRep.getAlias()).toRepresentation();
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}
            try {
                realmUserResource.organizations().get(orgId).identityProviders().get(idpRep.getAlias()).toRepresentation();
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}
            assertThat(realmAdminResource.organizations().get(orgId).identityProviders().get(idpRep.getAlias()).toRepresentation(), Matchers.notNullValue());

            //delete IdP
            try (Response userResponse = realmUserResource.organizations().get(orgId).identityProviders().get(idpRep.getAlias()).delete()) {
                assertThat(userResponse.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }

            /* Members */
            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testOrg.org")
                    .email("user@testOrg.org")
                    .build();

            try (Response response = realmAdminResource.users().create(userRep)) {
                userRep.setId(ApiUtil.getCreatedId(response));
            }

            String userId;

            //create member
            try (
                    Response userResponse = realmUserResource.organizations().get(orgId).members().addMember(userRep.getId());
                    Response adminResponse = realmAdminResource.organizations().get(orgId).members().addMember(userRep.getId())
            ) {
                assertThat(userResponse.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
                assertThat(adminResponse.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                userId = ApiUtil.getCreatedId(adminResponse);
                assertThat(userId, Matchers.notNullValue());
                realm.cleanup().add(r -> r.organizations().get(orgId).members().member(userId).delete().close());
            }

            //get members
            try {
                //we should get 403, not 400 or 404 etc.
                realmUserResource.organizations().get("non-existing").members().list(-1, -1);
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}
            try {
                realmUserResource.organizations().get(orgId).members().list(-1, -1);
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}
            assertThat(realmAdminResource.organizations().get(orgId).members().list(-1, -1), Matchers.notNullValue());

            //get member
            try {
                realmUserResource.organizations().get(orgId).members().member(userId).toRepresentation();
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}
            assertThat(realmAdminResource.organizations().get(orgId).members().member(userId).toRepresentation(), Matchers.notNullValue());

            //delete member
            try (Response userResponse = realmUserResource.organizations().get(orgId).members().member(userId).delete()) {
                assertThat(userResponse.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }
    }

    @Test
    public void testManageOrganizationsRole() {
        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            // create org
            OrganizationRepresentation orgRep = createRepresentation("testManageOrg", "testManageOrg.org");
            String orgId;
            try (Response response = manageOrgsResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            // search org
            assertThat(manageOrgsResource.organizations().search("testManageOrg"), Matchers.notNullValue());

            // get org
            assertThat(manageOrgsResource.organizations().get(orgId).toRepresentation(), Matchers.notNullValue());

            // update org
            try (Response response = manageOrgsResource.organizations().get(orgId).update(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }

            // add member
            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testManageOrg.org")
                    .email("user@testManageOrg.org")
                    .build();
            try (Response response = manageOrgsResource.users().create(userRep)) {
                userRep.setId(ApiUtil.getCreatedId(response));
            }
            try (Response response = manageOrgsResource.organizations().get(orgId).members().addMember(userRep.getId())) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }

            // get members
            assertThat(manageOrgsResource.organizations().get(orgId).members().list(-1, -1), Matchers.notNullValue());

            // delete member
            try (Response response = manageOrgsResource.organizations().get(orgId).members().member(userRep.getId()).delete()) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }

            // delete org
            try (Response response = manageOrgsResource.organizations().get(orgId).delete()) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }
    }

    @Test
    public void testViewOrganizationsRole() {
        // first create an org and a member with the realmAdmin
        OrganizationRepresentation orgRep = createRepresentation("testViewOrg", "testViewOrg.org");
        String orgId;
        String userId;
        try (
                Keycloak realmAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource realmAdminResource = realmAdminClient.realm(realm.getName());
            try (Response response = realmAdminResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testViewOrg.org")
                    .email("user@testViewOrg.org")
                    .build();
            try (Response response = realmAdminResource.users().create(userRep)) {
                userId = ApiUtil.getCreatedId(response);
            }
            try (Response response = realmAdminResource.organizations().get(orgId).members().addMember(userId)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
        }

        try (
                Keycloak viewOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("view-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource viewOrgsResource = viewOrgsClient.realm(realm.getName());

            // org-level read operations should succeed
            assertThat(viewOrgsResource.organizations().search("testViewOrg"), Matchers.notNullValue());
            assertThat(viewOrgsResource.organizations().get(orgId).toRepresentation(), Matchers.notNullValue());

            // member listing should fail - view-organizations alone is not enough, requires user query permission
            try {
                viewOrgsResource.organizations().get(orgId).members().list(-1, -1);
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}

            // getOrganizations for a member should fail - requires view-users
            try {
                viewOrgsResource.organizations().members().getOrganizations(userId, true);
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}

            // write operations should fail
            try (Response response = viewOrgsResource.organizations().create(createRepresentation("anotherOrg", "anotherOrg.org"))) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
            try (Response response = viewOrgsResource.organizations().get(orgId).update(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
            try (Response response = viewOrgsResource.organizations().get(orgId).delete()) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }
    }

    @Test
    public void testViewRealmRoleBackwardCompat() {
        // first create an org with the realmAdmin
        OrganizationRepresentation orgRep = createRepresentation("testViewRealmOrg", "testViewRealmOrg.org");
        String orgId;
        try (
                Keycloak realmAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            try (Response response = realmAdminClient.realm(realm.getName()).organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }
        }
    }

    @Test
    public void testViewOrganizationsWithViewUsersRole() {
        // first create an org and add a member with the realmAdmin
        OrganizationRepresentation orgRep = createRepresentation("testViewOrgsUsersOrg", "testViewOrgsUsersOrg.org");
        String orgId;
        String userId;
        try (
                Keycloak realmAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource realmAdminResource = realmAdminClient.realm(realm.getName());
            try (Response response = realmAdminResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testViewOrgsUsersOrg.org")
                    .email("user@testViewOrgsUsersOrg.org")
                    .build();
            try (Response response = realmAdminResource.users().create(userRep)) {
                userId = ApiUtil.getCreatedId(response);
            }
            try (Response response = realmAdminResource.organizations().get(orgId).members().addMember(userId)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
        }

        // view-orgs-and-users-admin has view-organizations + view-users
        try (
                Keycloak viewOrgsAndUsersClient = adminClientFactory.create()
                        .realm(realm.getName()).username("view-orgs-and-users-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource viewOrgsAndUsersResource = viewOrgsAndUsersClient.realm(realm.getName());

            // org-level reads should succeed
            assertThat(viewOrgsAndUsersResource.organizations().search("testViewOrgsUsersOrg"), Matchers.notNullValue());
            assertThat(viewOrgsAndUsersResource.organizations().get(orgId).toRepresentation(), Matchers.notNullValue());

            // member listing should succeed - has both view-organizations and view-users (which implies query-users)
            assertThat(viewOrgsAndUsersResource.organizations().get(orgId).members().list(-1, -1), Matchers.not(Matchers.empty()));

            // get specific member should succeed
            assertThat(viewOrgsAndUsersResource.organizations().get(orgId).members().member(userId).toRepresentation(), Matchers.notNullValue());

            // member count should succeed
            assertThat(viewOrgsAndUsersResource.organizations().get(orgId).members().count(), equalTo(1L));

            // getOrganizations for a member should succeed — both top-level and per-org paths
            assertThat(viewOrgsAndUsersResource.organizations().members().getOrganizations(userId, true), Matchers.not(Matchers.empty()));
            assertThat(viewOrgsAndUsersResource.organizations().get(orgId).members().member(userId).getOrganizations(), Matchers.not(Matchers.empty()));

            // write operations should fail
            try (Response response = viewOrgsAndUsersResource.organizations().get(orgId).members().addMember(userId)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
            try (Response response = viewOrgsAndUsersResource.organizations().get(orgId).members().member(userId).delete()) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }
    }

    @Test
    public void testIdpLinkingRequiresManageIdentityProviders() {
        // manage-orgs-only-admin has manage-organizations but NOT manage-identity-providers
        // manage-orgs-admin has both manage-organizations AND manage-identity-providers

        String orgId;
        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            // create org
            OrganizationRepresentation orgRep = createRepresentation("testIdpOrg", "testIdpOrg.org");
            try (Response response = manageOrgsResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            // create IdP at realm level (requires manage-identity-providers)
            IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
            idpRep.setAlias("testIdpLink");
            idpRep.setProviderId("oidc");
            manageOrgsResource.identityProviders().create(idpRep).close();
            realm.cleanup().add(r -> r.identityProviders().get(idpRep.getAlias()).remove());

            // manage-orgs-admin (has both roles) can link IdP
            try (Response response = manageOrgsResource.organizations().get(orgId).identityProviders().addIdentityProvider(idpRep.getAlias())) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }

            // unlink so we can test the other user
            manageOrgsResource.organizations().get(orgId).identityProviders().get(idpRep.getAlias()).delete().close();
        }

        // manage-orgs-only-admin (only manage-organizations, no manage-identity-providers) cannot link IdP
        try (
                Keycloak manageOrgsOnlyClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-only-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsOnlyResource = manageOrgsOnlyClient.realm(realm.getName());

            try (Response response = manageOrgsOnlyResource.organizations().get(orgId).identityProviders().addIdentityProvider("testIdpLink")) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }
    }

    @Test
    public void testIdpUnlinkingRequiresManageOrganizations() {
        String orgId;

        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            OrganizationRepresentation orgRep = createRepresentation("testIdpUnlinkOrg", "testIdpUnlinkOrg.org");
            try (Response response = manageOrgsResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
            idpRep.setAlias("testIdpUnlink");
            idpRep.setProviderId("oidc");
            manageOrgsResource.identityProviders().create(idpRep).close();
            realm.cleanup().add(r -> r.identityProviders().get(idpRep.getAlias()).remove());

            try (Response response = manageOrgsResource.organizations().get(orgId).identityProviders().addIdentityProvider(idpRep.getAlias())) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }

        // view-orgs-manage-idps-admin has view-organizations + manage-identity-providers but NOT manage-organizations
        try (
                Keycloak viewOrgsManageIdpsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("view-orgs-manage-idps-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource viewOrgsManageIdpsResource = viewOrgsManageIdpsClient.realm(realm.getName());

            try (Response response = viewOrgsManageIdpsResource.organizations().get(orgId).identityProviders().get("testIdpUnlink").delete()) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }

        // manage-orgs-admin can unlink
        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            try (Response response = manageOrgsResource.organizations().get(orgId).identityProviders().get("testIdpUnlink").delete()) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }
    }

    @Test
    public void testQueryOrganizationsRole() {
        // first create an org and a member with the realm-admin
        OrganizationRepresentation orgRep = createRepresentation("testQueryOrg", "testQueryOrg.org");
        String orgId;
        String userId;
        try (
                Keycloak realmAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource realmAdminResource = realmAdminClient.realm(realm.getName());
            try (Response response = realmAdminResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testQueryOrg.org")
                    .email("user@testQueryOrg.org")
                    .build();
            try (Response response = realmAdminResource.users().create(userRep)) {
                userId = ApiUtil.getCreatedId(response);
            }
            try (Response response = realmAdminResource.organizations().get(orgId).members().addMember(userId)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
        }

        // query-orgs-admin has only query-organizations
        try (
                Keycloak queryOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("query-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource queryOrgsResource = queryOrgsClient.realm(realm.getName());

            // search should return empty list - query-organizations without view-organizations
            assertThat(queryOrgsResource.organizations().search("testQueryOrg"), Matchers.empty());

            // count should return 0
            assertThat(queryOrgsResource.organizations().count("testQueryOrg"), equalTo(0L));

            // getOrganizations for a member should return empty list
            assertThat(queryOrgsResource.organizations().members().getOrganizations(userId), Matchers.empty());

            // get specific org should fail - requires view-organizations
            try {
                queryOrgsResource.organizations().get(orgId).toRepresentation();
                fail("Expected ForbiddenException");
            } catch (ForbiddenException expected) {}

            // write operations should fail
            try (Response response = queryOrgsResource.organizations().create(createRepresentation("anotherOrg", "anotherOrg.org"))) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
            try (Response response = queryOrgsResource.organizations().get(orgId).update(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
            try (Response response = queryOrgsResource.organizations().get(orgId).delete()) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }
    }

    @Test
    public void testMemberManagementRequiresManageUsers() {
        // manage-orgs-only-admin has manage-organizations but NOT manage-users
        // manage-orgs-admin has both manage-organizations AND manage-users

        String orgId;
        String userId;

        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            // create org
            OrganizationRepresentation orgRep = createRepresentation("testMemberOrg", "testMemberOrg.org");
            try (Response response = manageOrgsResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            // create a user
            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testMemberOrg.org")
                    .email("user@testMemberOrg.org")
                    .build();
            try (Response response = manageOrgsResource.users().create(userRep)) {
                userId = ApiUtil.getCreatedId(response);
            }
        }

        // manage-orgs-only-admin (only manage-organizations, no manage-users) cannot add/remove members
        try (
                Keycloak manageOrgsOnlyClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-only-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsOnlyResource = manageOrgsOnlyClient.realm(realm.getName());

            // adding member should fail - requires manage-users
            try (Response response = manageOrgsOnlyResource.organizations().get(orgId).members().addMember(userId)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }

        // manage-orgs-admin (has both roles) can add and remove members
        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            try (Response response = manageOrgsResource.organizations().get(orgId).members().addMember(userId)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }

            try (Response response = manageOrgsResource.organizations().get(orgId).members().member(userId).delete()) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }
    }

    @Test
    public void testInvitationRequiresManageOrganizations() {
        String orgId;
        String userId;

        try (
                Keycloak realmAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource realmAdminResource = realmAdminClient.realm(realm.getName());

            OrganizationRepresentation orgRep = createRepresentation("testInviteOrg", "testInviteOrg.org");
            try (Response response = realmAdminResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testInviteOrg.org")
                    .email("user@testInviteOrg.org")
                    .build();
            try (Response response = realmAdminResource.users().create(userRep)) {
                userId = ApiUtil.getCreatedId(response);
            }
        }

        // view-orgs-manage-users-admin has view-organizations + manage-users but NOT manage-organizations
        try (
                Keycloak viewOrgsManageUsersClient = adminClientFactory.create()
                        .realm(realm.getName()).username("view-orgs-manage-users-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource viewOrgsManageUsersResource = viewOrgsManageUsersClient.realm(realm.getName());

            // inviteUser should fail
            try (Response response = viewOrgsManageUsersResource.organizations().get(orgId).members().inviteUser("newinvite@testInviteOrg.org", "New", "User")) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }

            // inviteExistingUser should fail
            try (Response response = viewOrgsManageUsersResource.organizations().get(orgId).members().inviteExistingUser(userId)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }

        // manage-orgs-admin has manage-organizations + manage-users — can invite
        String invitationId;
        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            try (Response response = manageOrgsResource.organizations().get(orgId).members().inviteUser("newinvite@testInviteOrg.org", "New", "User")) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }

            invitationId = manageOrgsResource.organizations().get(orgId).invitations().list().get(0).getId();
        }

        // view-orgs-manage-users-admin cannot delete or resend invitations
        try (
                Keycloak viewOrgsManageUsersClient = adminClientFactory.create()
                        .realm(realm.getName()).username("view-orgs-manage-users-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource viewOrgsManageUsersResource = viewOrgsManageUsersClient.realm(realm.getName());

            try (Response response = viewOrgsManageUsersResource.organizations().get(orgId).invitations().delete(invitationId)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }

            try (Response response = viewOrgsManageUsersResource.organizations().get(orgId).invitations().resend(invitationId)) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }

        // manage-orgs-admin can delete invitations
        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            try (Response response = manageOrgsResource.organizations().get(orgId).invitations().delete(invitationId)) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }
    }

    @Test
    public void testRemoveMemberRequiresManageOrganizations() {
        String orgId;
        String userId;

        try (
                Keycloak realmAdminClient = adminClientFactory.create()
                        .realm(realm.getName()).username("realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource realmAdminResource = realmAdminClient.realm(realm.getName());

            OrganizationRepresentation orgRep = createRepresentation("testRemoveMemberOrg", "testRemoveMemberOrg.org");
            try (Response response = realmAdminResource.organizations().create(orgRep)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
                orgId = ApiUtil.getCreatedId(response);
                realm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
            }

            UserRepresentation userRep = UserConfigBuilder.create()
                    .username("user@testRemoveMemberOrg.org")
                    .email("user@testRemoveMemberOrg.org")
                    .build();
            try (Response response = realmAdminResource.users().create(userRep)) {
                userId = ApiUtil.getCreatedId(response);
            }
            try (Response response = realmAdminResource.organizations().get(orgId).members().addMember(userId)) {
                assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
        }

        // view-orgs-manage-users-admin has view-organizations + manage-users but NOT manage-organizations
        try (
                Keycloak viewOrgsManageUsersClient = adminClientFactory.create()
                        .realm(realm.getName()).username("view-orgs-manage-users-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource viewOrgsManageUsersResource = viewOrgsManageUsersClient.realm(realm.getName());

            try (Response response = viewOrgsManageUsersResource.organizations().get(orgId).members().member(userId).delete()) {
                assertThat(response.getStatus(), equalTo(Status.FORBIDDEN.getStatusCode()));
            }
        }

        // manage-orgs-admin has manage-organizations + manage-users — can remove members
        try (
                Keycloak manageOrgsClient = adminClientFactory.create()
                        .realm(realm.getName()).username("manage-orgs-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()
        ) {
            RealmResource manageOrgsResource = manageOrgsClient.realm(realm.getName());

            try (Response response = manageOrgsResource.organizations().get(orgId).members().member(userId).delete()) {
                assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }
    }

    /**
     * Realm configuration with organizations enabled and test users
     */
    public static class OrganizationAdminPermissionsRealmConfig extends OrganizationRealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            super.configure(realm);
            realm.addUser("realm-admin")
                    .password("password")
                    .name("realm", "admin")
                    .email("admin-user@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.MANAGE_REALM,
                            AdminRoles.MANAGE_IDENTITY_PROVIDERS,
                            AdminRoles.MANAGE_USERS);
            realm.addUser("manage-orgs-admin")
                    .password("password")
                    .name("manage", "orgs")
                    .email("manage-orgs-admin@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.MANAGE_ORGANIZATIONS,
                            AdminRoles.MANAGE_IDENTITY_PROVIDERS,
                            AdminRoles.MANAGE_USERS);
            realm.addUser("view-orgs-admin")
                    .password("password")
                    .name("view", "orgs")
                    .email("view-orgs-admin@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_ORGANIZATIONS);
            realm.addUser("view-realm-admin")
                    .password("password")
                    .name("view", "realm")
                    .email("view-realm-admin@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_REALM);
            realm.addUser("manage-orgs-only-admin")
                    .password("password")
                    .name("manage-only", "orgs")
                    .email("manage-orgs-only-admin@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.MANAGE_ORGANIZATIONS);
            realm.addUser("view-orgs-and-users-admin")
                    .password("password")
                    .name("view", "orgs-and-users")
                    .email("view-orgs-and-users@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_ORGANIZATIONS,
                            AdminRoles.VIEW_USERS);
            realm.addUser("query-orgs-admin")
                    .password("password")
                    .name("query", "orgs")
                    .email("query-orgs@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.QUERY_ORGANIZATIONS);
            realm.addUser("view-orgs-manage-users-admin")
                    .password("password")
                    .name("view-orgs", "manage-users")
                    .email("view-orgs-manage-users@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_ORGANIZATIONS,
                            AdminRoles.MANAGE_USERS);
            realm.addUser("view-orgs-manage-idps-admin")
                    .password("password")
                    .name("view-orgs", "manage-idps")
                    .email("view-orgs-manage-idps@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_ORGANIZATIONS,
                            AdminRoles.MANAGE_IDENTITY_PROVIDERS);
            realm.addUser("test-user")
                    .password("password")
                    .name("test", "user")
                    .email("test-user@localhost")
                    .emailVerified(true);
            return realm;
        }
    }
}
