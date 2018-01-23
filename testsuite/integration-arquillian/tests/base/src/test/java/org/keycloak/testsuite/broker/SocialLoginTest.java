package org.keycloak.testsuite.broker;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.social.openshift.OpenshiftV3IdentityProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.social.AbstractSocialLoginPage;
import org.keycloak.testsuite.pages.social.BitbucketLoginPage;
import org.keycloak.testsuite.pages.social.FacebookLoginPage;
import org.keycloak.testsuite.pages.social.GitHubLoginPage;
import org.keycloak.testsuite.pages.social.GitLabLoginPage;
import org.keycloak.testsuite.pages.social.GoogleLoginPage;
import org.keycloak.testsuite.pages.social.LinkedInLoginPage;
import org.keycloak.testsuite.pages.social.MicrosoftLoginPage;
import org.keycloak.testsuite.pages.social.PayPalLoginPage;
import org.keycloak.testsuite.pages.social.StackOverflowLoginPage;
import org.keycloak.testsuite.pages.social.TwitterLoginPage;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.BITBUCKET;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.FACEBOOK;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITHUB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITHUB_PRIVATE_EMAIL;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITLAB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GOOGLE;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.LINKEDIN;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.MICROSOFT;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.OPENSHIFT;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.PAYPAL;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.STACKOVERFLOW;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.TWITTER;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SocialLoginTest extends AbstractKeycloakTest {

    public static final String SOCIAL_CONFIG = "social.config";
    public static final String REALM = "social";
    public static final String EXCHANGE_CLIENT = "exchange-client";

    private static Properties config = new Properties();

    @Page
    private LoginPage loginPage;

    @Page
    private UpdateAccount updateAccountPage;

    public enum Provider {
        GOOGLE("google", GoogleLoginPage.class),
        FACEBOOK("facebook", FacebookLoginPage.class),
        GITHUB("github", GitHubLoginPage.class),
        GITHUB_PRIVATE_EMAIL("github", "github-private-email", GitHubLoginPage.class),
        TWITTER("twitter", TwitterLoginPage.class),
        LINKEDIN("linkedin", LinkedInLoginPage.class),
        MICROSOFT("microsoft", MicrosoftLoginPage.class),
        PAYPAL("paypal", PayPalLoginPage.class),
        STACKOVERFLOW("stackoverflow", StackOverflowLoginPage.class),
        OPENSHIFT("openshift-v3", null),
        GITLAB("gitlab", GitLabLoginPage.class),
        BITBUCKET("bitbucket", BitbucketLoginPage.class);

        private String id;
        private Class<? extends AbstractSocialLoginPage> pageObjectClazz;
        private String configId = null;

        Provider(String id, Class<? extends AbstractSocialLoginPage> pageObjectClazz) {
            this.id = id;
            this.pageObjectClazz = pageObjectClazz;
        }

        Provider(String id, String configId, Class<? extends AbstractSocialLoginPage> pageObjectClazz) {
            this.id = id;
            this.pageObjectClazz = pageObjectClazz;
            this.configId = configId;
        }

        public String id() {
            return id;
        }

        public Class<? extends AbstractSocialLoginPage> pageObjectClazz() {
            return pageObjectClazz;
        }

        public String configId() {
            return configId != null ? configId : id;
        }
    }

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create();
    }

    private Provider currentTestProvider = null;
    private AbstractSocialLoginPage currentSocialLoginPage = null;

    @BeforeClass
    public static void loadConfig() throws Exception {
        assumeTrue(System.getProperties().containsKey(SOCIAL_CONFIG));
        config.load(new FileInputStream(System.getProperty(SOCIAL_CONFIG)));
    }
    
    @Before
    public void beforeSocialLoginTest() {
        accountPage.setAuthRealm(REALM);
    }

    @After
    public void afterSocialLoginTest() {
        currentSocialLoginPage.logout();
        currentTestProvider = null;
    }

    private void removeUser() {
        List<UserRepresentation> users = adminClient.realm(REALM).users().search(null, null, null);
        for (UserRepresentation user : users) {
            if (user.getServiceAccountClientId() == null) {
                log.infof("removing test user '%s'", user.getUsername());
                adminClient.realm(REALM).users().get(user.getId()).remove();
            }
        }
    }

    private void setTestProvider(Provider provider) {
        adminClient.realm(REALM).identityProviders().create(buildIdp(provider));
        log.infof("added '%s' identity provider", provider.id());
        currentTestProvider = provider;
        currentSocialLoginPage = Graphene.createPageFragment(currentTestProvider.pageObjectClazz(), driver.findElement(By.tagName("html")));
        accountPage.navigateTo();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = RealmBuilder.create().name(REALM).build();
        testRealms.add(rep);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    public static void setupClientExchangePermissions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM);
        ClientModel client = session.realms().getClientByClientId(EXCHANGE_CLIENT, realm);
        // lazy init
        if (client != null) return;
        client = realm.addClient(EXCHANGE_CLIENT);
        client.setSecret("secret");
        client.setPublicClient(false);
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setEnabled(true);
        client.setDirectAccessGrantsEnabled(true);

        ClientPolicyRepresentation clientPolicyRep = new ClientPolicyRepresentation();
        clientPolicyRep.setName("client-policy");
        clientPolicyRep.addClient(client.getId());
        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.users().setPermissionsEnabled(true);
        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(clientPolicyRep, server);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientPolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        for (IdentityProviderModel idp : realm.getIdentityProviders()) {
            management.idps().setPermissionsEnabled(idp, true);
            management.idps().exchangeToPermission(idp).addAssociatedPolicy(clientPolicy);
        }

    }

    @Test
    @Ignore
    // TODO: Fix and revamp this test
    public void openshiftLogin() throws Exception {
        loginPage.clickSocial("openshift-v3");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("inputUsername")));
        driver.findElement(By.id("inputUsername")).sendKeys(config.getProperty("openshift-v3.username", config.getProperty("common.username")));
        driver.findElement(By.id("inputPassword")).sendKeys(config.getProperty("openshift-v3.password", config.getProperty("common.password")));
        driver.findElement(By.cssSelector("button[type=submit]")).click();

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name=approve]")));
        driver.findElement(By.cssSelector("input[name=approve]")).click();

        assertEquals(config.getProperty("openshift-v3.username", config.getProperty("common.profile.username")), accountPage.getUsername());
    }

    @Test
    public void googleLogin() throws InterruptedException {
        setTestProvider(GOOGLE);
        performLogin();
        assertAccount();
        testTokenExchange();
    }

    @Test
    public void bitbucketLogin() throws InterruptedException {
        setTestProvider(BITBUCKET);
        performLogin();
        assertAccount();
        testTokenExchange();
    }

    @Test
    public void gitlabLogin() throws InterruptedException {
        setTestProvider(GITLAB);
        performLogin();
        assertAccount();
        testTokenExchange();
    }

    @Test
    public void facebookLogin() throws InterruptedException {
        setTestProvider(FACEBOOK);
        performLogin();
        assertAccount();
        testTokenExchange();
    }


    @Test
    public void githubLogin() throws InterruptedException {
        setTestProvider(GITHUB);
        performLogin();
        assertAccount();
        testTokenExchange();
    }

    @Test
    public void githubPrivateEmailLogin() throws InterruptedException {
        setTestProvider(GITHUB_PRIVATE_EMAIL);
        performLogin();
        assertAccount();
    }

    @Test
    public void twitterLogin() {
        setTestProvider(TWITTER);
        performLogin();
        assertUpdateProfile(false, false, true);
        assertAccount();
    }

    @Test
    public void linkedinLogin() {
        setTestProvider(LINKEDIN);
        performLogin();
        assertAccount();
    }

    @Test
    public void microsoftLogin() {
        setTestProvider(MICROSOFT);
        performLogin();
        assertAccount();
    }

    @Test
    public void paypalLogin() {
        setTestProvider(PAYPAL);
        performLogin();
        assertAccount();
    }

    @Test
    public void stackoverflowLogin() throws InterruptedException {
        setTestProvider(STACKOVERFLOW);
        performLogin();
        assertUpdateProfile(false, false, true);
        assertAccount();
    }

    private IdentityProviderRepresentation buildIdp(Provider provider) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create().alias(provider.id()).providerId(provider.id()).build();
        idp.setEnabled(true);
        idp.setStoreToken(true);
        idp.getConfig().put("clientId", getConfig(provider, "clientId"));
        idp.getConfig().put("clientSecret", getConfig(provider, "clientSecret"));
        if (provider == STACKOVERFLOW) {
            idp.getConfig().put("key", getConfig(provider, "clientKey"));
        }
        if (provider == OPENSHIFT) {
            idp.getConfig().put("baseUrl", config.getProperty(provider.id() + ".baseUrl", OpenshiftV3IdentityProvider.BASE_URL));
        }
        if (provider == PAYPAL) {
            idp.getConfig().put("sandbox", getConfig(provider, "sandbox"));
        }
        return idp;
    }

    private String getConfig(Provider provider, String key) {
        return config.getProperty(provider.configId() + "." + key, config.getProperty("common." + key));
    }

    private String getConfig(String key) {
        return getConfig(currentTestProvider, key);
    }

    private void performLogin() {
        loginPage.clickSocial(currentTestProvider.id());

        // Just to be sure there's no redirect in progress
        WaitUtils.pause(3000);
        WaitUtils.waitForPageToLoad();

        // Only when there's not active session for the social provider, i.e. login is required
        if (URLUtils.currentUrlDoesntStartWith(getAuthServerRoot().toASCIIString())) {
            log.infof("current URL: %s", driver.getCurrentUrl());
            log.infof("performing log in to '%s' ...", currentTestProvider.id());
            currentSocialLoginPage.login(getConfig("username"), getConfig("password"));
        }
        else {
            log.infof("already logged in to '%s'; skipping the login process", currentTestProvider.id());
        }
    }

    private void assertAccount() {
        assertTrue(URLUtils.currentUrlStartWith(accountPage.toString())); // Sometimes after login the URL ends with /# or similar

        assertEquals(getConfig("profile.firstName"), accountPage.getFirstName());
        assertEquals(getConfig("profile.lastName"), accountPage.getLastName());
        assertEquals(getConfig("profile.email"), accountPage.getEmail());
    }

    private void assertUpdateProfile(boolean firstName, boolean lastName, boolean email) {
        assertTrue(URLUtils.currentUrlDoesntStartWith(accountPage.toString()));

        if (firstName) {
            assertTrue(updateAccountPage.fields().getFirstName().isEmpty());
            updateAccountPage.fields().setFirstName(getConfig("profile.firstName"));
        }
        else {
            assertEquals(getConfig("profile.firstName"), updateAccountPage.fields().getFirstName());
        }

        if (lastName) {
            assertTrue(updateAccountPage.fields().getLastName().isEmpty());
            updateAccountPage.fields().setLastName(getConfig("profile.lastName"));
        }
        else {
            assertEquals(getConfig("profile.lastName"), updateAccountPage.fields().getLastName());
        }

        if (email) {
            assertTrue(updateAccountPage.fields().getEmail().isEmpty());
            updateAccountPage.fields().setEmail(getConfig("profile.email"));
        }
        else {
            assertEquals(getConfig("profile.email"), updateAccountPage.fields().getEmail());
        }

        updateAccountPage.submit();
    }

    protected void testTokenExchange() {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.TOKEN_EXCHANGE);

        testingClient.server().run(SocialLoginTest::setupClientExchangePermissions);

        List<UserRepresentation> users = adminClient.realm(REALM).users().search(null, null, null);
        Assert.assertEquals(1, users.size());
        String username = users.get(0).getUsername();
        Client httpClient = ClientBuilder.newClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(REALM)
                .path("protocol/openid-connect/token");

        // obtain social token
        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(EXCHANGE_CLIENT, "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, username)
                                .param(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_ISSUER, currentTestProvider.id())

                ));
        Assert.assertEquals(200, response.getStatus());
        AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
        response.close();

        String socialToken = tokenResponse.getToken();
        Assert.assertNotNull(socialToken);

        // remove all users
        removeUser();

        users = adminClient.realm(REALM).users().search(null, null, null);
        Assert.assertEquals(0, users.size());

        // now try external exchange where we trust social provider and import the external token.
        response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(EXCHANGE_CLIENT, "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, socialToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.SUBJECT_ISSUER, currentTestProvider.id())

                ));
        Assert.assertEquals(200, response.getStatus());
        tokenResponse = response.readEntity(AccessTokenResponse.class);
        response.close();

        users = adminClient.realm(REALM).users().search(null, null, null);
        Assert.assertEquals(1, users.size());

        Assert.assertEquals(username, users.get(0).getUsername());

        // remove all users
        removeUser();

        users = adminClient.realm(REALM).users().search(null, null, null);
        Assert.assertEquals(0, users.size());

        ///// Test that we can update social token from session with stored tokens turned off.

        // turn off store token
        IdentityProviderRepresentation idp = adminClient.realm(REALM).identityProviders().get(currentTestProvider.id).toRepresentation();
        idp.setStoreToken(false);
        adminClient.realm(REALM).identityProviders().get(idp.getAlias()).update(idp);


        // first exchange social token to get a user session that should store the social token there
        response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(EXCHANGE_CLIENT, "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, socialToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.SUBJECT_ISSUER, currentTestProvider.id())

                ));
        Assert.assertEquals(200, response.getStatus());
        tokenResponse = response.readEntity(AccessTokenResponse.class);
        String keycloakToken = tokenResponse.getToken();
        response.close();

        // now take keycloak token and make sure it can get back the social token from the user session since stored tokens are off
        response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(EXCHANGE_CLIENT, "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, keycloakToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_ISSUER, currentTestProvider.id())

                ));
        Assert.assertEquals(200, response.getStatus());
        tokenResponse = response.readEntity(AccessTokenResponse.class);
        response.close();

        Assert.assertEquals(socialToken, tokenResponse.getToken());


         // turn on store token
        idp = adminClient.realm(REALM).identityProviders().get(currentTestProvider.id).toRepresentation();
        idp.setStoreToken(true);
        adminClient.realm(REALM).identityProviders().get(idp.getAlias()).update(idp);

        httpClient.close();
    }

}
