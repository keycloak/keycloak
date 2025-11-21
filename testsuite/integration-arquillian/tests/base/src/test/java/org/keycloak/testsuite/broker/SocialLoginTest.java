package org.keycloak.testsuite.broker;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.social.AbstractSocialLoginPage;
import org.keycloak.testsuite.pages.social.BitbucketLoginPage;
import org.keycloak.testsuite.pages.social.FacebookLoginPage;
import org.keycloak.testsuite.pages.social.GitHubLoginPage;
import org.keycloak.testsuite.pages.social.GitLabLoginPage;
import org.keycloak.testsuite.pages.social.GoogleLoginPage;
import org.keycloak.testsuite.pages.social.InstagramLoginPage;
import org.keycloak.testsuite.pages.social.LinkedInLoginPage;
import org.keycloak.testsuite.pages.social.MicrosoftLoginPage;
import org.keycloak.testsuite.pages.social.OpenShiftLoginPage;
import org.keycloak.testsuite.pages.social.PayPalLoginPage;
import org.keycloak.testsuite.pages.social.StackOverflowLoginPage;
import org.keycloak.testsuite.pages.social.TwitterConsentLoginPage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

import com.google.common.collect.ImmutableMap;
import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.BITBUCKET;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.FACEBOOK;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.FACEBOOK_INCLUDE_BIRTHDAY;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITHUB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITHUB_PRIVATE_EMAIL;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITLAB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GOOGLE;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GOOGLE_HOSTED_DOMAIN;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GOOGLE_NON_MATCHING_HOSTED_DOMAIN;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.INSTAGRAM;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.LINKEDIN;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.MICROSOFT;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.MICROSOFT_SINGLE_TENANT;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.OPENSHIFT4;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.OPENSHIFT4_KUBE_ADMIN;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.PAYPAL;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.STACKOVERFLOW;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.TWITTER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@EnableFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)
public class SocialLoginTest extends AbstractKeycloakTest {

    public static final String SOCIAL_CONFIG = "social.config";
    public static final String REALM = "social";
    public static final String EXCHANGE_CLIENT = "exchange-client";

    private static final Properties config = new Properties();

    @Page
    private LoginPage loginPage;

    @Page
    private AppPage appPage;

    @Page
    private UpdateAccount updateAccountPage;

    public enum Provider {
        GOOGLE("google", GoogleLoginPage.class),
        GOOGLE_HOSTED_DOMAIN("google", "google-hosted-domain", GoogleLoginPage.class),
        GOOGLE_NON_MATCHING_HOSTED_DOMAIN("google", "google-hosted-domain", GoogleLoginPage.class),
        FACEBOOK("facebook", FacebookLoginPage.class),
        FACEBOOK_INCLUDE_BIRTHDAY("facebook", FacebookLoginPage.class),
        GITHUB("github", GitHubLoginPage.class),
        GITHUB_PRIVATE_EMAIL("github", "github-private-email", GitHubLoginPage.class),
        TWITTER("twitter", TwitterConsentLoginPage.class),
        LINKEDIN("linkedin-openid-connect", LinkedInLoginPage.class),
        MICROSOFT("microsoft", MicrosoftLoginPage.class),
        MICROSOFT_SINGLE_TENANT("microsoft", "microsoft-single-tenant", MicrosoftLoginPage.class),
        PAYPAL("paypal", PayPalLoginPage.class),
        STACKOVERFLOW("stackoverflow", StackOverflowLoginPage.class),
        OPENSHIFT4("openshift-v4", OpenShiftLoginPage.class),
        OPENSHIFT4_KUBE_ADMIN("openshift-v4", "openshift-v4-admin", OpenShiftLoginPage.class),
        GITLAB("gitlab", GitLabLoginPage.class),
        BITBUCKET("bitbucket", BitbucketLoginPage.class),
        INSTAGRAM("instagram", InstagramLoginPage.class);

        private final String id;
        private final Class<? extends AbstractSocialLoginPage> pageObjectClazz;
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

    private Provider currentTestProvider = null;
    private AbstractSocialLoginPage currentSocialLoginPage = null;

    @BeforeClass
    public static void loadConfig() throws Exception {
        assumeTrue(System.getProperties().containsKey(SOCIAL_CONFIG));
        config.load(new FileInputStream(System.getProperty(SOCIAL_CONFIG)));
    }

    @Before
    public void beforeSocialLoginTest() {
        oauth.realm(REALM);
        createAppClientInRealm(REALM);
    }

