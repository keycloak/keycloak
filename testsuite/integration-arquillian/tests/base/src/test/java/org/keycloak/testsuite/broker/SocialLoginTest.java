package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.social.openshift.OpenshiftV3IdentityProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.social.AbstractSocialLoginPage;
import org.keycloak.testsuite.pages.social.FacebookLoginPage;
import org.keycloak.testsuite.pages.social.GitHubLoginPage;
import org.keycloak.testsuite.pages.social.GoogleLoginPage;
import org.keycloak.testsuite.pages.social.LinkedInLoginPage;
import org.keycloak.testsuite.pages.social.MicrosoftLoginPage;
import org.keycloak.testsuite.pages.social.StackOverflowLoginPage;
import org.keycloak.testsuite.pages.social.TwitterLoginPage;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.FACEBOOK;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GITHUB;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.GOOGLE;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.LINKEDIN;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.MICROSOFT;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.OPENSHIFT;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.STACKOVERFLOW;
import static org.keycloak.testsuite.broker.SocialLoginTest.Provider.TWITTER;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SocialLoginTest extends AbstractKeycloakTest {

    public static final String SOCIAL_CONFIG = "social.config";
    public static final String REALM = "social";

    private static Properties config = new Properties();

    @Page
    private LoginPage loginPage;

    @Page
    private UpdateAccount updateAccountPage;

    public enum Provider {
        GOOGLE("google", GoogleLoginPage.class),
        FACEBOOK("facebook", FacebookLoginPage.class),
        GITHUB("github", GitHubLoginPage.class),
        TWITTER("twitter", TwitterLoginPage.class),
        LINKEDIN("linkedin", LinkedInLoginPage.class),
        MICROSOFT("microsoft", MicrosoftLoginPage.class),
        STACKOVERFLOW("stackoverflow", StackOverflowLoginPage.class),
        OPENSHIFT("openshift-v3", null);

        private String id;
        private Class<? extends AbstractSocialLoginPage> pageObjectClazz;

        Provider(String id, Class<? extends AbstractSocialLoginPage> pageObjectClazz) {
            this.id = id;
            this.pageObjectClazz = pageObjectClazz;
        }

        public String id() {
            return id;
        }

        public Class<? extends AbstractSocialLoginPage> pageObjectClazz() {
            return pageObjectClazz;
        }
    }

    private Provider currentTestProvider;

    @BeforeClass
    public static void loadConfig() throws Exception {
        assumeTrue(System.getProperties().containsKey(SOCIAL_CONFIG));

        config.load(new FileInputStream(System.getProperty(SOCIAL_CONFIG)));
    }
    
    @Before
    public void beforeSocialLoginTest() {
        accountPage.setAuthRealm(REALM);
        accountPage.navigateTo();
        currentTestProvider = null;
    }

    @After
    public void removeUser() {
        List<UserRepresentation> users = adminClient.realm(REALM).users().search(null, null, null);
        for (UserRepresentation user : users) {
            if (user.getServiceAccountClientId() == null) {
                log.infof("removing test user '%s'", user.getUsername());
                adminClient.realm(REALM).users().get(user.getId()).remove();
            }
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = RealmBuilder.create().name(REALM).build();
        List<IdentityProviderRepresentation> idps = new LinkedList<>();
        rep.setIdentityProviders(idps);

        for (Provider provider : Provider.values()) {
            idps.add(buildIdp(provider));
        }

        testRealms.add(rep);
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
        currentTestProvider = GOOGLE;
        performLogin();
        assertAccount();
    }

    @Test
    public void facebookLogin() {
        currentTestProvider = FACEBOOK;
        performLogin();
        assertAccount();
    }

    @Test
    public void githubLogin() {
        currentTestProvider = GITHUB;
        performLogin();
        assertAccount();
    }

    @Test
    public void twitterLogin() {
        currentTestProvider = TWITTER;
        performLogin();
        assertUpdateProfile(false, false, true);
        assertAccount();
    }

    @Test
    public void linkedinLogin() {
        currentTestProvider = LINKEDIN;
        performLogin();
        assertAccount();
    }

    @Test
    public void microsoftLogin() {
        currentTestProvider = MICROSOFT;
        performLogin();
        assertAccount();
    }

    @Test
    public void stackoverflowLogin() {
        currentTestProvider = STACKOVERFLOW;
        performLogin();
        assertUpdateProfile(false, false, true);
        assertAccount();
    }

    private IdentityProviderRepresentation buildIdp(Provider provider) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create().alias(provider.id()).providerId(provider.id()).build();
        idp.setEnabled(true);
        idp.getConfig().put("clientId", getConfig(provider, "clientId"));
        idp.getConfig().put("clientSecret", getConfig(provider, "clientSecret"));
        if (provider == STACKOVERFLOW) {
            idp.getConfig().put("key", getConfig(provider, "clientKey"));
        }
        if (provider == OPENSHIFT) {
            idp.getConfig().put("baseUrl", config.getProperty(provider.id() + ".baseUrl", OpenshiftV3IdentityProvider.BASE_URL));
        }
        return idp;
    }

    private String getConfig(Provider provider, String key) {
        return config.getProperty(provider.id() + "." + key, config.getProperty("common." + key));
    }

    private String getConfig(String key) {
        return getConfig(currentTestProvider, key);
    }

    private void performLogin() {
        loginPage.clickSocial(currentTestProvider.id());

        // Just to be sure there's no redirect in progress
        WaitUtils.pause(3000);
        WaitUtils.waitForPageToLoad(driver);

        // Only when there's not active session for the social provider, i.e. login is required
        if (URLUtils.currentUrlDoesntStartWith(driver, getAuthServerRoot().toASCIIString())) {
            log.infof("current URL: %s", driver.getCurrentUrl());
            log.infof("performing log in to '%s' ...", currentTestProvider.id());
            AbstractSocialLoginPage loginPage = Graphene.createPageFragment(currentTestProvider.pageObjectClazz(), driver.findElement(By.tagName("html")));
            loginPage.login(getConfig("username"), getConfig("password"));
        }
        else {
            log.infof("already logged in to '%s'; skipping the login process", currentTestProvider.id());
        }
    }

    private void assertAccount() {
        assertTrue(URLUtils.currentUrlStartWith(driver, accountPage.toString())); // Sometimes after login the URL ends with /# or similar

        assertEquals(getConfig("profile.firstName"), accountPage.getFirstName());
        assertEquals(getConfig("profile.lastName"), accountPage.getLastName());
        assertEquals(getConfig("profile.email"), accountPage.getEmail());
    }

    private void assertUpdateProfile(boolean firstName, boolean lastName, boolean email) {
        assertTrue(URLUtils.currentUrlDoesntStartWith(driver, accountPage.toString()));

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
}
