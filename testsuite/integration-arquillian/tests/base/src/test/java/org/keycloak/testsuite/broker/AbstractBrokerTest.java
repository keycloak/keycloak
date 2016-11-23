package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.Retry;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.RealmBuilder;

import org.openqa.selenium.TimeoutException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.*;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.UserBuilder;

public abstract class AbstractBrokerTest extends AbstractKeycloakTest {

    @Page
    protected LoginPage accountLoginPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected AccountPasswordPage accountPasswordPage;

    @Page
    protected ErrorPage errorPage;
    
    @Page
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    protected BrokerConfiguration bc = getBrokerConfiguration();

    /**
     * Returns a broker configuration. Return value should not change between calls.
     * @return 
     */
    protected abstract BrokerConfiguration getBrokerConfiguration();

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation providerRealm = bc.createProviderRealm();
        RealmRepresentation consumerRealm = bc.createConsumerRealm();

        testRealms.add(providerRealm);
        testRealms.add(consumerRealm);
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + bc.providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().create(bc.setUpIdentityProvider(suiteContext));
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = bc.createProviderClients(suiteContext);
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.providerRealmName());

                providerRealm.clients().create(client);
            }
        }

        clients = bc.createConsumerClients(suiteContext);
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.consumerRealmName());

                consumerRealm.clients().create(client);
            }
        }
    }

    protected String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    @Test
    public void logInAsUserInIDP() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        log.debug("Clicking social " + bc.getIDPAlias());
        accountLoginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "update account information");

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
                isUserFound);

        testSingleLogout();
    }

    @Test
    public void loginWithExistingUser() {
        logInAsUserInIDP();

        Integer userCount = adminClient.realm(bc.consumerRealmName()).users().count();

        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        log.debug("Clicking social " + bc.getIDPAlias());
        accountLoginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now", driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

        assertEquals(accountPage.buildUri().toASCIIString().replace("master", "consumer") + "/", driver.getCurrentUrl());

        assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());
    }
    
    // KEYCLOAK-2957
    @Test
    public void testLinkAccountWithEmailVerified() {
        //start mail server
        MailServer.start();
        MailServer.createEmailAccount(USER_EMAIL, "password");
        
        try {
            //configure smpt server in the realm
            RealmRepresentation master = adminClient.realm(bc.consumerRealmName()).toRepresentation();
            master.setSmtpServer(suiteContext.getSmtpServer());
            adminClient.realm(bc.consumerRealmName()).update(master);
        
            //create user on consumer's site who should be linked later
            UserRepresentation newUser = UserBuilder.create().username("consumer").email(USER_EMAIL).enabled(true).build();
            String userId = createUserWithAdminClient(adminClient.realm(bc.consumerRealmName()), newUser);
            resetUserPassword(adminClient.realm(bc.consumerRealmName()).users().get(userId), "password", false);
        
            //test
            driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

            log.debug("Clicking social " + bc.getIDPAlias());
            accountLoginPage.clickSocial(bc.getIDPAlias());

            waitForPage(driver, "log in to");

            Assert.assertTrue("Driver should be on the provider realm page right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

            log.debug("Logging in");
            accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

            waitForPage(driver, "update account information");

            Assert.assertTrue(updateAccountInformationPage.isCurrent());
            Assert.assertTrue("We must be on correct realm right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

            log.debug("Updating info on updateAccount page");
            updateAccountInformationPage.updateAccountInformation("Firstname", "Lastname");

            //link account by email
            waitForPage(driver, "account already exists");
            idpConfirmLinkPage.clickLinkAccount();
            
            String url = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL, 
                    "Someone wants to link your ", false);

            log.info("navigating to url from email: " + url);
            driver.navigate().to(url);

            //test if user is logged in
            assertEquals(accountPage.buildUri().toASCIIString().replace("master", "consumer") + "/", driver.getCurrentUrl());
            
            //test if the user has verified email
            assertTrue(adminClient.realm(bc.consumerRealmName()).users().get(userId).toRepresentation().isEmailVerified());
        } finally {
            // stop mail server
            MailServer.stop();
        }
    }

    // KEYCLOAK-3267
    @Test
    public void loginWithExistingUserWithBruteForceEnabled() {
        adminClient.realm(bc.consumerRealmName()).update(RealmBuilder.create().bruteForceProtected(true).failureFactor(2).build());

        loginWithExistingUser();

        driver.navigate().to(getAccountPasswordUrl(bc.consumerRealmName()));

        accountPasswordPage.changePassword("password", "password");

        logoutFromRealm(bc.providerRealmName());

        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        try {
            waitForPage(driver, "log in to");
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }

        for (int i = 0; i < 3; i++) {
            try {
                waitForElementEnabled(driver, "login");
            } catch (TimeoutException e) {
                Assert.fail("Timeout while waiting for login element enabled");
            }

            accountLoginPage.login(bc.getUserLogin(), "invalid");
        }

        assertEquals("Invalid username or password.", accountLoginPage.getError());

        accountLoginPage.clickSocial(bc.getIDPAlias());

        try {
            waitForPage(driver, "log in to");
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }

        Assert.assertTrue("Driver should be on the provider realm page right now", driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

        assertEquals("Account is disabled, contact admin.", errorPage.getError());
    }

    protected void logoutFromRealm(String realm) {
        driver.navigate().to(getAuthRoot()
          + "/auth/realms/" + realm
          + "/protocol/" + "openid-connect"
          + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(realm)));

        try {
            Retry.execute(() -> {
                try {
                    waitForPage(driver, "log in to " + realm);
                } catch (TimeoutException ex) {
                    driver.navigate().refresh();
                    log.debug("[Retriable] Timed out waiting for login page");
                    throw ex;
                }
            }, 10, 100);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }
    }

    protected void testSingleLogout() {
        log.debug("Testing single log out");

        driver.navigate().to(getAccountUrl(bc.providerRealmName()));

        Assert.assertTrue("Should be logged in the account page", driver.getTitle().endsWith("Account Management"));

        logoutFromRealm(bc.providerRealmName());

        Assert.assertTrue("Should be on " + bc.providerRealmName() + " realm", driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName()));

        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        Assert.assertTrue("Should be on " + bc.consumerRealmName() + " realm on login page",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/"));
    }

    private String getAccountUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/account";
    }

    private String getAccountPasswordUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/account/password";
    }
}