    @After
    public void afterSocialLoginTest() {
        currentTestProvider = null;
        oauth.realm("test");
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

        if(provider == OPENSHIFT4 || provider == OPENSHIFT4_KUBE_ADMIN) {
            ((OpenShiftLoginPage) currentSocialLoginPage).setUserLoginLinkTitle(getConfig(currentTestProvider, "loginBtnTitle"));
        }
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
        ClientModel client = session.clients().getClientByClientId(realm, EXCHANGE_CLIENT);
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
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientPolicyRep);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientPolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        session.identityProviders().getAllStream(IdentityProviderQuery.userAuthentication()).forEach(idp -> {
            management.idps().setPermissionsEnabled(idp, true);
            management.idps().exchangeToPermission(idp).addAssociatedPolicy(clientPolicy);
        });
    }

    @Test
    @UncaughtServerErrorExpected
    public void openshift4Login() {
        setTestProvider(OPENSHIFT4);
        performLogin();
        assertUpdateProfile(false, false, true);
        appPage.assertCurrent();
        testTokenExchange();
    }

    @Test
    public void openshift4KubeAdminLogin() {
        setTestProvider(OPENSHIFT4_KUBE_ADMIN);
        performLogin();
        assertUpdateProfile(true, true, true);
        appPage.assertCurrent();
    }

    @Test
    @UncaughtServerErrorExpected
    public void openshift4LoginWithGroupsMapper() {
        setTestProvider(OPENSHIFT4);
        addAttributeMapper("ocp-groups", "groups");
        performLogin();
        assertUpdateProfile(false, false, true);
        appPage.assertCurrent();
        assertAttribute("ocp-groups", getConfig("groups"));
    }

    @Test
    @UncaughtServerErrorExpected
    public void googleLogin() throws InterruptedException {
        setTestProvider(GOOGLE);
        performLogin();
        DroneUtils.getCurrentDriver().findElement(By.xpath("//button//span[contains(.,'Continue')]")).click();
        WaitUtils.pause(3000);
        WaitUtils.waitForPageToLoad();
        appPage.assertCurrent();
        testTokenExchange();
    }

    @Test
    @UncaughtServerErrorExpected
    public void googleHostedDomainLogin() throws InterruptedException {
        setTestProvider(GOOGLE_HOSTED_DOMAIN);
        navigateToLoginPage();
        assertTrue(driver.getCurrentUrl().contains("hd=" + getConfig(GOOGLE_HOSTED_DOMAIN, "hostedDomain")));
        doLogin();
        appPage.assertCurrent();
        testTokenExchange();
    }

    @Test
    public void googleNonMatchingHostedDomainLogin() throws InterruptedException {
        setTestProvider(GOOGLE_NON_MATCHING_HOSTED_DOMAIN);
        navigateToLoginPage();
        assertTrue(driver.getCurrentUrl().contains("hd=non-matching-hosted-domain"));
        doLogin();

        // Just to be sure there's no redirect in progress
        WaitUtils.waitForPageToLoad();

        WebElement errorMessage = driver.findElement(By.xpath(".//p[@class='instruction']"));

        assertTrue(errorMessage.isDisplayed());
        assertEquals("Unexpected error when authenticating with identity provider", errorMessage.getText());
    }

    @Test
    @UncaughtServerErrorExpected
    public void bitbucketLogin() throws InterruptedException {
        setTestProvider(BITBUCKET);
        performLogin();
        appPage.assertCurrent();
        testTokenExchange();
    }

    @Test
    @UncaughtServerErrorExpected
    public void gitlabLogin() throws InterruptedException {
        setTestProvider(GITLAB);
        performLogin();
        appPage.assertCurrent();
        testTokenExchange();
    }

    @Test
    @UncaughtServerErrorExpected
    public void facebookLogin() throws InterruptedException {
        setTestProvider(FACEBOOK);
        performLogin();
        appPage.assertCurrent();
        testTokenExchange();
    }

    @Test
    @UncaughtServerErrorExpected
    public void facebookLoginWithEnhancedScope() throws InterruptedException {
        setTestProvider(FACEBOOK_INCLUDE_BIRTHDAY);
        addAttributeMapper("birthday", "birthday");
        performLogin();
        appPage.assertCurrent();
        assertAttribute("birthday", getConfig("profile.birthday"));
        testTokenExchange();
    }

    @Test
    @Ignore
    public void instagramLogin() throws InterruptedException {
        setTestProvider(INSTAGRAM);
        performLogin();
        assertUpdateProfile(true, true, true);
        appPage.assertCurrent();
    }


    @Test
    @UncaughtServerErrorExpected
    public void githubLogin() throws InterruptedException {
        setTestProvider(GITHUB);
        performLogin();
        assertUpdateProfile(true, true, false);
        appPage.assertCurrent();
        testTokenExchange();
    }

