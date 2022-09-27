/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.example.authorization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPhotozExampleAdapterTest extends AbstractBasePhotozExampleAdapterTest {

    @Test
    public void testUserCanCreateAndDeleteAlbum() throws Exception {
        loginToClientPage(aliceUser);

        clientPage.createAlbum(ALICE_ALBUM_NAME);
        log.debug("Check if alice has resources stored");
        assertThat(getResourcesOfUser("alice"), is(not(empty())));

        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        log.debug("Check if alice has resources deleted");
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    @UncaughtServerErrorExpected
    public void createAlbumWithInvalidUser() throws Exception {
        loginToClientPage(aliceUser);

        clientPage.createAlbumWithInvalidUser(ALICE_ALBUM_NAME, response -> {
            assertThat(response.get("status"), is(equalTo(500L)));
            assertThat(response.get("res"), is(equalTo("Could not register protected resource.")));
        });
    }

    @Test
    public void testPathConfigInvalidation() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);

        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);

        log.debug("Check if alice has resources stored");
        assertThat(getResourcesOfUser("alice"), is(not(empty())));

        log.debug("Adding applyPolicies \"Only Owner Policy\" to \"Delete Album Permission\" policies.");
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(adminUser);

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        
        log.debug("Check if alice has resources stored");
        assertThat(getResourcesOfUser("alice"), is(not(empty())));

        log.debug("Adding applyPolicies \"Only Owner and Administrators Policy\" to \"Delete Album Permission\" policies.");
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner and Administrators Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        
        log.debug("Check if alice has resources deleted");
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }
 
    
    @Test
    public void testRegularUserCanNotAccessAdminResources() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.navigateToAdminAlbum(this::assertWasDenied);
    }

    @Test
    public void testAdminOnlyFromSpecificAddress() throws Exception {
        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);

        log.debug("Changing codes \"127.0.0.1\" to \"127.3.3.3\" of \"Only From a Specific Client Address\" policies.");
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Administration Policy".equals(policy.getName())) {
                policy.setPolicies(new HashSet<>());
                policy.getPolicies().add("Any Admin Policy");
                policy.getPolicies().add("Deny From a Specific Client Address");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasDenied);
    }

    @Test
    public void testAdminWithoutPermissionsToTypedResource() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);
        
        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);

        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        PoliciesResource policiesResource = getAuthorizationResource().policies();
        List<PolicyRepresentation> policies = policiesResource.policies();
        for (PolicyRepresentation policy : policies) {
            if ("Album Resource Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Any User Policy\"]");
                policiesResource.policy(policy.getId()).update(policy);
            }
            if ("Any User Policy".equals(policy.getName())) {
                ClientResource resourceServerClient = getClientResource(RESOURCE_SERVER_ID);
                RoleResource manageAlbumRole = resourceServerClient.roles().get("manage-albums");
                RoleRepresentation roleRepresentation = manageAlbumRole.toRepresentation();
                List<Map<String, Object>> roles = JsonSerialization.readValue(policy.getConfig().get("roles"), List.class);

                roles = roles.stream().filter((Map map) -> !map.get("id").equals(roleRepresentation.getId())).collect(Collectors.toList());

                policy.getConfig().put("roles", JsonSerialization.writeValueAsString(roles));

                policiesResource.policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(adminUser); // Clear cache

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);

        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        for (PolicyRepresentation policy : policies) {
            if ("Album Resource Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Any User Policy\", \"Administration Policy\"]");
                policiesResource.policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(adminUser); // Clear cache

        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    public void testAdminWithoutPermissionsToDeleteAlbum() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);

        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice"), is(empty()));

        PoliciesResource policiesResource = getAuthorizationResource().policies();
        List<PolicyRepresentation> policies = policiesResource.policies();
        for (PolicyRepresentation policy : policies) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner Policy\"]");
                policiesResource.policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);

        loginToClientPage(adminUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice"), is(not(empty())));

        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        for (PolicyRepresentation policy : policies) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner and Administrators Policy\"]");
                policiesResource.policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(adminUser); // Clear cache

        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    public void testClientRoleRepresentingUserConsent() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        RealmResource realmResource = realmsResouce().realm(REALM_NAME);
        UsersResource usersResource = realmResource.users();
        List<UserRepresentation> users = usersResource.search("alice", null, null, null, null, null);

        assertFalse(users.isEmpty());

        UserRepresentation userRepresentation = users.get(0);
        UserResource userResource = usersResource.get(userRepresentation.getId());

        ClientResource html5ClientApp = getClientResource("photoz-html5-client");
        ClientRepresentation clientRepresentation = html5ClientApp.toRepresentation();

        userResource.revokeConsent(clientRepresentation.getClientId());

        setManageAlbumScopeRequired();

        loginToClientPage(aliceUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser, "manage-albums");
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
    }

    @Test
    public void testClientRoleNotRequired() throws Exception {
        loginToClientPage(aliceUser);

        clientPage.createAlbum(ALICE_ALBUM_NAME);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        UsersResource usersResource = realmsResouce().realm(REALM_NAME).users();
        List<UserRepresentation> users = usersResource.search("alice", null, null, null, null, null);

        assertFalse(users.isEmpty());

        UserRepresentation userRepresentation = users.get(0);
        UserResource userResource = usersResource.get(userRepresentation.getId());

        ClientResource html5ClientApp = getClientResource("photoz-html5-client");

        userResource.revokeConsent(html5ClientApp.toRepresentation().getClientId());

        ClientResource resourceServerClient = getClientResource(RESOURCE_SERVER_ID);
        RoleResource manageAlbumRole = resourceServerClient.roles().get("manage-albums");
        RoleRepresentation roleRepresentation = manageAlbumRole.toRepresentation();

        setManageAlbumScopeRequired();

        manageAlbumRole.update(roleRepresentation);

        loginToClientPage(aliceUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Any User Policy".equals(policy.getName())) {
                List<Map<String, Object>> roles = JsonSerialization.readValue(policy.getConfig().get("roles"), List.class);

                roles.forEach(role -> {
                    String roleId = (String) role.get("id");
                    if (roleId.equals(manageAlbumRole.toRepresentation().getId())) {
                        role.put("required", false);
                    }
                });

                policy.getConfig().put("roles", JsonSerialization.writeValueAsString(roles));
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(aliceUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
    }

    @Test
    public void testOverridePermissionFromResourceParent() throws Exception {
        loginToClientPage(aliceUser);
        String resourceName = "My-Resource-Instance";
        clientPage.createAlbum(resourceName);

        clientPage.viewAlbum(resourceName, this::assertWasNotDenied);
        clientPage.deleteAlbum(resourceName, this::assertWasNotDenied);

        clientPage.createAlbum(resourceName);

        loginToClientPage(adminUser);

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(resourceName, this::assertWasNotDenied);
        clientPage.deleteAlbum(resourceName, this::assertWasNotDenied);

        loginToClientPage(aliceUser);
        clientPage.createAlbum(resourceName);

        AuthorizationResource authorizationResource = getAuthorizationResource();
        authorizationResource.resources().resources().forEach(resource -> {
            if (resource.getName().equals(resourceName)) {
                try {
                    PolicyRepresentation resourceInstancePermission = new PolicyRepresentation();

                    resourceInstancePermission.setName(resourceName + "Permission");
                    resourceInstancePermission.setType("resource");

                    Map<String, String> config = new HashMap<>();

                    config.put("resources", JsonSerialization.writeValueAsString(Arrays.asList(resource.getId())));
                    config.put("applyPolicies", JsonSerialization.writeValueAsString(Arrays.asList("Only Owner Policy")));

                    resourceInstancePermission.setConfig(config);
                    authorizationResource.policies().create(resourceInstancePermission);
                } catch (IOException e) {
                    throw new RuntimeException("Error creating policy.", e);
                }
            }
        });
        printUpdatedPolicies();

        loginToClientPage(adminUser);

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(resourceName, this::assertWasDenied);
        clientPage.deleteAlbum(resourceName, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.deleteAlbum(resourceName, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    public void testInheritPermissionFromResourceParent() throws Exception {
        loginToClientPage(aliceUser);

        final String RESOURCE_NAME = "My-Resource-Instance";
        clientPage.createAlbum(RESOURCE_NAME);
        clientPage.viewAlbum(RESOURCE_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(RESOURCE_NAME, this::assertWasNotDenied);

        clientPage.createAlbum(RESOURCE_NAME);

        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(RESOURCE_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(RESOURCE_NAME, this::assertWasNotDenied);

        loginToClientPage(aliceUser);
        clientPage.createAlbum(RESOURCE_NAME);

        ResourcesResource resourcesResource = getAuthorizationResource().resources();
        resourcesResource.resources().forEach(resource -> {
            if (resource.getName().equals(RESOURCE_NAME)) {
                try {
                    PolicyRepresentation resourceInstancePermission = new PolicyRepresentation();

                    resourceInstancePermission.setName(RESOURCE_NAME + "Permission");
                    resourceInstancePermission.setType("resource");

                    Map<String, String> config = new HashMap<>();

                    config.put("resources", JsonSerialization.writeValueAsString(Arrays.asList(resource.getId())));
                    config.put("applyPolicies", JsonSerialization.writeValueAsString(Arrays.asList("Only Owner Policy")));

                    resourceInstancePermission.setConfig(config);
                    getAuthorizationResource().policies().create(resourceInstancePermission);
                } catch (IOException e) {
                    throw new RuntimeException("Error creating policy.", e);
                }
            }
        });

        loginToClientPage(adminUser);

        clientPage.viewAlbum(RESOURCE_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(RESOURCE_NAME, this::assertWasDenied);

        resourcesResource.resources().forEach(resource -> {
            if (resource.getName().equals(RESOURCE_NAME)) {
                resource.setScopes(resource.getScopes().stream().filter(scope -> !scope.getName().equals("album:view")).collect(Collectors.toSet()));
                resourcesResource.resource(resource.getId()).update(resource);
            }
        });

        loginToClientPage(adminUser);

        clientPage.viewAlbum(RESOURCE_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(RESOURCE_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.deleteAlbum(RESOURCE_NAME, this::assertWasNotDenied);
        List<ResourceRepresentation> resources = resourcesResource.resources();
        assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());
    }

    //KEYCLOAK-3777

    @Test
    public void testEntitlementRequest() throws Exception {
        loginToClientPage(adminUser);

        clientPage.requestEntitlements((driver1, output, events) -> assertThat((String) output, containsString("admin:manage")));

        loginToClientPage(adminUser);
        clientPage.requestEntitlement((driver1, output, events) -> {
                assertThat((String) output, not(containsString("admin:manage")));
                assertThat((String) output, containsString("album:view"));
                assertThat((String) output, containsString("album:delete"));
            }
        );
    }

    @Test
    public void testResourceProtectedWithAnyScope() throws Exception {
        loginToClientPage(aliceUser);

        clientPage.requestResourceProtectedAllScope(this::assertWasDenied);
        clientPage.requestResourceProtectedAnyScope(response -> {
            assertThat(response.get("status"), anyOf(is(equalTo(404L)), is(equalTo(0L)))); // PhantomJS returns 0 and chrome 404
        });
    }
}
