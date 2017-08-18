package org.keycloak.testsuite.broker;

import org.junit.Before;
import org.junit.Test;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.util.*;

import org.openqa.selenium.TimeoutException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;

import org.jboss.arquillian.graphene.page.Page;

import javax.ws.rs.core.Response;

import static org.keycloak.testsuite.broker.BrokerTestTools.*;

public abstract class AbstractBrokerTest extends AbstractBaseBrokerTest {

    protected IdentityProviderResource identityProviderResource;

    @Before
    public void beforeBrokerTest() {
        log.debug("creating user for realm " + bc.providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);

        if (testContext.isInitialized()) {
            return;
        }

        log.debug("adding identity provider to realm " + bc.consumerRealmName());
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().create(bc.setUpIdentityProvider(suiteContext)).close();
        identityProviderResource = realm.identityProviders().get(bc.getIDPAlias());

        // addClients
        List<ClientRepresentation> clients = bc.createProviderClients(suiteContext);
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.providerRealmName());

                providerRealm.clients().create(client).close();
            }
        }

        clients = bc.createConsumerClients(suiteContext);
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.consumerRealmName());

                consumerRealm.clients().create(client).close();
            }
        }

        testContext.setInitialized(true);
    }


    @Test
    public void testLogInAsUserInIDP() {
        loginUser();

        testSingleLogout();
    }

    protected void loginUser() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        log.debug("Clicking social " + bc.getIDPAlias());
        accountLoginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now",
          driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "update account information");

        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
          driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

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
    }

    @Test
    public void loginWithExistingUser() {
        testLogInAsUserInIDP();

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

    @Page
    ConsentPage consentPage;

    // KEYCLOAK-4181
    @Test
    public void loginWithExistingUserWithErrorFromProviderIdP() {
        ClientRepresentation client = adminClient.realm(bc.providerRealmName())
          .clients()
          .findByClientId(bc.getIDPClientIdInProviderRealm(suiteContext))
          .get(0);

        adminClient.realm(bc.providerRealmName())
          .clients()
          .get(client.getId())
          .update(ClientBuilder.edit(client).consentRequired(true).build());

        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        log.debug("Clicking social " + bc.getIDPAlias());
        accountLoginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.MINUTES);

        waitForPage(driver, "grant access");
        consentPage.cancel();

        waitForPage(driver, "log in to");

        // Revert consentRequired
        adminClient.realm(bc.providerRealmName())
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client).consentRequired(false).build());

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

    protected void createRolesForRealm(String realm) {
        RoleRepresentation managerRole = new RoleRepresentation("manager",null, false);
        RoleRepresentation userRole = new RoleRepresentation("user",null, false);
        adminClient.realm(realm).roles().create(managerRole);
        adminClient.realm(realm).roles().create(userRole);
    }

    protected void createRoleMappersForConsumerRealm() {
        log.debug("adding mappers to identity provider in realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        IdentityProviderResource idpResource = realm.identityProviders().get(bc.getIDPAlias());
        for (IdentityProviderMapperRepresentation mapper : createIdentityProviderMappers()) {
            mapper.setIdentityProviderAlias(bc.getIDPAlias());
            Response resp = idpResource.addMapper(mapper);
            resp.close();
        }
    }

    protected abstract Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers();

    // KEYCLOAK-3987
    @Test
    public void grantNewRoleFromToken() {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm();

        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get("manager").toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get("user").toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        logInAsUserInIDPForFirstTime();

        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
        assertEquals("There should be manager role",1, currentRoles.stream().filter(role -> role.getName().equals("manager")).collect(Collectors.toList()).size());
        assertEquals("User shouldn't have user role", 0, currentRoles.stream().filter(role -> role.getName().equals("user")).collect(Collectors.toList()).size());

        logoutFromRealm(bc.consumerRealmName());

        userResource.roles().realmLevel().add(Collections.singletonList(userRole));

        logInAsUserInIDP();

        currentRoles = userResource.roles().realmLevel().listAll();
        assertEquals("There should be manager role",1, currentRoles.stream().filter(role -> role.getName().equals("manager")).collect(Collectors.toList()).size());
        assertEquals("There should be user role",1, currentRoles.stream().filter(role -> role.getName().equals("user")).collect(Collectors.toList()).size());

        logoutFromRealm(bc.providerRealmName());
        logoutFromRealm(bc.consumerRealmName());
    }


    // KEYCLOAK-4016
    @Test
    public void testExpiredCode() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        log.debug("Expire all browser cookies");
        driver.manage().deleteAllCookies();

        log.debug("Clicking social " + bc.getIDPAlias());
        accountLoginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sorry");
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();
        Assert.assertTrue(link.endsWith("/auth/realms/consumer/account"));
    }
}