    @Test
    public void githubPrivateEmailLogin() throws InterruptedException {
        setTestProvider(GITHUB_PRIVATE_EMAIL);
        performLogin();
        assertUpdateProfile(true, true, false);
        appPage.assertCurrent();
    }

    @Test
    public void twitterLogin() {
        setTestProvider(TWITTER);
        performLogin();
        assertUpdateProfile(false, false, true);
        appPage.assertCurrent();
    }

    @Test
    public void linkedinLogin() {
        setTestProvider(LINKEDIN);
        addAttributeMapper("picture", "picture", "linkedin-user-attribute-mapper");
        performLogin();
        appPage.assertCurrent();
        assertAttribute("picture", getConfig("profile.picture"));
    }

    @Test
    public void microsoftLogin() {
        setTestProvider(MICROSOFT);
        performLogin();
        appPage.assertCurrent();
    }

    @Test
    public void microsoftSingleTenantLogin() {
        setTestProvider(MICROSOFT_SINGLE_TENANT);
        navigateToLoginPage();
        assertTrue(driver.getCurrentUrl().contains("/" + getConfig(MICROSOFT_SINGLE_TENANT, "tenantId") + "/"));
        doLogin();
        appPage.assertCurrent();
    }

    @Test
    public void paypalLogin() {
        setTestProvider(PAYPAL);
        performLogin();
        appPage.assertCurrent();
    }

    @Test
    public void stackoverflowLogin() throws InterruptedException {
        setTestProvider(STACKOVERFLOW);
        performLogin();
        assertUpdateProfile(false, false, true);
        appPage.assertCurrent();
    }

    public IdentityProviderRepresentation buildIdp(Provider provider) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
                .alias(provider.id())
                .providerId(provider.id())
                .build();
        idp.setEnabled(true);
        idp.setStoreToken(true);
        idp.getConfig().put("clientId", getConfig(provider, "clientId"));
        idp.getConfig().put("clientSecret", getConfig(provider, "clientSecret"));

