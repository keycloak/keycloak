package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.social.openshift.OpenshiftV3IdentityProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Created by st on 19.01.17.
 */
public class SocialLoginTest extends AbstractKeycloakTest {

    public static final String SOCIAL_CONFIG = "social.config";

    private static Properties config = new Properties();

    @Page
    public AccountUpdateProfilePage account;

    @Page
    public LoginPage loginPage;

    @Page
    public LoginUpdateProfilePage updateProfilePage;

    @BeforeClass
    public static void loadConfig() throws Exception {
        assumeTrue(System.getProperties().containsKey(SOCIAL_CONFIG));

        config.load(new FileInputStream(System.getProperty(SOCIAL_CONFIG)));
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = RealmBuilder.create().name("social").build();
        List<IdentityProviderRepresentation> idps = new LinkedList<>();
        rep.setIdentityProviders(idps);

        idps.add(buildIdp("openshift-v3"));
        idps.add(buildIdp("google"));
        idps.add(buildIdp("facebook"));
        idps.add(buildIdp("github"));
        idps.add(buildIdp("twitter"));
        idps.add(buildIdp("linkedin"));
        idps.add(buildIdp("microsoft"));
        idps.add(buildIdp("stackoverflow"));

        testRealms.add(rep);
    }

    @Test
    public void openshiftLogin() throws Exception {
        account.open("social");
        loginPage.clickSocial("openshift-v3");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("inputUsername")));
        driver.findElement(By.id("inputUsername")).sendKeys(config.getProperty("openshift-v3.username", config.getProperty("common.username")));
        driver.findElement(By.id("inputPassword")).sendKeys(config.getProperty("openshift-v3.password", config.getProperty("common.password")));
        driver.findElement(By.cssSelector("button[type=submit]")).click();

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name=approve]")));
        driver.findElement(By.cssSelector("input[name=approve]")).click();

        assertEquals(config.getProperty("openshift-v3.username", config.getProperty("common.profile.username")), account.getUsername());
    }

    @Test
    public void googleLogin() throws InterruptedException {
        account.open("social");

        loginPage.clickSocial("google");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("Email")));

        driver.findElement(By.id("Email")).sendKeys(config.getProperty("google.username", config.getProperty("common.username")));
        driver.findElement(By.id("next")).click();

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("Passwd")));

        driver.findElement(By.id("Passwd")).sendKeys(config.getProperty("google.password", config.getProperty("common.password")));
        driver.findElement(By.id("signIn")).click();

        Graphene.waitGui().until(ExpectedConditions.elementToBeClickable(By.id("submit_approve_access")));

        driver.findElement(By.id("submit_approve_access")).click();

        assertEquals(config.getProperty("google.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("google.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals(config.getProperty("google.profile.email", config.getProperty("common.profile.email")), account.getEmail());
    }

    @Test
    public void faceBookLogin() {
        account.open("social");

        loginPage.clickSocial("facebook");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        driver.findElement(By.id("email")).sendKeys(config.getProperty("facebook.username", config.getProperty("common.username")));
        driver.findElement(By.id("pass")).sendKeys(config.getProperty("facebook.password", config.getProperty("common.password")));

        driver.findElement(By.id("loginbutton")).click();

        assertEquals(config.getProperty("facebook.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("facebook.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals(config.getProperty("facebook.profile.email", config.getProperty("common.profile.email")), account.getEmail());
    }

    @Test
    public void githubLogin() {
        account.open("social");

        loginPage.clickSocial("github");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("login_field")));
        driver.findElement(By.id("login_field")).sendKeys(config.getProperty("github.username", config.getProperty("common.username")));
        driver.findElement(By.id("password")).sendKeys(config.getProperty("github.password", config.getProperty("common.password")));

        driver.findElement(By.name("commit")).click();

        assertEquals(config.getProperty("github.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("github.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals(config.getProperty("github.profile.email", config.getProperty("common.profile.email")), account.getEmail());
    }

    @Test
    public void twitterLogin() {
        account.open("social");

        loginPage.clickSocial("twitter");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("username_or_email")));
        driver.findElement(By.id("username_or_email")).sendKeys(config.getProperty("twitter.username", config.getProperty("common.username")));
        driver.findElement(By.id("password")).sendKeys(config.getProperty("twitter.password", config.getProperty("common.password")));

        driver.findElement(By.id("allow")).click();

        assertTrue(updateProfilePage.isCurrent());

        assertEquals(config.getProperty("twitter.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("twitter.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals("", updateProfilePage.getEmail());

        updateProfilePage.update(null, null, "keycloakey@gmail.com");

        assertEquals(config.getProperty("twitter.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("twitter.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals(config.getProperty("twitter.profile.email", config.getProperty("common.profile.email")), account.getEmail());
    }

    @Test
    public void linkedinLogin() {
        account.open("social");

        loginPage.clickSocial("linkedin");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.id("session_key-oauth2SAuthorizeForm")));
        driver.findElement(By.id("session_key-oauth2SAuthorizeForm")).sendKeys(config.getProperty("linkedin.username", config.getProperty("common.username")));
        driver.findElement(By.id("session_password-oauth2SAuthorizeForm")).sendKeys(config.getProperty("linkedin.password", config.getProperty("common.password")));

        driver.findElement(By.name("authorize")).click();

        assertEquals(config.getProperty("linkedin.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("linkedin.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals(config.getProperty("linkedin.profile.email", config.getProperty("common.profile.email")), account.getEmail());
    }

    @Test
    public void microsoftLogin() {
        account.open("social");

        loginPage.clickSocial("microsoft");

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.name("loginfmt")));
        driver.findElement(By.name("loginfmt")).sendKeys(config.getProperty("microsoft.username", config.getProperty("common.username")));
        driver.findElement(By.xpath("//input[@value='Next']")).click();

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.name("passwd")));
        driver.findElement(By.name("passwd")).sendKeys(config.getProperty("microsoft.password", config.getProperty("common.password")));
        driver.findElement(By.xpath("//input[@value='Sign in']")).click();

        assertEquals(config.getProperty("microsoft.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("microsoft.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals(config.getProperty("microsoft.profile.email", config.getProperty("common.profile.email")), account.getEmail());
    }

    @Test
    public void stackoverflowLogin() {
        account.open("social");

        loginPage.clickSocial("stackoverflow");

        Graphene.waitModel().until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@title='log in with Stack_Exchange']")));
        driver.findElement(By.xpath("//a[@title='log in with Stack_Exchange']")).click();

        driver.switchTo().frame(driver.findElement(By.id("affiliate-signin-iframe")));

        Graphene.waitGui().until(ExpectedConditions.visibilityOfElementLocated(By.name("email")));
        driver.findElement(By.name("email")).sendKeys(config.getProperty("stackoverflow.username", config.getProperty("common.username")));
        driver.findElement(By.name("password")).sendKeys(config.getProperty("stackoverflow.password", config.getProperty("common.password")));

        driver.findElement(By.xpath("//input[@value='Sign In']")).click();

        assertEquals(config.getProperty("stackoverflow.profile.firstName", config.getProperty("common.profile.firstName")), updateProfilePage.getFirstName());
        assertEquals(config.getProperty("stackoverflow.profile.lastName", config.getProperty("common.profile.lastName")), updateProfilePage.getLastName());
        assertEquals("", updateProfilePage.getEmail());

        updateProfilePage.update(null, null, "keycloakey@gmail.com");

        assertEquals(config.getProperty("stackoverflow.profile.firstName", config.getProperty("common.profile.firstName")), account.getFirstName());
        assertEquals(config.getProperty("stackoverflow.profile.lastName", config.getProperty("common.profile.lastName")), account.getLastName());
        assertEquals(config.getProperty("stackoverflow.profile.email", config.getProperty("common.profile.email")), account.getEmail());
    }

    private IdentityProviderRepresentation buildIdp(String id) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create().alias(id).providerId(id).build();
        idp.setEnabled(true);
        idp.getConfig().put("clientId", config.getProperty(id + ".clientId"));
        idp.getConfig().put("clientSecret", config.getProperty(id + ".clientSecret"));
        if (id.equals("stackoverflow")) {
            idp.getConfig().put("key", config.getProperty(id + ".clientKey"));
        }
        if (id.equals("openshift-v3")) {
            idp.getConfig().put("baseUrl", config.getProperty(id + ".baseUrl", OpenshiftV3IdentityProvider.BASE_URL));
        }
        return idp;
    }

}
