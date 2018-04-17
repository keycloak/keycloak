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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.PhotozClientAuthzTestApp;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.NoSuchElementException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.IOUtil.loadJson;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPhotozExampleAdapterTest extends AbstractExampleAdapterTest {

    private static final String REALM_NAME = "photoz";
    protected static final String RESOURCE_SERVER_ID = "photoz-restful-api";
    private static final int TOKEN_LIFESPAN_LEEWAY = 3; // seconds

    @ArquillianResource
    private Deployer deployer;

    @Page
    @JavascriptBrowser
    private PhotozClientAuthzTestApp clientPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(REALM_NAME);
    }

    @BeforeClass
    public static void enabled() { ProfileAssume.assumePreview(); }

    @Before
    public void beforePhotozExampleAdapterTest() throws Exception {
        DroneUtils.addWebDriver(jsDriver);
        deleteAllCookiesForClientPage();
        this.deployer.deploy(RESOURCE_SERVER_ID);
        
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build()) {
            HttpGet request = new HttpGet(clientPage.toString() + "/unsecured/clean");
            httpClient.execute(request).close();
        } 
    }
    
    @After
    public void afterPhotozExampleAdapterTest() {
        this.deployer.undeploy(RESOURCE_SERVER_ID);
        DroneUtils.removeWebDriver();
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadRealm(new File(TEST_APPS_HOME_DIR + "/photoz/photoz-realm.json"));

        realm.setAccessTokenLifespan(30 + TOKEN_LIFESPAN_LEEWAY); // seconds

        testRealms.add(realm);
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
        importResourceServerSettings();
    }

    private List<ResourceRepresentation> getResourcesOfUser(String username) throws FileNotFoundException {
        return getAuthorizationResource().resources().resources().stream().filter(resource -> resource.getOwner().getName().equals(username)).collect(Collectors.toList());
    }
    
    private void printUpdatedPolicies() throws FileNotFoundException {
        log.debug("Check updated policies");
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            log.debugf("Policy: {0}", policy.getName());
            for (String key : policy.getConfig().keySet()) {
                log.debugf("-- key: {0}, value: {1}", key, policy.getConfig().get(key));
            }
        }
        log.debug("------------------------------");
    }
    
    @Test
    public void testUserCanCreateAndDeleteAlbum() throws Exception {
        loginToClientPage("alice", "alice");

        clientPage.createAlbum("Alice Family Album");
        log.debug("Check if alice has resources stored");
        assertThat(getResourcesOfUser("alice"), is(not(empty())));

        clientPage.deleteAlbum("Alice Family Album", false);
        log.debug("Check if alice has resources deleted");
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    public void createAlbumWithInvalidUser() throws Exception {
        loginToClientPage("alice", "alice");

        clientPage.createAlbumWithInvalidUser("Alice Family Album");

        log.debug("Check if the album was not created.");
        waitUntilElement(clientPage.getOutput()).text().not().contains("Request was successful");
        waitUntilElement(clientPage.getOutput()).text().contains("Could not register protected resource");
    }

    @Test
    public void testOnlyOwnerCanDeleteAlbum() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice-Family-Album");

        loginToClientPage("admin", "admin");
        clientPage.navigateToAdminAlbum(false);

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

        loginToClientPage("admin", "admin");

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum("Alice-Family-Album", true);
        
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

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum("Alice-Family-Album", false);
        
        log.debug("Check if alice has resources deleted");
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }
 
    
    @Test
    public void testRegularUserCanNotAccessAdminResources() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.navigateToAdminAlbum(true);
    }

    @Test
    public void testAdminOnlyFromSpecificAddress() throws Exception {
        loginToClientPage("admin", "admin");
        clientPage.navigateToAdminAlbum(false);

        log.debug("Changing codes \"127.0.0.1\" to \"127.3.3.3\" of \"Only From a Specific Client Address\" policies.");
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Only From a Specific Client Address".equals(policy.getName())) {
                String code = policy.getConfig().get("code");
                policy.getConfig().put("code", code.replaceAll("127.0.0.1", "127.3.3.3"));
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        clientPage.navigateToAdminAlbum(true);
    }

    @Test
    public void testAdminWithoutPermissionsToTypedResource() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice Family Album");
        
        loginToClientPage("admin", "admin");
        clientPage.navigateToAdminAlbum(false);

        clientPage.viewAlbum("Alice Family Album", false);

        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Album Resource Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Any User Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
            if ("Any User Policy".equals(policy.getName())) {
                ClientResource resourceServerClient = getClientResource(RESOURCE_SERVER_ID);
                RoleResource manageAlbumRole = resourceServerClient.roles().get("manage-albums");
                RoleRepresentation roleRepresentation = manageAlbumRole.toRepresentation();
                List<Map<String, Object>> roles = JsonSerialization.readValue(policy.getConfig().get("roles"), List.class);

                roles = roles.stream().filter((Map map) -> !map.get("id").equals(roleRepresentation.getId())).collect(Collectors.toList());

                policy.getConfig().put("roles", JsonSerialization.writeValueAsString(roles));

                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum("Alice Family Album", true);

        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Album Resource Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Any User Policy\", \"Administration Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum("Alice Family Album", false);

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum("Alice Family Album", false);
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    public void testAdminWithoutPermissionsToDeleteAlbum() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice Family Album");

        loginToClientPage("admin", "admin");
        clientPage.navigateToAdminAlbum(false);

        clientPage.deleteAlbum("Alice Family Album", false);
        assertThat(getResourcesOfUser("alice"), is(empty()));
        
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice Family Album");

        loginToClientPage("admin", "admin");
        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum("Alice Family Album", false);
        assertThat(getResourcesOfUser("alice"), is(not(empty())));

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum("Alice Family Album", true);

        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner and Administrators Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum("Alice Family Album", false);
        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    public void testClientRoleRepresentingUserConsent() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice Family Album");
        clientPage.viewAlbum("Alice Family Album", false);

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
        clientPage.viewAlbum("Alice Family Album", true);

        loginToClientPage("alice", "alice", RESOURCE_SERVER_ID + "/manage-albums");
        clientPage.viewAlbum("Alice Family Album", false);
    }

    @Test
    public void testClientRoleNotRequired() throws Exception {
        loginToClientPage("alice", "alice");

        clientPage.createAlbum("Alice Family Album");
        clientPage.viewAlbum("Alice Family Album", false);

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
        clientPage.viewAlbum("Alice Family Album", true);

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

        loginToClientPage("alice", "alice");
        clientPage.viewAlbum("Alice Family Album", false);
    }

    @Test
    public void testOverridePermissionFromResourceParent() throws Exception {
        loginToClientPage("alice", "alice");
        String resourceName = "My Resource Instance";
        clientPage.createAlbum(resourceName);

        clientPage.viewAlbum(resourceName, false);

        clientPage.navigateTo();
        clientPage.deleteAlbum(resourceName, false);

        clientPage.createAlbum(resourceName);

        loginToClientPage("admin", "admin");

        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum(resourceName, false);

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum(resourceName, false);

        loginToClientPage("alice", "alice");
        clientPage.createAlbum(resourceName);

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
                } catch (IOException e) {
                    throw new RuntimeException("Error creating policy.", e);
                }
            }
        });
        printUpdatedPolicies();

        loginToClientPage("admin", "admin");

        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum(resourceName, true);

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum(resourceName, true);

        loginToClientPage("alice", "alice");
        clientPage.deleteAlbum(resourceName, false);

        assertThat(getResourcesOfUser("alice"), is(empty()));
    }

    @Test
    public void testInheritPermissionFromResourceParent() throws Exception {
        loginToClientPage("alice", "alice");

        String resourceName = "My Resource Instance";
        clientPage.createAlbum(resourceName);

        clientPage.viewAlbum(resourceName, false);

        clientPage.navigateTo();
        clientPage.deleteAlbum(resourceName, false);

        clientPage.createAlbum(resourceName);

        loginToClientPage("admin", "admin");

        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum(resourceName, false);

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum(resourceName, false);

        loginToClientPage("alice", "alice");
        clientPage.createAlbum(resourceName);

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
                } catch (IOException e) {
                    throw new RuntimeException("Error creating policy.", e);
                }
            }
        });

        loginToClientPage("admin", "admin");

        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum(resourceName, true);

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum(resourceName, true);

        resourcesResource.resources().forEach(resource -> {
            if (resource.getName().equals(resourceName)) {
                resource.setScopes(resource.getScopes().stream().filter(scope -> !scope.getName().equals("album:view")).collect(Collectors.toSet()));
                resourcesResource.resource(resource.getId()).update(resource);
            }
        });

        loginToClientPage("admin", "admin");

        clientPage.navigateToAdminAlbum(false);
        clientPage.viewAlbum(resourceName, false);

        clientPage.navigateToAdminAlbum(false);
        clientPage.deleteAlbum(resourceName, true);

        loginToClientPage("alice", "alice");
        clientPage.deleteAlbum(resourceName, false);
        List<ResourceRepresentation> resources = resourcesResource.resources();
        assertTrue(resources.stream().filter(resource -> resource.getOwner().getName().equals("alice")).collect(Collectors.toList()).isEmpty());

        resourcesResource.resources().forEach(resource -> {
            if (resource.getName().equals(resourceName)) {
                resource.setScopes(Collections.emptySet());
                resourcesResource.resource(resource.getId()).update(resource);
            }
        });
    }
    
    //KEYCLOAK-3777
    @Test
    public void testEntitlementRequest() throws Exception {
        clientPage.navigateTo();
        loginToClientPage("admin", "admin");

        clientPage.requestEntitlements();
        assertTrue(jsDriver.getPageSource().contains("admin:manage"));

        clientPage.requestEntitlement();
        String pageSource = jsDriver.getPageSource();
        assertTrue(pageSource.contains("album:view"));
        assertTrue(pageSource.contains("album:delete"));
    }

    @Test
    public void testResourceProtectedWithAnyScope() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.requestResourceProtectedAllScope(true);
        clientPage.requestResourceProtectedAnyScope(false);
    }

    @Test
    public void testRequestResourceToOwner() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice-Family-Album", true);

        loginToClientPage("jdoe", "jdoe");
        clientPage.viewAllAlbums();
        clientPage.viewAlbum("Alice-Family-Album", true);
        clientPage.navigateTo();
        clientPage.viewAllAlbums();
        clientPage.deleteAlbum("Alice-Family-Album", true);

        loginToClientPage("alice", "alice");
        clientPage.accountGrantResource("Alice-Family-Album", "jdoe");

        loginToClientPage("jdoe", "jdoe");
        clientPage.viewAllAlbums();
        clientPage.viewAlbum("Alice-Family-Album", false);
        clientPage.navigateTo();
        clientPage.viewAllAlbums();
        clientPage.deleteAlbum("Alice-Family-Album", false);

        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice-Family-Album", true);

        loginToClientPage("jdoe", "jdoe");
        clientPage.viewAllAlbums();
        clientPage.viewAlbum("Alice-Family-Album", true);
        clientPage.navigateTo();
        clientPage.viewAllAlbums();
        clientPage.deleteAlbum("Alice-Family-Album", true);

        loginToClientPage("alice", "alice");
        clientPage.accountGrantRemoveScope("Alice-Family-Album", "jdoe", "album:delete");
        clientPage.accountGrantResource("Alice-Family-Album", "jdoe");

        loginToClientPage("jdoe", "jdoe");
        clientPage.viewAllAlbums();
        clientPage.viewAlbum("Alice-Family-Album", false);
        clientPage.navigateTo();
        clientPage.viewAllAlbums();
        clientPage.deleteAlbum("Alice-Family-Album", true);
    }

    @Test
    public void testOwnerSharingResource() throws Exception {
        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice-Family-Album", true);
        clientPage.accountShareResource("Alice-Family-Album", "jdoe");

        loginToClientPage("jdoe", "jdoe");
        clientPage.viewAllAlbums();
        clientPage.viewAlbum("Alice-Family-Album", false);
        clientPage.navigateTo();
        clientPage.viewAllAlbums();
        clientPage.deleteAlbum("Alice-Family-Album", false);

        loginToClientPage("alice", "alice");
        clientPage.createAlbum("Alice-Family-Album", true);
        clientPage.accountShareRemoveScope("Alice-Family-Album", "jdoe", "album:delete");

        loginToClientPage("jdoe", "jdoe");
        clientPage.viewAllAlbums();
        clientPage.viewAlbum("Alice-Family-Album", false);
        clientPage.navigateTo();
        clientPage.viewAllAlbums();
        clientPage.deleteAlbum("Alice-Family-Album", true);

        loginToClientPage("alice", "alice");
        clientPage.accountRevokeResource("Alice-Family-Album", "jdoe");

        loginToClientPage("jdoe", "jdoe");
        clientPage.viewAllAlbums();
        clientPage.viewAlbum("Alice-Family-Album", true);
    }

    private void importResourceServerSettings() throws FileNotFoundException {
        ResourceServerRepresentation authSettings = loadJson(new FileInputStream(new File(TEST_APPS_HOME_DIR + "/photoz/photoz-restful-api-authz-service.json")), ResourceServerRepresentation.class);

        authSettings.getPolicies().stream()
                .filter(x -> "Only Owner Policy".equals(x.getName()))
                .forEach(x -> x.getConfig().put("mavenArtifactVersion", System.getProperty("project.version")));

        getAuthorizationResource().importSettings(authSettings);
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
        jsDriver.manage().deleteAllCookies();
    }

    private void loginToClientPage(String username, String password, String... scopes) throws InterruptedException {
        log.debugf("--logging in as {0} with password: {1}; scopes: {2}", username, password, Arrays.toString(scopes));

        clientPage.navigateTo();
        if (jsDriver.getCurrentUrl().startsWith(clientPage.toString())) {
            try {
                clientPage.logOut();
            } catch (NoSuchElementException ex) {
                if ("phantomjs".equals(System.getProperty("js.browser"))) {
                    // PhantomJS is broken, it can't logout using sign out button sometimes, we have to clean sessions and remove cookies
                    adminClient.realm(REALM_NAME).logoutAll();

                    jsDriverTestRealmLoginPage.navigateTo();
                    driver.manage().deleteAllCookies();

                    clientPage.navigateTo();
                    driver.manage().deleteAllCookies();

                    clientPage.navigateTo();
                    // Check for correct logout
                    this.jsDriverTestRealmLoginPage.form().waitForLoginButtonPresent();
                } else {
                    throw ex;
                }
            }
        }

        clientPage.navigateTo();
        waitForPageToLoad();
        clientPage.login(username, password, scopes);
    }
}