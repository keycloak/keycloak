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
public class OrganizationAdminPermissionsTest extends AbstractOrganizationTest {

    @InjectRealm(config = OrganizationAdminPermissionsRealmConfig.class)
    ManagedRealm realm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testManageRealmRole() throws Exception {
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

    /**
     * Realm configuration with organizations enabled as well as 'realmAdmin' and 'testUser' users.
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
            realm.addUser("test-user")
                    .password("password")
                    .name("test", "user")
                    .email("test-user@localhost")
                    .emailVerified(true);
            return realm;
        }
    }
}
