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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.adapter.page.PhotozClientAuthzTestApp;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.javascript.JavascriptTestExecutorWithAuthorization;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.utils.io.IOUtil.loadJson;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPhotozExampleAdapterTest extends AbstractPhotozJavascriptExecutorTest {

    protected static final String RESOURCE_SERVER_ID = "photoz-restful-api";
    protected static final String ALICE_ALBUM_NAME = "Alice-Family-Album";
    private static final int TOKEN_LIFESPAN_LEEWAY = 3; // seconds

    @ArquillianResource
    private Deployer deployer;

    @Page
    @JavascriptBrowser
    private PhotozClientAuthzTestApp clientPage;

    @Page
    @JavascriptBrowser
    private OAuthGrant oAuthGrantPage;

    private JavascriptTestExecutorWithAuthorization testExecutor;

    @FindBy(id = "output")
    @JavascriptBrowser
    protected WebElement outputArea;

    @FindBy(id = "events")
    @JavascriptBrowser
    protected WebElement eventsArea;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(REALM_NAME);
        oAuthGrantPage.setAuthRealm(REALM_NAME);
    }

    @Before
    public void beforePhotozExampleAdapterTest() throws Exception {
        DroneUtils.addWebDriver(jsDriver);
        this.deployer.deploy(RESOURCE_SERVER_ID);

        clientPage.navigateTo();
        waitForPageToLoad();
        assertCurrentUrlStartsWith(clientPage.toString());

        testExecutor = JavascriptTestExecutorWithAuthorization.create(jsDriver, jsDriverTestRealmLoginPage);
        clientPage.setTestExecutorPlayground(testExecutor, appServerContextRootPage + "/" + RESOURCE_SERVER_ID);
        jsDriver.manage().deleteAllCookies();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build()) {
            HttpGet request = new HttpGet(clientPage.toString() + "/unsecured/clean");
            httpClient.execute(request).close();
        } 
    }

    // workaround for KEYCLOAK-8660 from https://stackoverflow.com/questions/50917932/what-versions-of-jackson-are-allowed-in-jboss-6-4-20-patch
    @Before
    public void fixBrokenDeserializationOnEAP6() throws IOException, CliException, TimeoutException, InterruptedException {
        if (AppServerTestEnricher.isEAP6AppServer()) {
            OnlineManagementClient client = AppServerTestEnricher.getManagementClient();
            Administration administration = new Administration(client);

            client.execute("/system-property=jackson.deserialization.whitelist.packages:add(value=org.keycloak.example.photoz)");
            administration.reloadIfRequired();
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
            log.debugf("Policy: %s", policy.getName());
            for (String key : policy.getConfig().keySet()) {
                log.debugf("-- key: %s, value: %s", key, policy.getConfig().get(key));
            }
        }
        log.debug("------------------------------");
    }

    private void assertOnTestAppUrl(WebDriver jsDriver, Object output, WebElement events) {
        waitForPageToLoad();
        assertCurrentUrlStartsWith(clientPage.toString(), jsDriver);
    }

    private void assertWasDenied(Map<String, Object> response) {
        assertThat(response.get("status")).isEqualTo(401L);
    }

    private void assertWasNotDenied(Map<String, Object> response) {
        assertThat(response.get("status")).isEqualTo(200L);
    }
    
    @Test
    public void testUserCanCreateAndDeleteAlbum() throws Exception {
        loginToClientPage(aliceUser);

        clientPage.createAlbum(ALICE_ALBUM_NAME);
        log.debug("Check if alice has resources stored");
        assertThat(getResourcesOfUser("alice")).isNotEmpty();

        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        log.debug("Check if alice has resources deleted");
        assertThat(getResourcesOfUser("alice")).isEmpty();
    }

    @Test
    @UncaughtServerErrorExpected
    public void createAlbumWithInvalidUser() throws Exception {
        loginToClientPage(aliceUser);

        clientPage.createAlbumWithInvalidUser(ALICE_ALBUM_NAME, response -> {
            assertThat(response.get("status")).isEqualTo(500L);
            assertThat(response.get("res")).isEqualTo("Could not register protected resource.");
        });
    }

    @Test
    public void testOnlyOwnerCanDeleteAlbum() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);

        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);

        log.debug("Check if alice has resources stored");
        assertThat(getResourcesOfUser("alice")).isNotEmpty();

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
        assertThat(getResourcesOfUser("alice")).isNotEmpty();

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
        assertThat(getResourcesOfUser("alice")).isEmpty();
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
            if ("Only From a Specific Client Address".equals(policy.getName())) {
                String code = policy.getConfig().get("code")
                        .replaceAll("127.0.0.1", "127.3.3.3")
                        .replaceAll("0:0:0:0:0:0:0:1", "0:0:0:0:0:ffff:7f03:303");
                policy.getConfig().put("code", code);
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

        loginToClientPage(adminUser); // Clear cache

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);

        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Album Resource Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Any User Policy\", \"Administration Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(adminUser); // Clear cache

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice")).isEmpty();
    }

    @Test
    public void testAdminWithoutPermissionsToDeleteAlbum() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);

        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice")).isEmpty();
        
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME);

        loginToClientPage(adminUser);
        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice")).isNotEmpty();

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if ("Delete Album Permission".equals(policy.getName())) {
                policy.getConfig().put("applyPolicies", "[\"Only Owner and Administrators Policy\"]");
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
        printUpdatedPolicies();

        loginToClientPage(adminUser); // Clear cache

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice")).isEmpty();
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

        loginToClientPage(adminUser);

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(resourceName, this::assertWasDenied);
        clientPage.deleteAlbum(resourceName, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.deleteAlbum(resourceName, this::assertWasNotDenied);
        assertThat(getResourcesOfUser("alice")).isEmpty();
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

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
        clientPage.viewAlbum(RESOURCE_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(RESOURCE_NAME, this::assertWasDenied);

        resourcesResource.resources().forEach(resource -> {
            if (resource.getName().equals(RESOURCE_NAME)) {
                resource.setScopes(resource.getScopes().stream().filter(scope -> !scope.getName().equals("album:view")).collect(Collectors.toSet()));
                resourcesResource.resource(resource.getId()).update(resource);
            }
        });

        loginToClientPage(adminUser);

        clientPage.navigateToAdminAlbum(this::assertWasNotDenied);
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

        clientPage.requestEntitlements((driver1, output, events) -> assertThat((String)output).contains("admin:manage"));

        loginToClientPage(adminUser);
        clientPage.requestEntitlement((driver1, output, events) -> assertThat((String)output)
                .doesNotContain("admin:manage")
                .contains("album:view")
                .contains("album:delete")
        );
    }

    @Test
    public void testResourceProtectedWithAnyScope() throws Exception {
        loginToClientPage(aliceUser);

        clientPage.requestResourceProtectedAllScope(this::assertWasDenied);
        clientPage.requestResourceProtectedAnyScope(response -> {
            assertThat(response.get("status")).isIn(404L, 0L); // PhantomJS returns 0 and chrome 404
        });
    }


    @Test
    public void testRequestResourceToOwner() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.accountGrantResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();

        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                    .login()
                    .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.accountGrantRemoveScope(ALICE_ALBUM_NAME, "jdoe", "album:delete");

        // get back to clientPage and init javascript adapter in order to navigate to accountPage again
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);
        clientPage.accountGrantResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);


        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
    }

    @Test
    public void testOwnerSharingResource() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);
        clientPage.accountShareResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);
        clientPage.accountShareRemoveScope(ALICE_ALBUM_NAME, "jdoe", "album:delete");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.accountRevokeResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
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

    private void loginToClientPage(UserRepresentation user, String... scopes) throws InterruptedException {
        log.debugf("--logging in as {0} with password: {1}; scopes: {2}", user.getUsername(), user.getCredentials().get(0).getValue(), Arrays.toString(scopes));

        if (testExecutor.isLoggedIn()) {
            testExecutor.logout(this::assertOnTestAppUrl);
            jsDriver.manage().deleteAllCookies();

            jsDriver.navigate().to(testRealmLoginPage.toString());
            waitForPageToLoad();
            jsDriver.manage().deleteAllCookies();
        }

        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginFormWithScopesWithPossibleConsentPage(user, this::assertOnTestAppUrl, oAuthGrantPage, scopes)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        new WebDriverWait(jsDriver, 10).until(this::isLoaded);
    }

    public boolean isLoaded(WebDriver w) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) w;

        Map<String, Object> o = (Map<String, Object>) jsExecutor.executeScript("return window.authorization.config");

        return o != null && o.containsKey("token_endpoint");
    }

    private void setManageAlbumScopeRequired() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();

        clientScope.setName("manage-albums");
        clientScope.setProtocol("openid-connect");

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();

        mapper.setName("manage-albums");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper(UserClientRoleMappingMapper.PROVIDER_ID);

        Map<String, String> config = new HashMap<>();
        config.put("access.token.claim", "true");
        config.put("id.token.claim", "true");
        config.put("userinfo.token.claim", "true");
        config.put(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID, "photoz-restful-api");

        mapper.setConfig(config);

        clientScope.setProtocolMappers(Arrays.asList(mapper));

        RealmResource realmResource = realmsResouce().realm(REALM_NAME);
        ClientScopesResource clientScopes = realmResource.clientScopes();
        Response resp = clientScopes.create(clientScope);
        Assert.assertEquals(201, resp.getStatus());
        resp.close();
        String clientScopeId = ApiUtil.getCreatedId(resp);
        ClientResource resourceServer = getClientResource(RESOURCE_SERVER_ID);
        clientScopes.get(clientScopeId).getScopeMappings().clientLevel(resourceServer.toRepresentation().getId()).add(Arrays.asList(resourceServer.roles().get("manage-albums").toRepresentation()));
        ClientResource html5ClientApp = getClientResource("photoz-html5-client");
        html5ClientApp.addOptionalClientScope(clientScopeId);
        html5ClientApp.getScopeMappings().realmLevel().add(Arrays.asList(realmResource.roles().get("user").toRepresentation(), realmResource.roles().get("admin").toRepresentation()));
        ClientRepresentation clientRep = html5ClientApp.toRepresentation();
        clientRep.setFullScopeAllowed(false);
        html5ClientApp.update(clientRep);
    }
}
