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
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

    public void testCreateDeleteAlbum() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            this.clientPage.login("alice", "alice");
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

    public void testOnlyOwnerCanDeleteAlbum() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);
            this.clientPage.login("alice", "alice");
            this.clientPage.createAlbum("Alice-Family-Album");
            this.clientPage.login("admin", "admin");
            this.clientPage.navigateToAdminAlbum();

            List<ResourceRepresentation> resources = getAuthorizationResource().resources().resources();

            assertFalse(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Delete Album Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Only Owner Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            this.clientPage.login("admin", "admin");
            this.clientPage.navigateToAdminAlbum();
            this.clientPage.deleteAlbum("Alice-Family-Album");

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

            resources = getAuthorizationResource().resources().resources();

            assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    public void testRegularUserCanNotAccessAdminResources() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);
            this.clientPage.login("alice", "alice");
            this.clientPage.navigateToAdminAlbum();

            assertTrue(this.clientPage.wasDenied());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    public void testAdminOnlyFromSpecificAddress() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);
            this.clientPage.login("admin", "admin");
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
            this.clientPage.login("alice", "alice");
            this.clientPage.createAlbum("Alice Family Album");

            this.clientPage.login("admin", "admin");
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

    public void testAdminWithoutPermissionsToDeleteScopePermission() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);
            this.clientPage.login("alice", "alice");
            this.clientPage.createAlbum("Alice Family Album");

            this.clientPage.login("admin", "admin");
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

            this.clientPage.login("alice", "alice");
            this.clientPage.createAlbum("Alice Family Album");

            this.clientPage.login("admin", "admin");
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

    public void testClientRoleRepresentingUserConsent() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);
            this.clientPage.login("alice", "alice");

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

            this.clientPage.login("alice", "alice");

            assertTrue(this.clientPage.wasDenied());

            this.clientPage.loginWithScopes("alice", "alice", RESOURCE_SERVER_ID + "/manage-albums");

            assertFalse(this.clientPage.wasDenied());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    public void testClientRoleNotRequired() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);
            this.clientPage.login("alice", "alice");

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

            this.clientPage.login("alice", "alice");

            assertTrue(this.clientPage.wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Any User Policy".equals(policy.getName())) {
                    List<Map> roles = JsonSerialization.readValue(policy.getConfig().get("roles"), List.class);

                    roles.forEach(new Consumer<Map>() {
                        @Override
                        public void accept(Map role) {
                            String roleId = (String) role.get("id");
                            if (roleId.equals(manageAlbumRole.toRepresentation().getId())) {
                                role.put("required", false);
                            }
                        }
                    });

                    policy.getConfig().put("roles", JsonSerialization.writeValueAsString(roles));

                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            this.clientPage.login("alice", "alice");

            assertFalse(this.clientPage.wasDenied());
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
}