        if (provider == GOOGLE_HOSTED_DOMAIN) {
            final String hostedDomain = getConfig(provider, "hostedDomain");
            if (hostedDomain == null) {
                throw new IllegalArgumentException("'hostedDomain' for Google IdP must be specified");
            }
            idp.getConfig().put("hostedDomain", hostedDomain);
        }
        if (provider == GOOGLE_NON_MATCHING_HOSTED_DOMAIN) {
            idp.getConfig().put("hostedDomain", "non-matching-hosted-domain");
        }
        if (provider == STACKOVERFLOW) {
            idp.getConfig().put("key", getConfig(provider, "clientKey"));
        }
        if (provider == OPENSHIFT4 || provider == OPENSHIFT4_KUBE_ADMIN) {
            idp.getConfig().put("baseUrl", getConfig(provider, "baseUrl"));
        }
        if (provider == MICROSOFT_SINGLE_TENANT) {
            final String tenantId = getConfig(provider, "tenantId");
            if (tenantId == null) {
                throw new IllegalArgumentException("'tenantId' for Microsoft IdP must be specified");
            }
            idp.getConfig().put("tenantId", tenantId);
        }
        if (provider == PAYPAL) {
            idp.getConfig().put("sandbox", getConfig(provider, "sandbox"));
        }
        if (provider == FACEBOOK_INCLUDE_BIRTHDAY) {
            idp.getConfig().put("defaultScope", "public_profile,email,user_birthday");
            idp.getConfig().put("fetchedFields", "birthday");
        }
        return idp;
    }

    private void addAttributeMapper(String name, String jsonField) {
        addAttributeMapper(name, jsonField, currentTestProvider.id + "-user-attribute-mapper");
    }

    private void addAttributeMapper(String name, String jsonField, String mapperName) {
        IdentityProviderResource identityProvider = adminClient.realm(REALM).identityProviders().get(currentTestProvider.id);
        IdentityProviderRepresentation identityProviderRepresentation = identityProvider.toRepresentation();
        //Add birthday mapper
        IdentityProviderMapperRepresentation mapperRepresentation = new IdentityProviderMapperRepresentation();
        mapperRepresentation.setName(name);
        mapperRepresentation.setIdentityProviderAlias(identityProviderRepresentation.getAlias());
        mapperRepresentation.setIdentityProviderMapper(mapperName);
        mapperRepresentation.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString())
                .put(AbstractJsonUserAttributeMapper.CONF_JSON_FIELD, jsonField)
                .put(AbstractJsonUserAttributeMapper.CONF_USER_ATTRIBUTE, name)
                .build());
        identityProvider.addMapper(mapperRepresentation).close();
    }

    private String getConfig(Provider provider, String key) {
        String providerKey = provider.configId() + "." + key;
        return System.getProperty("social." + providerKey, config.getProperty(providerKey, config.getProperty("common." + key)));
    }

    private String getConfig(String key) {
        return getConfig(currentTestProvider, key);
    }

    private void performLogin() {
        navigateToLoginPage();
        doLogin();
    }

    private void navigateToLoginPage() {
        currentSocialLoginPage.logout(); // try to logout first to be sure we're not logged in

        driver.navigate().to(
                currentTestProvider.equals(PAYPAL)
                        ? oauth.loginForm().build().replace("https://localhost", "https://127.0.0.1")
                        : oauth.loginForm().build()
        );
        loginPage.clickSocial(currentTestProvider.id());

        // Just to be sure there's no redirect in progress
        WaitUtils.pause(3000);
        WaitUtils.waitForPageToLoad();
    }

    private void doLogin() {
        // Only when there's not active session for the social provider, i.e. login is required
        if (URLUtils.currentUrlDoesntStartWith(getAuthServerRoot().toASCIIString())) {
            log.infof("current URL: %s", driver.getCurrentUrl());
            log.infof("performing log in to '%s' ...", currentTestProvider.id());
            currentSocialLoginPage.login(getConfig("username"), getConfig("password"));
        } else {
            log.infof("already logged in to '%s'; skipping the login process", currentTestProvider.id());
        }
        WaitUtils.pause(3000);
        WaitUtils.waitForPageToLoad();
    }

    private void assertAttribute(String attrName, String expectedValue) {
        List<UserRepresentation> users = adminClient.realm(REALM).users().search(null, null, null);
        assertEquals(1, users.size());
        assertNotNull(users.get(0).getAttributes());
        assertNotNull(users.get(0).getAttributes().get(attrName));
        String attrValue = users.get(0).getAttributes().get(attrName).get(0);
        if (currentTestProvider.equals(LINKEDIN) && attrName.equals("picture")) {
            assertTrue(attrValue.contains(expectedValue.replace("image/","image/v2/").replace("/0/","/")));
        } else {
            assertEquals(expectedValue, attrValue);
        }
    }

    private void assertUpdateProfile(boolean firstName, boolean lastName, boolean email) {
        assertTrue(URLUtils.currentUrlDoesntStartWith(oauth.APP_AUTH_ROOT));

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

    private WebTarget getExchangeUrl(Client httpClient) {
        return httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(REALM)
                .path("protocol/openid-connect/token");
    }

    private AccessTokenResponse checkFeature(int expectedStatusCode, String username) {
        Client httpClient = AdminClientUtil.createResteasyClient();
        Response response = null;
        try {
            testingClient.server(REALM).run(SocialLoginTest::setupClientExchangePermissions);

            WebTarget exchangeUrl = getExchangeUrl(httpClient);
            response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(EXCHANGE_CLIENT, "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, username)
                                    .param(OAuth2Constants.REQUESTED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_ISSUER, currentTestProvider.id())
                    ));
            Assert.assertEquals(expectedStatusCode, response.getStatus());
            if (expectedStatusCode == Response.Status.OK.getStatusCode())
                return response.readEntity(AccessTokenResponse.class);
            else
                return null;
        } finally {
            if (response != null)
                response.close();
            httpClient.close();
        }
    }

    protected void testTokenExchange() {
        List<UserRepresentation> users = adminClient.realm(REALM).users().search(null, null, null);
        Assert.assertEquals(1, users.size());

        String username = users.get(0).getUsername();
        checkFeature(400, username);

        testingClient.enableFeature(Profile.Feature.TOKEN_EXCHANGE);

        Client httpClient = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse tokenResponse = checkFeature(200, username);
            Assert.assertNotNull(tokenResponse);
            String socialToken = tokenResponse.getToken();
            Assert.assertNotNull(socialToken);

            // remove all users
            removeUser();

            users = adminClient.realm(REALM).users().search(null, null, null);
            Assert.assertEquals(0, users.size());

            // now try external exchange where we trust social provider and import the external token.
            Response response = getExchangeUrl(httpClient).request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(EXCHANGE_CLIENT, "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, socialToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.SUBJECT_ISSUER, currentTestProvider.id())
                    ));
            Assert.assertEquals(200, response.getStatus());
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
            response = getExchangeUrl(httpClient).request()
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
            response = getExchangeUrl(httpClient).request()
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
        } finally {
            httpClient.close();
            testingClient.disableFeature(Profile.Feature.TOKEN_EXCHANGE);
            checkFeature(400, username);
        }
    }
}
