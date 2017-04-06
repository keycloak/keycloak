package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.social.openshift.OpenshiftV3IdentityProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.pages.LoginPage;
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

    @Page
    private GoogleLoginPage googleLoginPage;
    @Page
    private FacebookLoginPage facebookLoginPage;
    @Page
    private GitHubLoginPage gitHubLoginPage;
    @Page
    private TwitterLoginPage twitterLoginPage;
    @Page
    private LinkedInLoginPage linkedInLoginPage;
    @Page
    private MicrosoftLoginPage microsoftLoginPage;
    @Page
    private StackOverflowLoginPage stackOverflowLoginPage;

    public enum Provider {
        GOOGLE("google"),
        FACEBOOK("facebook"),
        GITHUB("github"),
        TWITTER("twitter"),
        LINKEDIN("linkedin"),
        MICROSOFT("microsoft"),
        STACKOVERFLOW("stackoverflow"),
        OPENSHIFT("openshift-v3");

        private String id;

        Provider(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    @BeforeClass
    public static void loadConfig() throws Exception {
        assumeTrue(System.getProperties().containsKey(SOCIAL_CONFIG));

        config.load(new FileInputStream(System.getProperty(SOCIAL_CONFIG)));
    }
    
    @Before
    public void beforeSocialLoginTest() {
        accountPage.setAuthRealm(REALM);
        accountPage.navigateTo();
    }

    @After
    public void removeUser() {
        log.info("Removing test user");
        List<UserRepresentation> users = adminClient.realm(REALM).users().search(null, null, null);
        for (UserRepresentation user : users) {
            if (user.getServiceAccountClientId() == null) {
                adminClient.realm(REALM).users().get(user.getId()).remove();
            }
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = RealmBuilder.create().name(REALM).build();
        List<IdentityProviderRepresentation> idps = new LinkedList<>();
        rep.setIdentityProviders(idps);

        idps.add(buildIdp(OPENSHIFT));
        idps.add(buildIdp(GOOGLE));
        idps.add(buildIdp(FACEBOOK));
        idps.add(buildIdp(GITHUB));
        idps.add(buildIdp(TWITTER));
        idps.add(buildIdp(LINKEDIN));
        idps.add(buildIdp(MICROSOFT));
        idps.add(buildIdp(STACKOVERFLOW));

        testRealms.add(rep);
    }

    @Test
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
        loginPage.clickSocial(GOOGLE.id());
        googleLoginPage.login(getConfig(GOOGLE, "username"), getConfig(GOOGLE, "password"));
        assertAccount(GOOGLE);
    }

    @Test
    public void facebookLogin() {
        loginPage.clickSocial(FACEBOOK.id());
        facebookLoginPage.login(getConfig(FACEBOOK, "profile.email"), getConfig(FACEBOOK, "password"));
        assertAccount(FACEBOOK);
    }

    @Test
    public void githubLogin() {
        loginPage.clickSocial(GITHUB.id());
        gitHubLoginPage.login(getConfig(GITHUB, "username"), getConfig(GITHUB, "password"));
        assertAccount(GITHUB);
    }

    @Test
    public void twitterLogin() {
        loginPage.clickSocial(TWITTER.id());
        twitterLoginPage.login(getConfig(TWITTER, "username"), getConfig(TWITTER, "password"));

        assertUpdateProfile(TWITTER, false, false, true);
        assertAccount(TWITTER);
    }

    @Test
    public void linkedinLogin() {
        loginPage.clickSocial(LINKEDIN.id());
        linkedInLoginPage.login(getConfig(LINKEDIN, "profile.email"), getConfig(LINKEDIN, "password"));
        assertAccount(LINKEDIN);
    }

    @Test
    public void microsoftLogin() {
        loginPage.clickSocial(MICROSOFT.id());
        microsoftLoginPage.login(getConfig(MICROSOFT, "profile.email"), getConfig(MICROSOFT, "password"));
        assertAccount(MICROSOFT);
    }

    @Test
    public void stackoverflowLogin() {
        loginPage.clickSocial(STACKOVERFLOW.id());
        stackOverflowLoginPage.login(getConfig(STACKOVERFLOW, "profile.email"), getConfig(STACKOVERFLOW, "password"));

        assertUpdateProfile(STACKOVERFLOW, false, false, true);
        assertAccount(STACKOVERFLOW);
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

    private void assertAccount(Provider provider) {
        assertTrue(URLUtils.currentUrlStartWith(driver, accountPage.toString())); // Sometimes after login the URL ends with /# or similar

        assertEquals(getConfig(provider, "profile.firstName"), accountPage.getFirstName());
        assertEquals(getConfig(provider, "profile.lastName"), accountPage.getLastName());
        assertEquals(getConfig(provider, "profile.email"), accountPage.getEmail());
    }

    private void assertUpdateProfile(Provider provider, boolean firstName, boolean lastName, boolean email) {
        assertTrue(URLUtils.currentUrlDoesntStartWith(driver, accountPage.toString()));

        if (firstName) {
            assertTrue(updateAccountPage.fields().getFirstName().isEmpty());
            updateAccountPage.fields().setFirstName(getConfig(provider, "profile.firstName"));
        }
        else {
            assertEquals(getConfig(provider, "profile.firstName"), updateAccountPage.fields().getFirstName());
        }

        if (lastName) {
            assertTrue(updateAccountPage.fields().getLastName().isEmpty());
            updateAccountPage.fields().setLastName(getConfig(provider, "profile.lastName"));
        }
        else {
            assertEquals(getConfig(provider, "profile.lastName"), updateAccountPage.fields().getLastName());
        }

        if (email) {
            assertTrue(updateAccountPage.fields().getEmail().isEmpty());
            updateAccountPage.fields().setEmail(getConfig(provider, "profile.email"));
        }
        else {
            assertEquals(getConfig(provider, "profile.email"), updateAccountPage.fields().getEmail());
        }

        updateAccountPage.submit();
    }
}
