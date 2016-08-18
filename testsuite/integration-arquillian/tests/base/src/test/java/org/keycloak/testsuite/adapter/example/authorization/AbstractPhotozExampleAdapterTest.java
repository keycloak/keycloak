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

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.PhotozClientAuthzTestApp;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.IOUtil.loadJson;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPhotozExampleAdapterTest extends AbstractExampleAdapterTest {

    private static final String REALM_NAME = "photoz";
    private static final String RESOURCE_SERVER_ID = "photoz-restful-api";
    private static int TOKEN_LIFESPAN_LEEWAY = 3; // seconds

    @ArquillianResource
    private Deployer deployer;

    @Page
    private PhotozClientAuthzTestApp clientPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(REALM_NAME);
    }

    @Before
    public void beforePhotozExampleAdapterTest() {
        deleteAllCookiesForClientPage();
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadRealm(new File(TEST_APPS_HOME_DIR + "/photoz/photoz-realm.json"));

        realm.setAccessTokenLifespan(30 + TOKEN_LIFESPAN_LEEWAY); // seconds

        testRealms.add(realm);
    }

    @Deployment(name = PhotozClientAuthzTestApp.DEPLOYMENT_NAME)
    public static WebArchive deploymentClient() throws IOException {
        return exampleDeployment(PhotozClientAuthzTestApp.DEPLOYMENT_NAME);
    }

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deploymentResourceServer() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID);
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
        importResourceServerSettings();
    }

    @Test
    public void testUserCanCreateAndDeleteAlbum() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");
            this.clientPage.createAlbum("Alice Family Album");

            List<ResourceRepresentation> resources = getAuthorizationResource().resources().resources();
            assertFalse(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

            this.clientPage.deleteAlbum("Alice Family Album");

            resources = getAuthorizationResource().resources().resources();
            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testOnlyOwnerCanDeleteAlbum() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);
            loginToClientPage("alice", "alice");
            this.clientPage.createAlbum("Alice-Family-Album");

            loginToClientPage("admin", "admin");
            this.clientPage.navigateToAdminAlbum();

            List<ResourceRepresentation> resources = getAuthorizationResource().resources().resources();
            assertFalse(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Delete Album Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Only Owner Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            loginToClientPage("admin", "admin");

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum("Alice-Family-Album");
            assertTrue(this.clientPage.wasDenied());
            resources = getAuthorizationResource().resources().resources();
            assertFalse(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Delete Album Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Only Owner and Administrators Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum("Alice-Family-Album");
            assertFalse(this.clientPage.wasDenied());
            resources = getAuthorizationResource().resources().resources();
            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testRegularUserCanNotAccessAdminResources() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");
            this.clientPage.navigateToAdminAlbum();
            assertTrue(this.clientPage.wasDenied());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testAdminOnlyFromSpecificAddress() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("admin", "admin");
            this.clientPage.navigateToAdminAlbum();
            assertFalse(this.clientPage.wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Only From a Specific Client Address".equals(policy.getName())) {
                    String code = policy.getConfig().get("code");
                    policy.getConfig().put("code", code.replaceAll("127.0.0.1", "127.3.3.3"));
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            this.clientPage.navigateToAdminAlbum();
            assertTrue(this.clientPage.wasDenied());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testAdminWithoutPermissionsToTypedResource() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");
            this.clientPage.createAlbum("Alice Family Album");

            loginToClientPage("admin", "admin");
            this.clientPage.navigateToAdminAlbum();
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.viewAlbum("Alice Family Album");
            assertFalse(this.clientPage.wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Album Resource Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Any User Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
                if ("Any User Policy".equals(policy.getName())) {
                    ClientResource resourceServerClient = getClientResource(RESOURCE_SERVER_ID);
                    RoleResource manageAlbumRole = resourceServerClient.roles().get("manage-albums");
                    RoleRepresentation roleRepresentation = manageAlbumRole.toRepresentation();
                    List<Map> roles = JsonSerialization.readValue(policy.getConfig().get("roles"), List.class);

                    roles = roles.stream().filter(new Predicate<Map>() {
                        @Override
                        public boolean test(Map map) {
                            return !map.get("id").equals(roleRepresentation.getId());
                        }
                    }).collect(Collectors.toList());

                    policy.getConfig().put("roles", JsonSerialization.writeValueAsString(roles));

                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum("Alice Family Album");
            assertTrue(this.clientPage.wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Album Resource Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Any User Policy\", \"Administration Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum("Alice Family Album");
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum("Alice Family Album");
            List<ResourceRepresentation> resources = getAuthorizationResource().resources().resources();
            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testAdminWithoutPermissionsToDeleteAlbum() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");
            this.clientPage.createAlbum("Alice Family Album");

            loginToClientPage("admin", "admin");
            this.clientPage.navigateToAdminAlbum();
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.deleteAlbum("Alice Family Album");
            assertFalse(this.clientPage.wasDenied());
            List<ResourceRepresentation> resources = getAuthorizationResource().resources().resources();
            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Delete Album Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Only Owner Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            loginToClientPage("alice", "alice");
            this.clientPage.createAlbum("Alice Family Album");

            loginToClientPage("admin", "admin");
            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum("Alice Family Album");
            assertFalse(this.clientPage.wasDenied());
            resources = getAuthorizationResource().resources().resources();
            assertFalse(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum("Alice Family Album");
            assertTrue(this.clientPage.wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Delete Album Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Only Owner and Administrators Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum("Alice Family Album");
            assertFalse(this.clientPage.wasDenied());
            resources = getAuthorizationResource().resources().resources();
            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testClientRoleRepresentingUserConsent() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");
            assertFalse(this.clientPage.wasDenied());

            UsersResource usersResource = realmsResouce().realm(REALM_NAME).users();
            List<UserRepresentation> users = usersResource.search("alice", null, null, null, null, null);

            assertFalse(users.isEmpty());

            UserRepresentation userRepresentation = users.get(0);
            UserResource userResource = usersResource.get(userRepresentation.getId());

            ClientResource html5ClientApp = getClientResource("photoz-html5-client");

            userResource.revokeConsent(html5ClientApp.toRepresentation().getClientId());

            ClientResource resourceServerClient = getClientResource(RESOURCE_SERVER_ID);
            RoleResource roleResource = resourceServerClient.roles().get("manage-albums");
            RoleRepresentation roleRepresentation = roleResource.toRepresentation();

            roleRepresentation.setScopeParamRequired(true);

            roleResource.update(roleRepresentation);

            loginToClientPage("alice", "alice");
            assertTrue(this.clientPage.wasDenied());

            loginToClientPage("alice", "alice", RESOURCE_SERVER_ID + "/manage-albums");
            assertFalse(this.clientPage.wasDenied());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testClientRoleNotRequired() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");

            assertFalse(this.clientPage.wasDenied());

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

            roleRepresentation.setScopeParamRequired(true);

            manageAlbumRole.update(roleRepresentation);

            loginToClientPage("alice", "alice");
            assertTrue(this.clientPage.wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Any User Policy".equals(policy.getName())) {
                    List<Map> roles = JsonSerialization.readValue(policy.getConfig().get("roles"), List.class);

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

            loginToClientPage("alice", "alice");
            assertFalse(this.clientPage.wasDenied());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testOverridePermissionFromResourceParent() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");
            String resourceName = "My Resource Instance";
            this.clientPage.createAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.viewAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.navigateTo();
            this.clientPage.deleteAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.createAlbum(resourceName);

            loginToClientPage("admin", "admin");

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.navigateToAdminAlbum();;
            this.clientPage.deleteAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            loginToClientPage("alice", "alice");
            this.clientPage.createAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            getAuthorizationResource().resources().resources().forEach(resource -> {
                if (resource.getName().equals(resourceName)) {
                    try {
                        PolicyRepresentation resourceInstancePermission = new PolicyRepresentation();

                        resourceInstancePermission.setName(resourceName + "Permission");
                        resourceInstancePermission.setType("resource");

                        Map<String, String> config = new HashMap<>();

                        config.put("resources", JsonSerialization.writeValueAsString(Arrays.asList(resource.getId())));
                        config.put("applyPolicies", JsonSerialization.writeValueAsString(Arrays.asList("Only Owner Policy")));

                        resourceInstancePermission.setConfig(config);
                        getAuthorizationResource().policies().create(resourceInstancePermission);
                    } catch (Exception e) {
                        throw new RuntimeException("Error creating policy.", e);
                    }
                }
            });

            loginToClientPage("admin", "admin");

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum(resourceName);
            assertTrue(this.clientPage.wasDenied());

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum(resourceName);
            assertTrue(this.clientPage.wasDenied());

            loginToClientPage("alice", "alice");
            this.clientPage.deleteAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            ResourcesResource resourcesResource = getAuthorizationResource().resources();
            List<ResourceRepresentation> resources = resourcesResource.resources();
            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testInheritPermissionFromResourceParent() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            loginToClientPage("alice", "alice");

            String resourceName = "My Resource Instance";
            this.clientPage.createAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.viewAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.navigateTo();
            this.clientPage.deleteAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.createAlbum(resourceName);

            loginToClientPage("admin", "admin");

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.navigateToAdminAlbum();;
            this.clientPage.deleteAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            loginToClientPage("alice", "alice");
            this.clientPage.createAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            ResourcesResource resourcesResource = getAuthorizationResource().resources();
            resourcesResource.resources().forEach(resource -> {
                if (resource.getName().equals(resourceName)) {
                    try {
                        PolicyRepresentation resourceInstancePermission = new PolicyRepresentation();

                        resourceInstancePermission.setName(resourceName + "Permission");
                        resourceInstancePermission.setType("resource");

                        Map<String, String> config = new HashMap<>();

                        config.put("resources", JsonSerialization.writeValueAsString(Arrays.asList(resource.getId())));
                        config.put("applyPolicies", JsonSerialization.writeValueAsString(Arrays.asList("Only Owner Policy")));

                        resourceInstancePermission.setConfig(config);
                        getAuthorizationResource().policies().create(resourceInstancePermission);
                    } catch (Exception e) {
                        throw new RuntimeException("Error creating policy.", e);
                    }
                }
            });

            loginToClientPage("admin", "admin");

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum(resourceName);
            assertTrue(this.clientPage.wasDenied());

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum(resourceName);
            assertTrue(this.clientPage.wasDenied());

            resourcesResource.resources().forEach(resource -> {
                if (resource.getName().equals(resourceName)) {
                    resource.setScopes(resource.getScopes().stream().filter(scope -> !scope.getName().equals("urn:photoz.com:scopes:album:view")).collect(Collectors.toSet()));
                    resourcesResource.resource(resource.getId()).update(resource);
                }
            });

            loginToClientPage("admin", "admin");

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.viewAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());

            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum(resourceName);
            assertTrue(this.clientPage.wasDenied());

            loginToClientPage("alice", "alice");
            this.clientPage.deleteAlbum(resourceName);
            assertFalse(this.clientPage.wasDenied());
            List<ResourceRepresentation> resources = resourcesResource.resources();
            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

            resourcesResource.resources().forEach(resource -> {
                if (resource.getName().equals(resourceName)) {
                    resource.setScopes(Collections.emptySet());
                    resourcesResource.resource(resource.getId()).update(resource);
                }
            });
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    private void importResourceServerSettings() throws FileNotFoundException {
        getAuthorizationResource().importSettings(loadJson(new FileInputStream(new File(TEST_APPS_HOME_DIR + "/photoz/photoz-restful-api-authz-service.json")), ResourceServerRepresentation.class));
    }

    private AuthorizationResource getAuthorizationResource() throws FileNotFoundException {
        return getClientResource(RESOURCE_SERVER_ID).authorization();
    }

    private ClientResource getClientResource(String clientId) {
        ClientsResource clients = this.realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation resourceServer = clients.findByClientId(clientId).get(0);
        return clients.get(resourceServer.getId());
    }

    private void deleteAllCookiesForClientPage() {
        clientPage.navigateTo();
        driver.manage().deleteAllCookies();
    }
    
    private void loginToClientPage(String username, String password, String... scopes) {
        // We need to log out by deleting cookies because the log out button sometimes doesn't work in PhantomJS
        deleteAllCookiesForClientPage();
        deleteAllCookiesForTestRealm();
        clientPage.navigateTo();
        clientPage.login(username, password, scopes);
    }
}