package org.keycloak.tests.broker;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Time;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.providers.federation.DummyUserFederationProviderFactory;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.TimeoutException;

import static org.keycloak.tests.broker.BrokerRunOnServerUtil.configurePostBrokerLoginWithOTP;
import static org.keycloak.tests.broker.BrokerRunOnServerUtil.disablePostBrokerLoginFlow;
import static org.keycloak.tests.broker.BrokerTestTools.getAuthPath;
import static org.keycloak.tests.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.tests.broker.BrokerTestTools.waitForElementEnabled;
import static org.keycloak.tests.broker.BrokerTestTools.waitForPage;
import static org.keycloak.tests.utils.admin.AdminApiUtil.removeUserByUsername;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of advanced functionalities related to brokering like:
 * - Account management linking
 * - Retrieve of broker token
 * - PostBrokerLoginFlow
 * - Single logout propagation to broker
 * - Disabled user
 * - etc
 */
@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public abstract class AbstractAdvancedBrokerTest extends AbstractBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    protected void createRoleMappersForConsumerRealm() {
        createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode.FORCE);
    }

    protected void createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode syncMode) {
        log.debug("adding mappers to identity provider in realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        IdentityProviderResource idpResource = realm.identityProviders().get(bc.getIDPAlias());
        for (IdentityProviderMapperRepresentation mapper : createIdentityProviderMappers(syncMode)) {
            mapper.setIdentityProviderAlias(bc.getIDPAlias());
            Response resp = idpResource.addMapper(mapper);
            resp.close();
        }
    }

    protected abstract Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode);

    protected abstract void createAdditionalMapperWithCustomSyncMode(IdentityProviderMapperSyncMode syncMode);

    /**
     * Refers to in old test suite: org.keycloak.tests.broker.AbstractKeycloakIdentityProviderTest#testAccountManagementLinkIdentity
     */
    @Test
    public void testAccountManagementLinkIdentity() {
        assumeFalse("Account linking does not apply to transient sessions", isUsingTransientSessions());

        createUser("consumer");

        // Link identity provider through Admin REST api
        Response response = addIdentityProviderLink("consumer", bc.getUserLogin());
        Assertions.assertEquals(204, response.getStatus(), "status");

        // Assert identity is linked through Admin REST api
        assertTrue(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "consumer");
        driver.manage().deleteAllCookies();

        // Assert I am logged immediately into app page due to previously linked "test-user" identity
        initiateBrokerAppLogin(bc.getUserLogin(), bc.getUserPassword());
        ensureBrokerLoginCompleted();

        // Unlink idp from consumer
        AccountHelper.deleteIdentityProvider(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias());
        assertFalse(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        // Logout from account management
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "consumer");
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "testuser");
        driver.manage().deleteAllCookies();

        // Assert I am not logged immediately into app page and first-broker-login appears instead
        initiateBrokerAppLogin(bc.getUserLogin(), bc.getUserPassword());
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        loginPage.login(bc.getUserPassword());
        boolean success = oauth.parseLoginResponse().isSuccess();
        Assertions.assertTrue(success, "Expected OAuth login response success. url=" + driver.getCurrentUrl()
                + ", pageId=" + driver.page().getCurrentPageId());
        assertTrue(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        // Unlink my "test-user"
        AccountHelper.deleteIdentityProvider(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias());
        assertFalse(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        // Logout from account management
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "consumer");
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "testuser");
        driver.manage().deleteAllCookies();

        //Try to log in. Previous link is not valid anymore, so now it should try to register new user instead of logging into app page
        initiateBrokerAppLogin(bc.getUserLogin(), bc.getUserPassword());
        updateAccountInformationPage.assertCurrent();
    }

    /**
     * Refers to in old test suite: org.keycloak.tests.broker.AbstractKeycloakIdentityProviderTest#testAccountManagementLinkedIdentityAlreadyExists
     */
    @Test
    public void testAccountManagementLinkedIdentityAlreadyExists() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");

        // Link identity provider through Admin REST api
        Response response = addIdentityProviderLink("consumer", bc.getUserLogin());
        Assertions.assertEquals(204, response.getStatus(), "status");

        // Test we will log in immediately into app page
        initiateBrokerAppLogin(bc.getUserLogin(), bc.getUserPassword());
        ensureBrokerLoginCompleted();
    }

    // KEYCLOAK-3267
    @Test
    public void loginWithExistingUserWithBruteForceEnabled() {
        assumeFalse("Brute force protection does not apply to transient sessions", isUsingTransientSessions());

        adminClient.realm(bc.consumerRealmName()).update(RealmBuilder.create().bruteForceProtected(true).failureFactor(2).build());

        loginWithExistingUser();

        Assertions.assertTrue(AccountHelper.updatePassword(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin(), "password"));

        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());
        driver.manage().deleteAllCookies();

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        try {
            waitForPage(driver, "sign in to", true);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assertions.fail("Timeout while waiting for login page");
        }

        for (int i = 0; i < 3; i++) {
            try {
                waitForElementEnabled(driver, "login");
            } catch (TimeoutException e) {
                Assertions.fail("Timeout while waiting for login element enabled");
            }

            loginPage.login(bc.getUserLogin(), "invalid");
        }

        assertEquals("Invalid username or password.", loginPage.getInputError());

        waitForBruteForceExecutorsInConsumerRealm();

        loginPage.clickSocial(bc.getIDPAlias());

        try {
            waitForPage(driver, "sign in to", true);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assertions.fail("Timeout while waiting for login page");
        }

        Assertions.assertTrue(driver.getCurrentUrl().contains(getAuthPath() + "/realms/" + bc.providerRealmName() + "/"), "Driver should be on the provider realm page right now");

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "sign in to", true);
        errorPage.assertCurrent();
        assertEquals("Account is disabled, contact your administrator.", errorPage.getError());
    }

    // KEYCLOAK-4181
    @Test
    public void loginWithExistingUserWithErrorFromProviderIdP() {
        ClientRepresentation client = adminClient.realm(bc.providerRealmName())
                .clients()
                .findByClientId(bc.getIDPClientIdInProviderRealm())
                .get(0);

        adminClient.realm(bc.providerRealmName())
                .clients()
                .get(client.getId())
                .update(ClientBuilder.update(client).consentRequired(true).build());

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithBroker(bc);

        driver.manage().timeouts().pageLoadTimeout(Duration.ofMinutes(30));

        waitForPage(driver, "grant access", false);
        consentPage.cancel();

        waitForPage(driver, "sign in to", true);

        // Revert consentRequired
        adminClient.realm(bc.providerRealmName())
                .clients()
                .get(client.getId())
                .update(ClientBuilder.update(client).consentRequired(false).build());

    }

    /**
     * Refers to in old test suite: org.keycloak.tests.broker.AbstractKeycloakIdentityProviderTest.testDisabledUser
     */
    @Test
    public void testDisabledUser() {
        assumeFalse("There is no user to update after user logout when using transient sessions", isUsingTransientSessions());

        loginUser();

        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation userRep = getConsumerUserRepresentation(bc.getUserLogin());
        UserResource user = realm.users().get(userRep.getId());

        userRep.setEnabled(false);

        user.update(userRep);

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithBroker(bc);
        errorPage.assertCurrent();
        assertEquals("Account is disabled, contact your administrator.", errorPage.getError());
    }

    // KEYCLOAK-3987
    @Test
    public void mapperDoesNotGrantNewRoleFromTokenWithSyncModeImport() {
        testMapperAssigningRoles(IdentityProviderMapperSyncMode.IMPORT, false);
    }

    @Test
    public void mapperGrantsNewRoleFromTokenWithInheritedSyncModeForce() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().get(bc.getIDPAlias())
                .update(bc.setUpIdentityProvider(IdentityProviderSyncMode.FORCE));

        testMapperAssigningRoles(IdentityProviderMapperSyncMode.INHERIT, true);
    }

    @Test
    public void mapperDoesNotGrantNewRoleFromTokenWithInheritedSyncModeImport() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().get(bc.getIDPAlias())
                .update(bc.setUpIdentityProvider(IdentityProviderSyncMode.IMPORT));

        testMapperAssigningRoles(IdentityProviderMapperSyncMode.INHERIT, false);
    }

    private void testMapperAssigningRoles(IdentityProviderMapperSyncMode anImport, boolean isAssigned) {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm(anImport);

        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        logInAsUserInIDPForFirstTime();

        UserResource consumerUserResource = adminClient.realm(bc.consumerRealmName()).users().get(getConsumerUserRepresentation(bc.getUserLogin()).getId());
        Set<String> currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER)));

        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        userResource.roles().realmLevel().add(Collections.singletonList(userRole));

        if (isUsingTransientSessions()) {
            // Transient sessions never update user, the rest of the test applies to persistent users only
            return;
        } else {
            logInAsUserInIDP();
        }

        currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        if (isAssigned) {
            assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_USER));
        } else {
            assertThat(currentRoles, hasItems(ROLE_MANAGER));
            assertThat(currentRoles, not(hasItems(ROLE_USER)));
        }

        logoutFromConsumerRealm();
        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
    }

    @Test
    public void differentMappersCanHaveDifferentSyncModes() {
        assumeFalse("Sync mode does not apply to transient sessions as the mappers are applied only once and there is nothing to update", isUsingTransientSessions());

        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm(IdentityProviderMapperSyncMode.INHERIT);
        createAdditionalMapperWithCustomSyncMode(IdentityProviderMapperSyncMode.FORCE);


        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();
        RoleRepresentation friendlyManagerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_FRIENDLY_MANAGER).toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        logInAsUserInIDPForFirstTime();

        UserResource consumerUserResource = adminClient.realm(bc.consumerRealmName()).users().get(
                adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin()).get(0).getId());
        Set<String> currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER, ROLE_FRIENDLY_MANAGER)));

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        userResource.roles().realmLevel().add(Arrays.asList(userRole, friendlyManagerRole));

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInAsUserInIDP();

        currentRoles = consumerUserResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_FRIENDLY_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER)));

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());
    }

    // KEYCLOAK-4016
    @Test
    public void testExpiredCode() {
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        log.debug("Expire all browser cookies");
        driver.manage().deleteAllCookies();

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sorry", false);
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();
        Assertions.assertTrue(link.contains(getAuthPath() + "/realms/" + bc.consumerRealmName() + "/app"));
    }

    /**
     * Refers to in old testsuite: org.keycloak.tests.broker.PostBrokerFlowTest#testPostBrokerLoginWithOTP()
     */
    @Test
    public void testPostBrokerLoginFlowWithOTP() {
        assumeFalse("Password / OTP setup does not apply to transient sessions as there is no persistent user to log in twice", isUsingTransientSessions());

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        runOnConsumerRealm(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithBroker(bc);

        totpPage.assertCurrent();
        String totpSecret = totpPage.getTotpSecret();
        totpPage.configure(totp.generateTOTP(totpSecret));
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        setOtpTimeOffset(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS, totp);

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithBroker(bc);

        waitForPage(driver, "sign in to", true);
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP(totpSecret));
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        runOnConsumerRealm(disablePostBrokerLoginFlow(bc.getIDPAlias()));

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithBroker(bc);
    }

    /**
     * Refers to in old testsuite: org.keycloak.tests.broker.OIDCKeyCloakServerBrokerBasicTest#testLogoutWorksWithTokenTimeout()
     */
    @Test
    public void testLogoutWorksWithTokenTimeout() {
        try {
            updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
            RealmRepresentation realm = adminClient.realm(bc.providerRealmName()).toRepresentation();
            assertNotNull(realm);
            realm.setAccessTokenLifespan(5);
            adminClient.realm(bc.providerRealmName()).update(realm);
            IdentityProviderRepresentation idp = adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).toRepresentation();
            idp.getConfig().put("backchannelSupported", "false");
            adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).update(idp);
            Time.setOffset(10);

            oauth.client("broker-app");
            oauth.realm(bc.consumerRealmName());
            oauth.openLoginForm();

            logInWithBroker(bc);
            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.assertCurrent();
            updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

            logoutFromConsumerRealm();

            oauth.client("broker-app");
            oauth.realm(bc.consumerRealmName());
            oauth.openLoginForm();

            waitForPage(driver, "sign in to", true);
            log.debug("Logging in");
            assertTrue(this.driver.getCurrentUrl().contains(getAuthPath() + "/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/auth"));
        } finally {
            Time.setOffset(0);
        }
    }

    /**
     * Refers to in old test suite: org.keycloak.tests.broker.AbstractKeycloakIdentityProviderTest#testWithLinkedFederationProvider
     */
    @Test
    public void testWithLinkedFederationProvider() {
        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

            ComponentRepresentation component = new ComponentRepresentation();
            component.setName(DummyUserFederationProviderFactory.PROVIDER_NAME);
            component.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            component.setProviderType(UserStorageProvider.class.getName());

            adminClient.realm(bc.consumerRealmName()).components().add(component);

            createUser(bc.providerRealmName(), "test-user", "password", "FirstName", "LastName", "test-user@localhost.com");

            oauth.client("broker-app");
            oauth.realm(bc.consumerRealmName());
            oauth.openLoginForm();

            loginPage.clickSocial(bc.getIDPAlias());
            loginPage.login("test-user", "password");

            if (isUsingTransientSessions()) {
                assertThat(getConsumerUserRepresentation("test-user"), notNullValue());
                // Updating password and the rest of the test is irrelevant for transient sessions
                return;
            }
            Assertions.assertTrue(AccountHelper.updatePassword(adminClient.realm(bc.consumerRealmName()), "test-user", "new-password"));

            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "test-user");
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "test-user");

            createUser(bc.providerRealmName(), "test-user-noemail", "password", "FirstName", "LastName", "test-user-noemail@localhost.com");

            oauth.client("broker-app");
            oauth.realm(bc.consumerRealmName());
            oauth.openLoginForm();

            loginPage.clickSocial(bc.getIDPAlias());

            loginPage.login("test-user-noemail", "password");

            Assertions.assertTrue(AccountHelper.updatePassword(adminClient.realm(bc.consumerRealmName()), "test-user-noemail", "new-password"));
        } finally {
            removeUserByUsername(adminClient.realm(bc.consumerRealmName()), "test-user");
            removeUserByUsername(adminClient.realm(bc.consumerRealmName()), "test-user-noemail");
        }
    }

    @Test
    public void testDisabledBroker() {
        loginUser();
        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        identityProviderResource = realm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        idpRep.setEnabled(false);
        identityProviderResource.update(idpRep);
        waitForPage(driver, "sign in to", true);
        loginPage.clickSocial(bc.getIDPAlias());
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Could not send authentication request to identity provider."));

        idpRep.setEnabled(true);
        identityProviderResource.update(idpRep);
        oauth.openLoginForm();
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains(getAuthPath() + "/realms/" + bc.providerRealmName() + "/"), "Driver should be on the provider realm page right now");
        idpRep.setEnabled(false);
        identityProviderResource.update(idpRep);
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Page not found"));
    }

    private void runOnConsumerRealm(RunOnServer function) {
        final String consumerRealmName = bc.consumerRealmName();
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(consumerRealmName);
            if (realm == null) {
                throw new IllegalStateException("Realm not found: " + consumerRealmName);
            }
            session.getContext().setRealm(realm);
            function.run(session);
        });
    }

    private void waitForBruteForceExecutorsInConsumerRealm() {
        runOnConsumerRealm(session -> {
            ExecutorsProvider provider = session.getProvider(ExecutorsProvider.class);
            ExecutorService executor = provider.getExecutor("bruteforce");
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
            try {
                CompletableFuture.runAsync(() -> {
                    do {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } while (!threadPoolExecutor.getQueue().isEmpty() || threadPoolExecutor.getActiveCount() > 0);
                }).get(30, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                Assertions.fail("Timeout while waiting for brute force executors!");
            } catch (Exception e) {
                Assertions.fail("Unexpected error while waiting for brute force executors!");
            }
            assertEquals(0, threadPoolExecutor.getActiveCount());
        });
    }

    private void initiateBrokerAppLogin(String username, String password) {
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        loginPage.clickSocial(bc.getIDPAlias());
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    private void ensureBrokerLoginCompleted() {
        if (parseLoginResponseSafely()) {
            return;
        }

        String currentPageId = driver.page().getCurrentPageId();
        if ("login-idp-review-user-profile".equals(currentPageId) || "login-login-update-profile".equals(currentPageId)) {
            updateAccountInformation();
        }

        currentPageId = driver.page().getCurrentPageId();
        if (idpConfirmLinkPage.getExpectedPageId().equals(currentPageId)) {
            idpConfirmLinkPage.clickLinkAccount();
        }

        currentPageId = driver.page().getCurrentPageId();
        if (loginPage.getExpectedPageId().equals(currentPageId) && loginPage.isPasswordInputPresent()) {
            loginPage.login(bc.getUserPassword());
        }

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    private boolean parseLoginResponseSafely() {
        try {
            return oauth.parseLoginResponse().isSuccess();
        } catch (AssertionError error) {
            return false;
        }
    }

    private Response addIdentityProviderLink(String consumerUsername, String providerUsername) {
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());

        UserRepresentation consumerUser = AccountHelper.getUserRepresentation(consumerRealm, consumerUsername);
        FederatedIdentityRepresentation identity = new FederatedIdentityRepresentation();
        identity.setIdentityProvider(bc.getIDPAlias());
        identity.setUserName(providerUsername);

        if (BrokerTestConstants.IDP_SAML_ALIAS.equals(bc.getIDPAlias())) {
            identity.setUserId(providerUsername);
        } else {
            identity.setUserId(AccountHelper.getUserRepresentation(providerRealm, providerUsername).getId());
        }

        return consumerRealm.users().get(consumerUser.getId()).addFederatedIdentity(bc.getIDPAlias(), identity);
    }
}
