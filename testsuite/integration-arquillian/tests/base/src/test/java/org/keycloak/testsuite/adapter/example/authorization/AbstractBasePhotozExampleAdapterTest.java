/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.utils.io.IOUtil.loadJson;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

import javax.ws.rs.core.Response;
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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.page.PhotozClientAuthzTestApp;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.javascript.JavascriptTestExecutorWithAuthorization;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractBasePhotozExampleAdapterTest extends AbstractPhotozJavascriptExecutorTest {

    protected static final String RESOURCE_SERVER_ID = "photoz-restful-api";
    protected static final String ALICE_ALBUM_NAME = "Alice-Family-Album";
    private static final int TOKEN_LIFESPAN_LEEWAY = 3; // seconds

    @ArquillianResource
    private Deployer deployer;

    @Page
    @JavascriptBrowser protected PhotozClientAuthzTestApp clientPage;

    @Page
    @JavascriptBrowser
    private OAuthGrant oAuthGrantPage;

    protected JavascriptTestExecutorWithAuthorization testExecutor;

    @FindBy(id = "output")
    @JavascriptBrowser
    protected WebElement outputArea;

    @FindBy(id = "events")
    @JavascriptBrowser
    protected WebElement eventsArea;

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(REALM_NAME);
        oAuthGrantPage.setAuthRealm(REALM_NAME);
    }

    @Before
    public void beforePhotozExampleAdapterTest() throws Exception {
        DroneUtils.addWebDriver(jsDriver);
        deployIgnoreIfDuplicate(RESOURCE_SERVER_ID);

        clientPage.navigateTo();
//        waitForPageToLoad();
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

    protected List<ResourceRepresentation> getResourcesOfUser(String username) throws FileNotFoundException {
        return getAuthorizationResource().resources().resources().stream().filter(resource -> resource.getOwner().getName().equals(username)).collect(Collectors.toList());
    }
    
    protected void printUpdatedPolicies() throws FileNotFoundException {
        log.debug("Check updated policies");
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            log.debugf("Policy: %s", policy.getName());
            for (String key : policy.getConfig().keySet()) {
                log.debugf("-- key: %s, value: %s", key, policy.getConfig().get(key));
            }
        }
        log.debug("------------------------------");
    }

    protected void assertOnTestAppUrl(WebDriver jsDriver, Object output, WebElement events) {
        waitForPageToLoad();
        assertCurrentUrlStartsWith(clientPage.toString(), jsDriver);
    }

    protected void assertWasDenied(Map<String, Object> response) {
        assertThat(response.get("status"), is(equalTo(401L)));
    }

    protected void assertWasNotDenied(Map<String, Object> response) {
        assertThat(response.get("status"), is(equalTo(200L)));
    }
    
    private void importResourceServerSettings() throws FileNotFoundException {
        ResourceServerRepresentation authSettings = loadJson(new FileInputStream(new File(TEST_APPS_HOME_DIR + "/photoz/photoz-restful-api-authz-service.json")), ResourceServerRepresentation.class);

        authSettings.getPolicies().stream()
                .filter(x -> "Only Owner Policy".equals(x.getName()))
                .forEach(x -> x.getConfig().put("mavenArtifactVersion", System.getProperty("project.version")));

        getAuthorizationResource().importSettings(authSettings);
    }

    protected AuthorizationResource getAuthorizationResource() throws FileNotFoundException {
        return getClientResource(RESOURCE_SERVER_ID).authorization();
    }

    protected ClientResource getClientResource(String clientId) {
        ClientsResource clients = this.realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation resourceServer = clients.findByClientId(clientId).get(0);
        return clients.get(resourceServer.getId());
    }

    protected void loginToClientPage(UserRepresentation user, String... scopes) throws InterruptedException {
        log.debugf("--logging in as '%s' with password: '%s'; scopes: %s", user.getUsername(), user.getCredentials().get(0).getValue(), Arrays.toString(scopes));

        if (testExecutor.isLoggedIn()) {
            testExecutor.logout(this::assertOnTestAppUrl, logoutConfirmPage);
            jsDriver.manage().deleteAllCookies();

            jsDriver.navigate().to(testRealmLoginPage.toString());
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

    protected void setManageAlbumScopeRequired() {
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

    /**
     * Redeploy if duplicate resource is present.
     * KEYCLOAK-18442
     *
     * @param name Name of the deployment
     */
    protected void deployIgnoreIfDuplicate(String name) {
        try {
            deployer.deploy(name);
        } catch (Exception e) {
            //DeploymentException is thrown by an deployer event handler and cannot be explicitly caught
            //noinspection ConstantConditions
            if (e instanceof DeploymentException && e.getMessage().contains("Duplicate resource")) {
                log.warnf("Duplicate resource '%s'. Trying to undeploy and deploy again...", name);
                deployer.undeploy(name);
                deployer.deploy(name);
                return;
            }
            throw e;
        }
    }
}
