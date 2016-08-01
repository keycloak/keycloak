package org.keycloak.testsuite.broker;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.RealmBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class AbstractBrokerTest extends AbstractKeycloakTest {

    protected abstract RealmRepresentation createProviderRealm();
    protected abstract RealmRepresentation createConsumerRealm();

    protected abstract List<ClientRepresentation> createProviderClients();
    protected abstract List<ClientRepresentation> createConsumerClients();

    protected abstract IdentityProviderRepresentation setUpIdentityProvider();

    protected abstract String providerRealmName();
    protected abstract String consumerRealmName();

    protected abstract String getUserLogin();
    protected abstract String getUserPassword();
    protected abstract String getUserEmail();

    protected abstract String getIDPAlias();

    @Page
    protected LoginPage accountLoginPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected AccountPasswordPage accountPasswordPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation providerRealm = createProviderRealm();
        RealmRepresentation consumerRealm = createConsumerRealm();

        testRealms.add(providerRealm);
        testRealms.add(consumerRealm);
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(getUserLogin());
        user.setEmail(getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(providerRealmName());
        String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + consumerRealmName());

        RealmResource realm = adminClient.realm(consumerRealmName());
        realm.identityProviders().create(setUpIdentityProvider());
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = createProviderClients();
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + providerRealmName());

                providerRealm.clients().create(client);
            }
        }

        clients = createConsumerClients();
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + consumerRealmName());

                consumerRealm.clients().create(client);
            }
        }
    }

    protected String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    protected IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    @Test
    public void logInAsUserInIDP() {
        driver.navigate().to(getAccountUrl(consumerRealmName()));

        log.debug("Clicking social " + getIDPAlias());
        accountLoginPage.clickSocial(getIDPAlias());

        waitForPage("log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(getUserLogin(), getUserPassword());

        waitForPage("update account information");

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(consumerRealmName()).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(getUserLogin()) && user.getEmail().equals(getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + getUserLogin() + " in realm " + consumerRealmName(),
                isUserFound);

        testSingleLogout();
    }

    @Test
    public void loginWithExistingUser() {
        logInAsUserInIDP();

        Integer userCount = adminClient.realm(consumerRealmName()).users().count();

        driver.navigate().to(getAccountUrl(consumerRealmName()));

        log.debug("Clicking social " + getIDPAlias());
        accountLoginPage.clickSocial(getIDPAlias());

        waitForPage("log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now", driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"));

        accountLoginPage.login(getUserLogin(), getUserPassword());

        assertEquals(accountPage.buildUri().toASCIIString().replace("master", "consumer") + "/", driver.getCurrentUrl());

        assertEquals(userCount, adminClient.realm(consumerRealmName()).users().count());
    }

    // KEYCLOAK-3267
    @Test
    public void loginWithExistingUserWithBruteForceEnabled() {
        adminClient.realm(consumerRealmName()).update(RealmBuilder.create().bruteForceProtected(true).failureFactor(2).build());

        loginWithExistingUser();

        driver.navigate().to(getAccountPasswordUrl(consumerRealmName()));

        accountPasswordPage.changePassword("password", "password");

        driver.navigate().to(getAuthRoot()
                + "/auth/realms/" + providerRealmName()
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(providerRealmName())));

        driver.navigate().to(getAccountUrl(consumerRealmName()));

        try {
            waitForPage("log in to");
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }

        for (int i = 0; i < 3; i++) {
            try {
                waitForElementEnabled("login");
            } catch (TimeoutException e) {
                Assert.fail("Timeout while waiting for login element enabled");
            }

            accountLoginPage.login(getUserLogin(), "invalid");
        }

        assertEquals("Invalid username or password.", accountLoginPage.getError());

        accountLoginPage.clickSocial(getIDPAlias());

        try {
            waitForPage("log in to");
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }

        Assert.assertTrue("Driver should be on the provider realm page right now", driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"));

        accountLoginPage.login(getUserLogin(), getUserPassword());

        assertEquals("Account is disabled, contact admin.", errorPage.getError());
    }

    private void testSingleLogout() {
        log.debug("Testing single log out");

        driver.navigate().to(getAccountUrl(providerRealmName()));

        Assert.assertTrue("Should be logged in the account page", driver.getTitle().endsWith("Account Management"));

        driver.navigate().to(getAuthRoot()
                + "/auth/realms/" + providerRealmName()
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(providerRealmName())));

        waitForPage("log in to " + providerRealmName());

        Assert.assertTrue("Should be on " + providerRealmName() + " realm", driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName()));

        driver.navigate().to(getAccountUrl(consumerRealmName()));

        Assert.assertTrue("Should be on " + consumerRealmName() + " realm on login page",
                driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/protocol/openid-connect/"));
    }

    private String getAccountUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/account";
    }

    private String getAccountPasswordUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/account/password";
    }

    private void waitForPage(final String title) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return input.getTitle().toLowerCase().contains(title);
            }
        };

        wait.until(condition);
    }

    private void waitForElementEnabled(final String elementName) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                List<WebElement> elements = input.findElements(By.name(elementName));
                if (elements.size() == 0)
                    return false;

                return elements.get(0).isEnabled();
            }
        };

        wait.until(condition);
    }

    private String encodeUrl(String url) {
        String result;
        try {
            result = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            result = url;
        }

        return result;
    }
}
