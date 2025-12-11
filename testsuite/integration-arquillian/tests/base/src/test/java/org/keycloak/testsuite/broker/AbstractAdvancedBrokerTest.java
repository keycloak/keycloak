package org.keycloak.testsuite.broker;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.TestAppHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configurePostBrokerLoginWithOTP;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.disablePostBrokerLoginFlow;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.grantReadTokenRole;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.revokeReadTokenRole;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForElementEnabled;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Test of advanced functionalities related to brokering like:
 * - Account management linking
 * - Retrieve of broker token
 * - PostBrokerLoginFlow
 * - Single logout propagation to broker
 * - Disabled user
 * - etc
 */
public abstract class AbstractAdvancedBrokerTest extends AbstractBrokerTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

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
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testAccountManagementLinkIdentity
     */
    @Test
    public void testAccountManagementLinkIdentity() {
        assumeFalse("Account linking does not apply to transient sessions", isUsingTransientSessions());

        createUser("consumer");
        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);

        // Link identity provider through Admin REST api
        Response response = AccountHelper.addIdentityProvider(adminClient.realm(bc.consumerRealmName()), "consumer", adminClient.realm(bc.providerRealmName()), bc.getUserLogin(), bc.getIDPAlias());
        Assert.assertEquals("status", 204, response.getStatus());

        // Assert identity is linked through Admin REST api
        assertTrue(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "consumer");

        // Assert I am logged immediately into app page due to previously linked "test-user" identity
        testAppHelper.login(bc.getUserLogin(), bc.getUserPassword(), bc.consumerRealmName(), "broker-app", bc.getIDPAlias());

        // Unlink idp from consumer
        AccountHelper.deleteIdentityProvider(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias());
        assertFalse(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        // Logout from account management
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "consumer");
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "testuser");

        // Assert I am not logged immediately into app page and first-broker-login appears instead
        Assert.assertFalse(testAppHelper.login(bc.getUserLogin(), bc.getUserPassword(), bc.consumerRealmName(), "broker-app", bc.getIDPAlias()));

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        loginPage.login(bc.getUserPassword());
        appPage.assertCurrent();
        assertTrue(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        // Unlink my "test-user"
        AccountHelper.deleteIdentityProvider(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias());
        assertFalse(AccountHelper.isIdentityProviderLinked(adminClient.realm(bc.consumerRealmName()), "consumer", bc.getIDPAlias()));

        // Logout from account management
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "consumer");
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "testuser");

        //Try to log in. Previous link is not valid anymore, so now it should try to register new user instead of logging into app page
        Assert.assertFalse(testAppHelper.login(bc.getUserLogin(), bc.getUserPassword(), bc.consumerRealmName(), "broker-app", bc.getIDPAlias()));
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testAccountManagementLinkedIdentityAlreadyExists
     */
    @Test
    public void testAccountManagementLinkedIdentityAlreadyExists() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");
        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);

        // Link identity provider through Admin REST api
        Response response = AccountHelper.addIdentityProvider(adminClient.realm(bc.consumerRealmName()), "consumer", adminClient.realm(bc.providerRealmName()), bc.getUserLogin(), bc.getIDPAlias());
        Assert.assertEquals("status", 204, response.getStatus());

        // Test we will log in immediately into app page
        Assert.assertTrue(testAppHelper.login(bc.getUserLogin(), bc.getUserPassword(), bc.consumerRealmName(), "broker-app", bc.getIDPAlias()));
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testTokenStorageAndRetrievalByApplication
     */
    @Test
    public void testRetrieveToken() throws Exception {
        assumeFalse("There is no user to update once the user has logged in using transient sessions", isUsingTransientSessions());

        updateExecutions(AbstractBrokerTest::enableRequirePassword);
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();

        idpRep.setStoreToken(true);

        identityProviderResource.update(idpRep);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);
        updatePasswordPage.updatePasswords("password", "password");
        Assert.assertTrue(appPage.isCurrent());

        String username = bc.getUserLogin();

        testingClient.server(bc.consumerRealmName()).run(grantReadTokenRole(username));

        AccessTokenResponse accessTokenResponse = oauth.realm(bc.consumerRealmName()).client("broker-app", "broker-app-secret").doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        AtomicReference<String> accessToken = (AtomicReference<String>) new AtomicReference<>(accessTokenResponse.getAccessToken());
        Client client = KeycloakTestingClient.getRestEasyClientBuilder().register((ClientRequestFilter) request -> request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.get())).build();

        try {
            WebTarget target = client.target(Urls.identityProviderRetrieveToken(URI.create(getConsumerRoot() + "/auth"), bc.getIDPAlias(), bc.consumerRealmName()));

            try (Response response = target.request().get()) {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertNotNull(response.readEntity(String.class));
            }

            testingClient.server(bc.consumerRealmName()).run(revokeReadTokenRole(username));

            accessTokenResponse = oauth.realm(bc.consumerRealmName()).client("broker-app", "broker-app-secret").doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
            accessToken.set(accessTokenResponse.getAccessToken());

            try (Response response = target.request().get()) {
                assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            }
        } finally {
            client.close();
        }
    }



    // KEYCLOAK-3267
    @Test
    public void loginWithExistingUserWithBruteForceEnabled() {
        assumeFalse("Brute force protection does not apply to transient sessions", isUsingTransientSessions());

        adminClient.realm(bc.consumerRealmName()).update(RealmBuilder.create().bruteForceProtected(true).failureFactor(2).build());

        loginWithExistingUser();

        Assert.assertTrue(AccountHelper.updatePassword(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin(), "password"));

        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        try {
            waitForPage(driver, "sign in to", true);
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

            loginPage.login(bc.getUserLogin(), "invalid");
        }

        assertEquals("Invalid username or password.", loginPage.getInputError());

        loginPage.clickSocial(bc.getIDPAlias());

        try {
            waitForPage(driver, "sign in to", true);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }

        Assert.assertTrue("Driver should be on the provider realm page right now", driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

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
                .update(ClientBuilder.edit(client).consentRequired(true).build());

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.MINUTES);

        waitForPage(driver, "grant access", false);
        consentPage.cancel();

        waitForPage(driver, "sign in to", true);

        // Revert consentRequired
        adminClient.realm(bc.providerRealmName())
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client).consentRequired(false).build());

    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testDisabledUser
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

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

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

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

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
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Expire all browser cookies");
        driver.manage().deleteAllCookies();

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sorry", false);
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();
        Assert.assertTrue(link.contains("/auth/realms/" + bc.consumerRealmName() + "/app"));
    }

    /**
     * Refers to in old testsuite: org.keycloak.testsuite.broker.PostBrokerFlowTest#testPostBrokerLoginWithOTP()
     */
    @Test
    public void testPostBrokerLoginFlowWithOTP() {
        assumeFalse("Password / OTP setup does not apply to transient sessions as there is no persistent user to log in twice", isUsingTransientSessions());

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        totpPage.assertCurrent();
        String totpSecret = totpPage.getTotpSecret();
        totpPage.configure(totp.generateTOTP(totpSecret));
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        setOtpTimeOffset(TimeBasedOTP.DEFAULT_INTERVAL_SECONDS, totp);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "sign in to", true);
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP(totpSecret));
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        testingClient.server(bc.consumerRealmName()).run(disablePostBrokerLoginFlow(bc.getIDPAlias()));

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);
    }

    /**
     * Refers to in old testsuite: org.keycloak.testsuite.broker.OIDCKeyCloakServerBrokerBasicTest#testLogoutWorksWithTokenTimeout()
     */
    @Test
    public void testLogoutWorksWithTokenTimeout() {
        try {
            updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
            RealmRepresentation realm = adminClient.realm(bc.providerRealmName()).toRepresentation();
            assertNotNull(realm);
            realm.setAccessTokenLifespan(1);
            adminClient.realm(bc.providerRealmName()).update(realm);
            IdentityProviderRepresentation idp = adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).toRepresentation();
            idp.getConfig().put("backchannelSupported", "false");
            adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).update(idp);
            Time.setOffset(2);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);
            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.assertCurrent();
            updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

            logoutFromConsumerRealm();

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            waitForPage(driver, "sign in to", true);
            log.debug("Logging in");
            assertTrue(this.driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/auth"));
        } finally {
            Time.setOffset(0);
        }
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testWithLinkedFederationProvider
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

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            loginPage.clickSocial(bc.getIDPAlias());
            loginPage.login("test-user", "password");

            if (isUsingTransientSessions()) {
                assertThat(getConsumerUserRepresentation("test-user"), notNullValue());
                // Updating password and the rest of the test is irrelevant for transient sessions
                return;
            }
            Assert.assertTrue(AccountHelper.updatePassword(adminClient.realm(bc.consumerRealmName()), "test-user", "new-password"));

            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "test-user");
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "test-user");

            createUser(bc.providerRealmName(), "test-user-noemail", "password", "FirstName", "LastName", "test-user-noemail@localhost.com");

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            loginPage.clickSocial(bc.getIDPAlias());

            loginPage.login("test-user-noemail", "password");

            Assert.assertTrue(AccountHelper.updatePassword(adminClient.realm(bc.consumerRealmName()), "test-user-noemail", "new-password"));
        } finally {
            removeUserByUsername(adminClient.realm(bc.consumerRealmName()), "test-user");
            removeUserByUsername(adminClient.realm(bc.consumerRealmName()), "test-user-noemail");
        }
    }
}
